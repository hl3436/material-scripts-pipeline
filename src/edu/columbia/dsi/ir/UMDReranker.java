/**
 * 
 */
package edu.columbia.dsi.ir;

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

import org.apache.log4j.Logger;

import edu.columbia.dsi.utils.IoHdlr;
import edu.columbia.dsi.utils.ResourceFactory;
import edu.columbia.dsi.utils.WorkDirHdlr;

/**
 * @author badrashiny
 * Mar 12, 2021
 */
public class UMDReranker {
	private static Logger logger = Logger.getLogger(UMDReranker.class.getSimpleName());
	private ResourceFactory resourceFactory = new ResourceFactory();
	private String rerankerVersion;
	private File rerankerStandaloneScript;
	
	WorkDirHdlr workDirHdlr;
	private String workDir="UMD-ranker"+UUID.randomUUID().toString()+ (new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()));
	

	/**
	 * 
	 */
	public UMDReranker() {
		logger.debug("Initializing the UMD-Reranker...");
		this.rerankerVersion=resourceFactory.getUmdRerankerVersion();		
		this.rerankerStandaloneScript=resourceFactory.getUmdRerankerStandaloneScript();		
		logger.debug("UMD-Reranker is ready.");
	}
	
	/**
	 * @param QueryFileName: the name of the file that is required to be combined
	 * @param inputRootDir: is the parent directory of direcotriesList
	 * @param direcotriesList: a list of any child directory of inputRootDir that are required to be reranked 
	 */
	public void rerank(File configFile, File inputRootDir, ArrayList<String> directoriesList) {
		try {
			String  directories=directoriesList.get(0);
			for(int i=1; i<directoriesList.size();i++) {
				 directories+="+"+directoriesList.get(i);
			}
			logger.debug("reranking: "+inputRootDir.getCanonicalPath()+File.separator+directories);
			ArrayList<String>folders=new ArrayList<String>();
			folders.add(workDir);
			workDirHdlr = new WorkDirHdlr(folders);			
			
			String cmd="sh"+" "+rerankerStandaloneScript.getCanonicalPath()+" "+directories+" "+configFile.getName();
			cmd+=" "+workDirHdlr.getSubFolderHdlr(workDir).getCanonicalPath()+" "+resourceFactory.getScriptsCorporaPath();
			cmd+=" "+inputRootDir.getCanonicalPath()+" "+configFile.getParent()+" "+rerankerVersion;
			
			logger.debug(cmd);
			
			Process process = Runtime.getRuntime().exec(cmd);
			exportLog(process);
			process.waitFor();	
			
			
			File expectedLogFilePath=new File(workDirHdlr.getSubFolderHdlr(workDir).getCanonicalPath()+File.separator+"output.log");
			if(expectedLogFilePath.exists()) {
				String log=IoHdlr.getInstance().readFile(expectedLogFilePath);
				logger.debug(log);
				expectedLogFilePath.delete();
			}
			workDirHdlr.cleanup();
		}catch (IOException | InterruptedException  e) {
			logger.error(e.getMessage());			
		}
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
	
	/**
	 * Override the version in the config file
	 * @param version
	 */
	public void setVersion(String version) {
		if(version!=null && !version.equals(this.rerankerVersion)) {
			this.rerankerVersion=version;
		}		
	}

}
