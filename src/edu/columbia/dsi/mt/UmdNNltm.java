/**
 * 
 */
package edu.columbia.dsi.mt;

import java.io.BufferedReader;
import java.io.File;
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
 * May 2, 2020
 */
public class UmdNNltm {
	private static Logger logger = Logger.getLogger(UmdNNltm.class.getSimpleName());
	private ResourceFactory resourceFactory = new ResourceFactory();
	private Language src = Language.ps;
	private String gpuIdx;
	private int threadsNum=1;
	private String version;
	private File translateDirScript;
	private Type type = Type.text;
	private WorkDirHdlr workDirHdlr;
	private static final String workDir="UMD-nnltm-WorkDir";

	/**
	 * 
	 */
	public UmdNNltm(Language src, Type type, int[] gpuIdx, int threadsNum) {
		this.type=type;
		String idx=Integer.toString((gpuIdx[0]));
		for(int i=1;i<gpuIdx.length;i++) {
			idx+=","+Integer.toString((gpuIdx[i]));
		}
		this.gpuIdx = idx;
		logger.debug("Initializing umd-nnltm-Docker image from "+src.toString()+" to "+Language.en.toString()+" on GPU "+this.gpuIdx);		
		this.src = src;
		
		this.threadsNum=threadsNum;
		this.version=resourceFactory.getUmdnnltmVersion(src, type);
		this.translateDirScript=resourceFactory.getUmdnnltmTranslateDirScript();
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
			if(isTranslatedDir(targetDirPath)) {
				logger.debug("The output directory: "+targetDirPath.getCanonicalPath()+" is not empty. It looks like it is already translated...");
			}else {
				String cmd="sh"+" "+translateDirScript.getCanonicalPath()+" "+tmpSourceDirPath.getCanonicalPath()+" "+targetDirPath.getCanonicalPath()+" "+version+" "+src.toString()+" "+type.toString()+" "+String.valueOf(threadsNum)+" "+gpuIdx;
				logger.debug(cmd);
				Process process = Runtime.getRuntime().exec(cmd);
				exportLog(process);
				process.waitFor();
				logger.debug("Validating translation output...");
				if(isTranslatedDir(targetDirPath)) {
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
	private boolean isTranslatedDir(File outDir) {
		File outFile=new File(outDir.getAbsolutePath()+File.separator+"result.pkl");
		if(outFile.exists()) {
			return true;
		}else {
			return false;
		}		
	}
	
	public String getVersion() {
		return version;
	}

}
