/**
 * 
 */
package edu.columbia.dsi.summarization;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import edu.columbia.dsi.utils.IoHdlr;
import edu.columbia.dsi.utils.ResourceFactory;
import edu.columbia.dsi.utils.WorkDirHdlr;

/**
 * @author badrashiny
 * Jun 21, 2018
 */
public class CUsummarizer {
	private static Logger logger = Logger.getLogger(CUsummarizer.class.getSimpleName());
	private boolean validityFlag=false;
	private String instanceName=UUID.randomUUID().toString(); //Generate a random docker instance name
	private ResourceFactory resourceFactory = new ResourceFactory();
	
	private File startServerScriptPath=null;
	private File summarizeQueriesScriptPath=null;
	private File packagerScriptPath=null;
	private File outDir=null;
	private File ecDir=null;// the output directory of the evidence combination
	private File qpDir=null;// the output directory of the query processor
	private File expFile=null;
	private String nistDataDir=null;
	private String versionNumber=null;
	private int maxthread;
	private boolean serverIsUp=false;
	
	private WorkDirHdlr workDirHdlr;
	private String workDir="CU-Summarizer-WorkDir"+UUID.randomUUID().toString()+ (new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()));
	private boolean liteMode=false; //i.e. accurate mode. If true, it will use the lite mode. the lite mode is faster but the accurate mode is more accurate
//	private File serverLogTmp=null;

	/**
	 * 
	 */
	public CUsummarizer(String nistDataDir, File ecDir, File qpDir,  File expFile, File outDir, boolean liteMode) {
		this.nistDataDir=nistDataDir;
		this.ecDir=ecDir;
		this.qpDir=qpDir;
		this.outDir=outDir;	
		this.expFile=expFile;
		this.startServerScriptPath=resourceFactory.getCUSummarizerStartServerScript();
		this.summarizeQueriesScriptPath=resourceFactory.getCUSummarizeQueriesScript();
		this.packagerScriptPath=resourceFactory.getCUSummarizerPackagerScript();	
		this.versionNumber=resourceFactory.getCUSummarizerVersion();
		this.maxthread=resourceFactory.getCUSummarizerMaxConcurrentTasks();
		this.liteMode=liteMode;
		
		ArrayList<String>folders=new ArrayList<String>();
		folders.add(workDir);
		workDirHdlr = new WorkDirHdlr(folders);
//		this.serverLogTmp= new File(workDirHdlr.getSubFolderHdlr(this.workDir).getAbsolutePath()+File.separator+"server_log.txt");
		
	}
	
	public boolean isValid() {
		return validityFlag;
	}
	
	public void startServer(){	
		try {
			File expConfigFileTmp= new File(workDirHdlr.getSubFolderHdlr(this.workDir).getCanonicalPath()+File.separator+this.expFile.getName());
			FileUtils.copyFile(this.expFile, expConfigFileTmp);
			String tmp =String.valueOf(this.liteMode);					
			String liteModeFlag=tmp.substring(0, 1).toUpperCase()+tmp.substring(1);
			String cmd="sh"+" "+startServerScriptPath.getCanonicalPath()+" "+this.nistDataDir+" "+this.ecDir.getCanonicalPath()+" "+this.qpDir;
			cmd+=" "+workDirHdlr.getSubFolderHdlr(this.workDir).getCanonicalPath()+" "+this.outDir;
			cmd+=" "+this.instanceName+" "+liteModeFlag+" "+this.versionNumber;
//			cmd+=" "+this.instanceName+" "+this.versionNumber+" > "+this.serverLogTmp.getCanonicalPath();
			logger.debug(cmd);
			Process process= Runtime.getRuntime().exec(cmd);
			BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line="";
			while ((line = input.readLine()) != null) {
			   if (line.contains("Summarization server is ready")) {
				   logger.debug(line);
				   TimeUnit.SECONDS.sleep(30);
				   break;
			   }else {
				   logger.debug(line);
			   }
			}
	        validityFlag=true;	
	        serverIsUp=true;
		}catch (IOException | InterruptedException e) {
			logger.error("Couldn't start the CU-summarizer server");
			validityFlag=false;
			serverIsUp=false;
		}
		
	}
	
	public void summarizeQuerySearchResults(ArrayList<String> queriesIDs){
		File queriesIDsListTmp=new File(workDirHdlr.getSubFolderHdlr(this.workDir).getAbsolutePath()+File.separator+"QueriesIDs_"+UUID.randomUUID().toString()+ (new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime())));
		IoHdlr.getInstance().exportFile(queriesIDs, queriesIDsListTmp);
		try {
			
			logger.debug("Summarizing all quieres...");	
			String cmd="sh"+" "+this.summarizeQueriesScriptPath.getCanonicalPath()+" "+this.instanceName+" "+queriesIDsListTmp.getCanonicalPath()+" "+String.valueOf(this.maxthread);
			logger.debug(cmd);
			
			Process process = Runtime.getRuntime().exec(cmd);
			exportLog(process);
			process.waitFor();	
			if(queriesIDsListTmp.exists()) {
				queriesIDsListTmp.delete();
			}
		} catch (IOException | InterruptedException e) {
			logger.error(e.getMessage());
		}			
	}
	
	public void createPackage(String name) {		
		try {
			logger.debug("Packaging the summrizer output...");	
			String cmd="sh"+" "+this.packagerScriptPath.getCanonicalPath()+" "+this.instanceName+" "+String.valueOf(name);
			logger.debug(cmd);			
			Process process = Runtime.getRuntime().exec(cmd);
			exportLog(process);
			process.waitFor();		
//			File outPkgPathTmp=new File(workDirHdlr.getSubFolderHdlr(this.workDir).getCanonicalPath()+File.separator+"package");
//			ArrayList<File>pakagePaths=IoHdlr.getInstance().getListOfFiles(outPkgPathTmp);
//			for(File pkg:pakagePaths) {
//				if(pkg.getName().equals("summary-package.tgz")) {
////					continue;
//					File pkgOutPath=new File(this.outDir.getParent()+File.separator+pkg.getName());
//					FileUtils.copyFile(pkg, pkgOutPath);
//				}else {
//					File pkgOutPath=new File(this.outDir.getParent()+File.separator+pkg.getName());
//					logger.debug("Summarization output package: "+pkgOutPath.getCanonicalPath());
//					FileUtils.copyFile(pkg, pkgOutPath);
//				}								
//			}
		} catch (IOException | InterruptedException e) {
			logger.error(e.getMessage());
		}		
	}
	public String getVersion() {
		return this.versionNumber;
	}
	
	/**
	 * Override the version in the config file
	 * @param version
	 */
	public void setVersion(String version) {
		if(version!=null){
			if(!version.trim().equals(this.versionNumber)) {
				this.versionNumber=version;
				if(serverIsUp) {
					restart();
				}else{
					startServer();
				}				
			}else {
				if(!serverIsUp) {
					startServer();
				}
			}
			
		}		
	}
	
	
	private void restart() {		
		shutdown();
		ArrayList<String>folders=new ArrayList<String>();
		folders.add(workDir);
		workDirHdlr = new WorkDirHdlr(folders);
//		this.serverLogTmp= new File(workDirHdlr.getSubFolderHdlr(this.workDir).getAbsolutePath()+File.separator+"server_log.txt");
		startServer();	
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
	
	
	public void shutdown() {
		try {
			/**
			 * docker stop instanceName
			 */
			ProcessBuilder processBuilder = new ProcessBuilder("docker", "stop", instanceName);
			Process process = processBuilder.start();  
			BufferedReader reader =	new BufferedReader(new InputStreamReader(process.getInputStream()));
			while ((reader.readLine()) != null) {}
			process.waitFor();
			TimeUnit.SECONDS.sleep(10);
			
//			File expectedLogFilePath=new File(workDirHdlr.getSubFolderHdlr(workOutDir).getCanonicalPath()+File.separator+"output.log");
//			if(expectedLogFilePath.exists()) {
//				String log=IoHdlr.getInstance().readFile(expectedLogFilePath);
//				logger.debug(log);
//			}
//			if(this.serverLogTmp.exists()) {
//				logger.debug(IoHdlr.getInstance().readFile(this.serverLogTmp));
//			}
			workDirHdlr.cleanup();	
			serverIsUp=false;
		} catch (InterruptedException | IOException e) {
			logger.fatal(e.getMessage());
		}
	}

}
