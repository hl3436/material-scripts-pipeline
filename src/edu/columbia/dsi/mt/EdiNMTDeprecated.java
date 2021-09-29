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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import edu.columbia.dsi.SCRIPTS.Language;
import edu.columbia.dsi.containers.DocInfo.Type;
import edu.columbia.dsi.mt.Translator.TranslatorMode;
import edu.columbia.dsi.utils.IoHdlr;
import edu.columbia.dsi.utils.ResourceFactory;




/**
 * @author badrashiny
 * Mar 8, 2018
 */
public class EdiNMTDeprecated {
	private static Logger logger = Logger.getLogger(EdiNMTDeprecated.class.getSimpleName());
	private boolean validityFlag=false;
	private String instanceName=UUID.randomUUID().toString(); //Generate a random docker instance name
	private String networkName=null;
	private String portNum=null;
	private ResourceFactory resourceFactory = new ResourceFactory();
	private Language src = Language.tl;
	private Language tgt = Language.en;
	private Type type = Type.text;
	private String gpuIdx;
	private int numOfGpus;
	private TranslatorMode mode=TranslatorMode.accurate;
	
	private File startServerScriptPath=null;
	private File translateFileScriptPath=null;
	private File splitNbestSplitScriptPath=null;

	private String ediNMTVer=null;
	/**
	 * 
	 */
	public EdiNMTDeprecated(Language src, Language tgt, Type type, TranslatorMode mode, int[] gpuIdx) {
		try {
			String idx=Integer.toString((gpuIdx[0]));
			for(int i=1;i<gpuIdx.length;i++) {
				idx+=","+Integer.toString((gpuIdx[i]));
			}
			this.gpuIdx = idx;
			this.numOfGpus = gpuIdx.length;
			this.type=type;
			this.mode=mode;
			logger.debug("Loading nmt-Docker image from "+src.toString()+" to "+tgt.toString()+" on GPU "+this.gpuIdx);
			
			this.src = src;
			this.tgt = tgt;
			
			this.networkName=null;
			this.portNum=null;
			
			this.startServerScriptPath=resourceFactory.getEdiNMTStartServerScript();
			this.translateFileScriptPath=resourceFactory.getEdiNMTTranslateFileScript();
			this.splitNbestSplitScriptPath=resourceFactory.getEdiNMTNbestSplitScript();
			
			this.ediNMTVer=resourceFactory.getEdiNMTVersion(src, type);
			
			if(ediNMTVer!=null) {
				startServer();
				validityFlag=true;
				logger.debug("MarianNMT is loaded sucessfuly..."); 
			}else {
				logger.error("Couldn't start the nmt server");
				validityFlag=false;
			}
			
		} catch (IOException | InterruptedException e) {
			logger.error("Couldn't start the nmt server");
			validityFlag=false;
		}
		
	}
	
	/**
	 * initialize the MT on a specific network and a specific name
	 */
	public EdiNMTDeprecated(Language src, Language tgt, Type type, TranslatorMode mode, int[] gpuIdx, String networkName, String instanceName, String portNum) {
		try {
			String idx=Integer.toString((gpuIdx[0]));
			for(int i=1;i<gpuIdx.length;i++) {
				idx+=","+Integer.toString((gpuIdx[i]));
			}
			this.gpuIdx = idx;
			this.numOfGpus = gpuIdx.length;
			this.type=type;
			this.mode=mode;
			logger.debug("Loading nmt-Docker image from "+src.toString()+" to "+tgt.toString()+" on GPU "+this.gpuIdx+" using port number: "+portNum);
			this.src = src;
			this.tgt = tgt;
			
			this.instanceName=instanceName;
			this.networkName=networkName;
			this.portNum=portNum;
			
			this.startServerScriptPath=resourceFactory.getEdiNMTStartServerWithNetworkScript();
			this.translateFileScriptPath=resourceFactory.getEdiNMTTranslateFileScript();
			this.splitNbestSplitScriptPath=resourceFactory.getEdiNMTNbestSplitScript();
			this.ediNMTVer=resourceFactory.getEdiNMTVersion(src, type);
			if(ediNMTVer!=null) {
				startServer();
				validityFlag=true;
				logger.debug("MarianNMT is loaded sucessfuly..."); 
			}else {
				logger.error("Couldn't start the nmt server");
				validityFlag=false;
			}
		} catch (IOException | InterruptedException e) {
			logger.error("Couldn't start the nmt server");
			validityFlag=false;
		}
		validityFlag=true;
		logger.debug("MarianNMT is loaded sucessfuly..."); 
	}
	
	
	
	public boolean isValid() {
		return validityFlag;
	}
	private void startServer() throws IOException, InterruptedException{
		//String nmtVersion=resourceFactory.getEdiNMTVersion();
		
		String cmd="sh"+" "+ startServerScriptPath.getCanonicalPath()+" "+src.toString()+" "+tgt.toString()+" "+type.toString()+" "+mode.toString()+" "+instanceName;
		if(networkName!=null) {//i.e. start on a specific network
			cmd+=" "+networkName;
		}
		cmd+=" "+this.ediNMTVer+" "+gpuIdx;
		if(portNum!=null) {
			cmd+=" "+portNum;
		}
		logger.debug(cmd);
		Runtime.getRuntime().exec(cmd);
		
        TimeUnit.SECONDS.sleep(180);//MarianNMT needs ~180 sec. to load the models into the memory
	}
	
	public boolean translateDir(File sourceDirPath, File targetDirPath, String sourceFileExtension) {
		boolean isNoError=true;
		try {
			logger.debug("Translating directory: "+ sourceDirPath.getCanonicalPath());
			ArrayList<File> srcFiles = IoHdlr.getInstance().getListOfFiles(sourceDirPath, sourceFileExtension);
			for(int i=0;i<srcFiles.size();i++) {
				File inputFile=srcFiles.get(i);
				File outputFile = new File (targetDirPath.getCanonicalPath()+File.separator+inputFile.getName());
				if(!translateFile(inputFile,outputFile)) {// we only do that with the edi-NMT because it is the only one that can miss a file
					isNoError=false;
					restart();
				}
			}
		} catch (IOException e) {
			logger.error(e.getMessage());
			isNoError=false;
			restart();
		}
		return isNoError;//if false-->at least one file is not translated 	
	}
	
	public boolean translateDirMultiThred(File sourceDirPath, File targetDirPath, String sourceFileExtension) {
		boolean isNoError=true;
		ArrayList<Future<Boolean>>openThreads=new ArrayList<Future<Boolean>>();
//		int availableThreads=this.numOfGpus*(int)Math.max(1,(resourceFactory.getRamPerGPU()/16));
		int availableThreads=this.numOfGpus;
		int availableRunsTmp=availableThreads;
		ExecutorService executorService = Executors.newFixedThreadPool(this.numOfGpus);
		try {
			
			logger.debug("Translating directory: "+ sourceDirPath.getCanonicalPath());
			ArrayList<File> srcFiles = IoHdlr.getInstance().getListOfFiles(sourceDirPath, sourceFileExtension);
			int i=0;
			while(i<srcFiles.size()) {
				File inputFile=srcFiles.get(i);
				File outputFile = new File (targetDirPath.getCanonicalPath()+File.separator+inputFile.getName());
				if(availableRunsTmp>0) {// there is a free gpu
					openThreads.add(startThread(executorService, inputFile, outputFile));
					availableRunsTmp--;
					i++;
				}else { //all gpus are used. we need to wait for them to be available
					if(!cleanupOpenThreads(openThreads)) {//i.e. at least one file crashed
						isNoError=false;
						restart();
					}
					availableRunsTmp=availableThreads;
					openThreads=new ArrayList<Future<Boolean>>();
					openThreads.add(startThread(executorService, inputFile, outputFile));
					availableRunsTmp--;
					i++;					
				}
			}
			if(!cleanupOpenThreads(openThreads)) {//i.e. at least one file crashed
				isNoError=false;
				restart();
			}
			
		} catch (IOException e) {
			logger.error(e.getMessage());
			isNoError=false;
			restart();
		}
		return isNoError;//if false-->at least one file is not translated 	
	}
	
	private boolean cleanupOpenThreads(ArrayList<Future<Boolean>>openThreads) {
		boolean isNoError=true;
		for(int k=0;k<openThreads.size();k++) {
			try {
				if(!openThreads.get(k).get()) {
					isNoError =false;
				}
			} catch (InterruptedException | ExecutionException e) {
				logger.fatal(e.getMessage());
				isNoError =false;
			}					
		}	
		return isNoError;
	}

	private Future<Boolean> startThread(ExecutorService executorService, File sourceFilePath, File targetFilePath) {
		File sourceFilePathTmp = sourceFilePath;
		File targetFilePathTmp = targetFilePath;
		
		Future<Boolean> future = executorService.submit(new Callable<Boolean>() {
			public Boolean call() throws Exception {
		    	
		    	return translateFile(sourceFilePathTmp, targetFilePathTmp);
		    }
		});	
		return future;
	}
	
	public boolean translateFile(File sourceFilePath, File targetFilePath){	
		boolean isNoError=true;
		try {			
			if( isEmptyFile(targetFilePath)) {//i.e. the file has not been translated before. Or it crashed during the previous trial
				logger.debug("Translating "+sourceFilePath.getCanonicalPath());	
				
				String cmd="bash"+" "+ translateFileScriptPath.getCanonicalPath()+" "+src.toString()+" "+tgt.toString()+" "+type.toString()+" "+sourceFilePath.getCanonicalPath();
				if(mode==TranslatorMode.accurate) {
					cmd+=" "+targetFilePath.getCanonicalPath().replace(".txt", "");
				}else {
					cmd+=" "+targetFilePath.getCanonicalPath();
				}			
				cmd+=" "+instanceName+" "+ mode.toString()+" "+this.splitNbestSplitScriptPath.getCanonicalPath();
				logger.debug(cmd);
				Process process = Runtime.getRuntime().exec(cmd);
				
				exportLog(process);	
				process.waitFor();
				
				/**
				 * The MT has a memory leak problem. Sometimes it crashes and need to be restarted.
				 */
				if( isEmptyFile(targetFilePath)) {//i.e. the file crashed during the previous trial		
					isNoError=false;
					logger.error("Can not translate this file: "+sourceFilePath.getAbsolutePath());			
				}else {
					logger.debug(sourceFilePath.getCanonicalPath()+ " is translated to: "+ targetFilePath.getCanonicalPath());
				}			    
			}else {
				logger.debug("This file is already tranlsted: "+sourceFilePath.getAbsolutePath());
			}
					
		} catch (IOException | InterruptedException e) {
			logger.error(e.getMessage());
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
	private boolean isEmptyFile(File file){
		if(file==null || !file.exists()) {
			return true;
		}
		String emptyString="OCI runtime exec failed: exec failed";
		String fileStr=IoHdlr.getInstance().readFile(file);
		if(fileStr==null || fileStr.isEmpty()|| fileStr.contains(emptyString)) {//i.e. the file is empty or crashed before translation
			return true;
		}else {
			return false;
		}		
	}
	
	public void restart() {		
		try {
			shutdown();
			startServer();
		} catch (IOException | InterruptedException e) {
			logger.fatal(e.getMessage());
		}
		
	}
	
	
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
	        logger.debug("nmt image has been closed successfully...");
		} catch (InterruptedException | IOException e) {
			logger.error(e.getMessage());
		}
	}
	public String getVersion() {
		return this.ediNMTVer;
	}

}
