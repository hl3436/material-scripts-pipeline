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
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import edu.columbia.dsi.SCRIPTS.Language;
import edu.columbia.dsi.containers.DocInfo.Type;
import edu.columbia.dsi.mt.Translator.TranslatorMode;
import edu.columbia.dsi.utils.IoHdlr;
import edu.columbia.dsi.utils.ResourceFactory;
import edu.columbia.dsi.utils.WorkDirHdlr;

/**
 * @author badrashiny
 * Jan 6, 2021
 */
public class EdiNMT {
	private static Logger logger = Logger.getLogger(EdiNMT.class.getSimpleName());
	private ResourceFactory resourceFactory = new ResourceFactory();
	private Language src = Language.tl;
	private Language tgt = Language.en;
	private String gpuIdx;
	private String version;
	private File translateDirScript;
	private Type type = Type.text;
	private TranslatorMode mode=TranslatorMode.accurate;
	private WorkDirHdlr workDirHdlr;
	private File nbestSplitScriptPath=null;
	private static final String workDir="Edi-NMT-WorkDir";
	private String instanceName=null;
	
	/**
	 * 
	 */
	public EdiNMT(Language src, Language tgt, Type type, TranslatorMode mode, int[] gpuIdx) {
		this.type=type;
		this.mode=mode;
		String idx=Integer.toString((gpuIdx[0]));
		for(int i=1;i<gpuIdx.length;i++) {
			idx+=","+Integer.toString((gpuIdx[i]));
		}
		this.gpuIdx = idx;
		logger.debug("Initializing edi-nmt-Docker image from "+src.toString()+" to "+tgt.toString()+" on GPU "+this.gpuIdx);		
		this.src = src;
		this.tgt = tgt;
		
		this.version=resourceFactory.getEdiNMTVersion(src, type);
		this.translateDirScript=resourceFactory.getEdiNMTTranslateDirScript();	
		this.nbestSplitScriptPath=resourceFactory.getEdiNMTNbestSplitScript();
	}
	
	/**
	 * initialize the MT on a specific network and a specific name. Used by the Query analyzer
	 */
	public EdiNMT(Language src, Language tgt, Type type, TranslatorMode mode, int[] gpuIdx, String networkName, String instanceName) {
		try {
			this.instanceName=instanceName;
			String gpuIdxStr=Integer.toString((gpuIdx[0]));
			for(int i=1;i<gpuIdx.length;i++) {
				gpuIdxStr+=","+Integer.toString((gpuIdx[i]));
			}
			logger.debug("Loading Edi-nmt docker image from "+src.toString()+" to "+tgt.toString()+" on GPU "+this.gpuIdx+" in server mode");
			
			
			File startServerScriptPath=resourceFactory.getEdiNMTStartServerWithNetworkScript();
			
			String version=resourceFactory.getEdiNMTVersion(src, type);
			if(version!=null) {
				String cmd="sh"+" "+ startServerScriptPath.getCanonicalPath()+" "+src.toString()+" "+tgt.toString()+" "+type.toString()+" "+mode.toString()+" "+instanceName+" "+networkName;
				cmd+=" "+version+" "+gpuIdxStr;
				logger.debug(cmd);
				Runtime.getRuntime().exec(cmd);
				
		        TimeUnit.SECONDS.sleep(180);//MarianNMT needs ~180 sec. to load the models into the memory
				
				logger.debug("Edi-nmt is loaded sucessfuly..."); 
			}else {
				logger.error("Couldn't start Edi-nmt server");
			}
		} catch (IOException | InterruptedException e) {
			logger.error("Couldn't start the nmt server");
		}
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
				
				String cmd="sh"+" "+translateDirScript.getCanonicalPath()+" "+tmpSourceDirPath.getCanonicalPath()+" "+targetDirPath.getCanonicalPath()+" "+version+" "+src.toString()+" "+tgt.toString()+" "+type.toString()+" "+mode.toString()+" "+gpuIdx+" "+this.nbestSplitScriptPath.getCanonicalPath();
				logger.debug(cmd);
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
	
	private boolean isTranslatedDir(File sourceDirPath, File targetDirPath) {
		boolean translated=false;
		int srcFilesNumber = IoHdlr.getInstance().getListOfFiles(sourceDirPath, ".txt").size();
		int tgtJsonFilesNumber= IoHdlr.getInstance().getListOfFiles(targetDirPath, ".json").size();
		int tgtTxtFilesNumber= IoHdlr.getInstance().getListOfFiles(targetDirPath, ".txt").size();
		
		if(srcFilesNumber==tgtJsonFilesNumber && srcFilesNumber==tgtTxtFilesNumber) {
			translated=true;
		}
		
		return translated;
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
	
	public String getVersion() {
		return version;
	}

	/**
	 * Used to turn off EDI-NMT if it was started in the server mode. Used by the Query analyzer only
	 */
	public void shutdown() {
		try {
			/**
			 * docker stop instanceName
			 */
			ProcessBuilder processBuilderMarianNMT = new ProcessBuilder("docker", "stop", instanceName);
			Process processMarianNMT = processBuilderMarianNMT.start(); 
			exportLog(processMarianNMT);
			processMarianNMT.waitFor();
			TimeUnit.SECONDS.sleep(30);
	        logger.debug("Edi-nmt image has been closed successfully...");
		} catch (InterruptedException | IOException e) {
			logger.error(e.getMessage());
		}
	}
}
