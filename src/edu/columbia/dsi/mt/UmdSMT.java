/**
 * 
 */
package edu.columbia.dsi.mt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import edu.columbia.dsi.SCRIPTS.Language;
import edu.columbia.dsi.containers.DocInfo.Type;
import edu.columbia.dsi.utils.IoHdlr;
import edu.columbia.dsi.utils.ResourceFactory;
import edu.columbia.dsi.utils.WorkDirHdlr;

/**
 * @author badrashiny
 * Apr 17, 2018
 */
public class UmdSMT {
	private static Logger logger = Logger.getLogger(UmdSMT.class.getSimpleName());
	private ResourceFactory resourceFactory = new ResourceFactory();
	private Language src = Language.tl;
	private Language tgt = Language.en;
	private int threadsNum=1;
	private String version;
	private File translateDirScript;
	
	private WorkDirHdlr workDirHdlr;
	private static final String workDir="UMD-SMT-WorkDir";

	/**
	 * 
	 */
	public UmdSMT(Language src, Language tgt, Type type, int threadsNum) {
		logger.debug("Initializing umd-smt-Docker image from "+src.toString()+" to "+tgt.toString());		
		this.src = src;
		this.tgt = tgt;
		this.threadsNum=threadsNum;
		this.version=resourceFactory.getUmdSMTVersion(src, type);
		this.translateDirScript=resourceFactory.getUmdSMTTranslateDirScript();
	}
	
	public boolean translateDir(File sourceDirPath, File targetDirPath){	
		boolean isNoError=true;
		try {
			logger.debug("Translating directory: "+ sourceDirPath.getCanonicalPath());	
			/**
			 * This step because of the ASR output. It contains files other than the (.txt)
			 */
			ArrayList<String>folders=new ArrayList<String>();
			String workDirTmp=workDir+"-"+UUID.randomUUID().toString(); 
			folders.add(workDirTmp);
			workDirHdlr = new WorkDirHdlr(folders);
			File tmpSourceDirPath=workDirHdlr.getSubFolderHdlr(workDirTmp);
			
			ArrayList<File> srcFiles = IoHdlr.getInstance().getListOfFiles(sourceDirPath);
			for(File f:srcFiles) {
				if(f.getCanonicalPath().endsWith(".txt")) {
					String tgtFilePath=tmpSourceDirPath.getCanonicalPath()+File.separator+f.getName();
					FileUtils.copyFile(f, new File(tgtFilePath));
				}
			}			
			///////////////////////////////////////////////////////////
			if(isTranslatedDir(tmpSourceDirPath, targetDirPath)) {
				logger.debug("The output directory: "+targetDirPath.getCanonicalPath()+" is not empty. It looks like it is already translated...");
			}else {
				String cmd="sh"+" "+translateDirScript.getCanonicalPath()+" "+tmpSourceDirPath.getCanonicalPath()+" "+targetDirPath.getCanonicalPath()+" "+version+" "+src.toString()+" "+tgt.toString()+" "+String.valueOf(threadsNum);
				Process process = Runtime.getRuntime().exec(cmd);
				exportLog(process);
				process.waitFor();

				logger.debug("Validating translation output...");
				if(isTranslatedDir(tmpSourceDirPath, targetDirPath)) {
					logger.debug(sourceDirPath.getCanonicalPath()+ " is translated to: "+ targetDirPath.getCanonicalPath());
				}else {
					logger.error(sourceDirPath.getCanonicalPath()+ " is not completely translated. Please check the error messages.");
					isNoError=false;
				}			    
			}
			workDirHdlr.cleanup();					
		} catch (IOException | InterruptedException e) {
			logger.error(e.getMessage());
			workDirHdlr.cleanup();
			isNoError=false;
		}
		return isNoError;
	}
	private void exportLog(Process process) {
		ExecutorService executorService = Executors.newFixedThreadPool(2);
		ArrayList<Future<?>>openThreads=new ArrayList<Future<?>>();
		Future<?> stdoutFuture = executorService.submit(new Runnable() {
		    public void run() {
		    	BufferedReader stdout =	new BufferedReader(new InputStreamReader(process.getInputStream()));
				String str="";
		    	try {
					while ((str=stdout.readLine()) != null) {logger.debug(str);}
				} catch (IOException e) {
					logger.fatal(e.getMessage());
				}
		    }
		});	
		openThreads.add(stdoutFuture);
		
		Future<?> stderrFuture = executorService.submit(new Runnable() {
		    public void run() {
		    	BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
				String strErr="";
		    	try {
					while ((strErr=stderr.readLine()) != null) {logger.error(strErr);}
				} catch (IOException e) {
					logger.fatal(e.getMessage());
				}
		    }
		});	
		openThreads.add(stderrFuture);
		
		for(int k=0;k<openThreads.size();k++) {
			try {
				openThreads.get(k).get();
			} catch (InterruptedException | ExecutionException e) {
				logger.fatal(e.getMessage());
			}					
		}	
		executorService.shutdown();
	}
	private boolean isTranslatedDir(File sourceDirPath, File targetDirPath) {
		boolean translated=false;
		ArrayList<File> srcFiles = IoHdlr.getInstance().getListOfFiles(sourceDirPath);
		ArrayList<File> tgtFilesTmp = IoHdlr.getInstance().getListOfFiles(targetDirPath);
		ArrayList<File> tgtFiles=new ArrayList<File> ();
		for(File file:tgtFilesTmp) {
			if(file.getName().endsWith(".txt")) {
				tgtFiles.add(file);
			}
		}
		if(srcFiles.size()==tgtFiles.size()) {
			int notEmptyFiles=0;
			for(int i=0;i<tgtFiles.size();i++) {
				if(isEmptyFile(tgtFiles.get(i))) {
					logger.error(tgtFiles.get(i).getAbsolutePath()+" is empty");
				}else {
					notEmptyFiles++;
				}
			}
			if(notEmptyFiles==tgtFiles.size()) {
				translated=true;
			}			
		}	
		return translated;
	}
	
	private boolean isEmptyFile(File filePath) {
		BufferedReader tmp=null;
		boolean empty=true;
		try {
			if(filePath.exists()) {
				tmp = new BufferedReader(new FileReader(filePath.getCanonicalPath())); 
				String str=tmp.readLine();
				tmp.close();
				if(str == null) {//i.e. the file is empty or crashed before translation
					empty=true;
				}else {
					empty=false;
				}
			}else {
				empty=true;
			}
		}catch (IOException e) {
			logger.error(e.getMessage());
			try {
				tmp.close();
			} catch (IOException e1) {
				logger.error(e1.getMessage());
			}
			 empty=true;
		}
		return empty;
	}
	
	public String getVersion() {
		return version;
	}
	

}
