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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import edu.columbia.dsi.SCRIPTS.Language;
import edu.columbia.dsi.utils.IoHdlr;
import edu.columbia.dsi.utils.ResourceFactory;
import edu.columbia.dsi.utils.WorkDirHdlr;

/**
 * @author badrashiny
 * May 7, 2018
 */
public class UmdQueryMatcher {
	private static Logger logger = Logger.getLogger(UmdQueryMatcher.class.getSimpleName());
	private ResourceFactory resourceFactory = new ResourceFactory();
	private String qmVersion;
	private double qmNumericVersion;
	private int qmPortNumber;
	private File qmServerScript;
	private File qmClientScript;
	private File qmStandaloneScript;
	private File indexRootPath; //EX: /storage/data/NIST-data
	private WorkDirHdlr workDirHdlr;
	private String workInDir="UMD-Query-Matcher-WorkInDir"+UUID.randomUUID().toString()+ (new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()));
	private String workOutDir="UMD-Query-Matcher-WorkOutDir"+UUID.randomUUID().toString()+ (new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()));
	
	private String instanceName=UUID.randomUUID().toString(); //Generate a random docker instance name

	/**
	 * 
	 */
	public UmdQueryMatcher() {
		logger.debug("Initializing the UMD-QueryMAtcher...");
		this.qmVersion=resourceFactory.getUmdQueryMatcherVersion();
		this.qmNumericVersion=Double.valueOf(this.qmVersion.split(":")[1].replace("v", "")).doubleValue();
		this.qmStandaloneScript=resourceFactory.getUmdQueryMatcherStandaloneScript();
		this.qmPortNumber=resourceFactory.getUmdQueryMatcherPortNumber();
		this.qmServerScript=resourceFactory.getUmdQueryMatcherServerScript();
		this.qmClientScript=resourceFactory.getUmdQueryMatcherClientScript();
		this.indexRootPath=new File(resourceFactory.getScriptsCorporaPath());
		
		ArrayList<String>folders=new ArrayList<String>();
		folders.add(workInDir);
		folders.add(workOutDir);
		workDirHdlr = new WorkDirHdlr(folders);
		if(this.qmNumericVersion< 4.0) {//i.e. old client/server approach
			startServer();
		}
		
		logger.debug("UMD-Matcher-Server is ready.");
	}
	
	private void startServer() {
		try {
			logger.debug("Starting the UMD-Matcher-Server...");
			String cmd="sh"+" "+qmServerScript.getCanonicalPath()+" "+String.valueOf(qmPortNumber)+" "+indexRootPath.getCanonicalPath();
			cmd+=" "+workDirHdlr.getSubFolderHdlr(workInDir).getCanonicalPath();
			cmd+=" "+workDirHdlr.getSubFolderHdlr(workOutDir).getCanonicalPath()+" "+instanceName+" "+qmVersion;
			logger.debug(cmd);
			Runtime.getRuntime().exec(cmd);
//			BufferedReader reader =	new BufferedReader(new InputStreamReader(process.getInputStream()));
//			while ((reader.readLine()) != null) {}
//			process.waitFor();		
			TimeUnit.SECONDS.sleep(20);
		} catch (IOException | InterruptedException e) {
			logger.error(e.getMessage());			
		}
	}

	public void matchQueries(File configFile,Language tgtLang, File processedQueriesDir, File outMatchedQueriesDir, ArrayList<String> indexesPaths, String type, int cutOff, String matcherName) {
		try {
			Hashtable<String, HashSet<File>> queriesNames = IoHdlr.getInstance().getListOfFilesGrouped(processedQueriesDir);
			Set<Entry<String, HashSet<File>>> queriesNamesSet = queriesNames.entrySet();
			for(Entry<String, HashSet<File>> qNameSet : queriesNamesSet) {
				matchQuery(configFile, tgtLang, qNameSet, outMatchedQueriesDir, indexesPaths, type, cutOff, matcherName);
			}
		}  catch (Exception e) {
			logger.fatal(e.getMessage());
		}		
	}
	
	public void matchQuery(File configFile, Language tgtLang, Entry<String, HashSet<File>> processedQuery, File outMatchedQueriesDir, ArrayList<String> indexesPaths, String type, int cutOff, String matcherName) {
		try {
			logger.debug("Matching the input query: "+processedQuery.getKey());
			String indexPath=indexesPaths.get(0);
			for(int i=1; i<indexesPaths.size();i++) {
				indexPath+="+"+indexesPaths.get(i);
			}
			
			for(File f:processedQuery.getValue()) {
				File tmpPath=new File(workDirHdlr.getSubFolderHdlr(workInDir).getCanonicalPath()+File.separator+f.getName());
				FileUtils.copyFile(f, tmpPath);
			}
			
			
//			File expectedInFilePath=new File(workDirHdlr.getSubFolderHdlr(workInDir).getCanonicalPath()+File.separator+processedQuery.getKey());
//			FileUtils.copyFile(processedQuery, expectedInFilePath);
			String cmd=null;
			
			cmd="sh"+" "+qmStandaloneScript.getCanonicalPath()+" "+indexRootPath.getCanonicalPath();
			cmd+=" "+workDirHdlr.getSubFolderHdlr(workInDir).getCanonicalPath();
			cmd+=" "+workDirHdlr.getSubFolderHdlr(workOutDir).getCanonicalPath()+" "+instanceName+" "+qmVersion;
			cmd+=" "+processedQuery.getKey()+" "+indexPath+" "+type+" "+String.valueOf(cutOff)+" "+tgtLang.toString()+" "+matcherName;
			cmd+=" "+configFile.getName()+" "+configFile.getParent();
			
			logger.debug(cmd);
			
			Process process = Runtime.getRuntime().exec(cmd);
			exportLog(process);
			process.waitFor();	
			
			
			File expectedLogFilePath=new File(workDirHdlr.getSubFolderHdlr(workOutDir).getCanonicalPath()+File.separator+"output.log");
			if(expectedLogFilePath.exists()) {
				String log=IoHdlr.getInstance().readFile(expectedLogFilePath);
				logger.debug(log);
				expectedLogFilePath.delete();
			}
//			
//			
//			File expectedOutFilePath=new File(workDirHdlr.getSubFolderHdlr(workOutDir).getCanonicalPath()+File.separator+"q-"+processedQuery.getName().replaceAll("\\..*$", "")+"."+outFormat.toString());
//			if(expectedOutFilePath.exists()) {
//				File outFile=new File(outMatchedQueriesDir.getCanonicalPath()+File.separator+expectedOutFilePath.getName());
//				FileUtils.copyFile(expectedOutFilePath, outFile);	
//				expectedOutFilePath.delete();
//			}else {
//				logger.error("Can not match the input query: "+ processedQuery.getCanonicalPath());
//			}	
			
			
			
			/**copy  all other remaining files
			 * 
			 */
			ArrayList<File>remFiles=IoHdlr.getInstance().getListOfFiles(workDirHdlr.getSubFolderHdlr(workOutDir));
			for(File f:remFiles) {
				File outFile=new File(outMatchedQueriesDir.getCanonicalPath()+File.separator+f.getName());
				FileUtils.copyFile(f, outFile);	
				f.delete();
			}
			
			
		} catch (IOException | InterruptedException e) {
			logger.fatal(e.getMessage());
		} 
		
	}
	
	/**
	 * Override the version in the config file
	 * @param version
	 */
	public void setVersion(String version) {
		if(version!=null && !version.equals(qmVersion)) {
			this.qmVersion=version;
			restart();
		}		
	}
	
	private void restart() {
		if(qmNumericVersion< 4.0) {//i.e. old client/server approach
			shutdown();
			ArrayList<String>folders=new ArrayList<String>();
			folders.add(workInDir);
			folders.add(workOutDir);
			workDirHdlr = new WorkDirHdlr(folders);
			startServer();
		}else {
			workDirHdlr.cleanup();
			ArrayList<String>folders=new ArrayList<String>();
			folders.add(workInDir);
			folders.add(workOutDir);
			workDirHdlr = new WorkDirHdlr(folders);			
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
	
	
	
	public String getVersion() {
		return qmVersion;
	}
	public void shutdown() {
		try {
			if(qmNumericVersion< 4.0) {//i.e. old client/server approach
				/**
				 * docker stop instanceName
				 */
				ProcessBuilder processBuilder = new ProcessBuilder("docker", "stop", instanceName);
				Process process = processBuilder.start();  
				BufferedReader reader =	new BufferedReader(new InputStreamReader(process.getInputStream()));
				while ((reader.readLine()) != null) {}
				process.waitFor();
				TimeUnit.SECONDS.sleep(10);
			}			
			File expectedLogFilePath=new File(workDirHdlr.getSubFolderHdlr(workOutDir).getCanonicalPath()+File.separator+"output.log");
			if(expectedLogFilePath.exists()) {
				String log=IoHdlr.getInstance().readFile(expectedLogFilePath);
				logger.debug(log);
			}	
			workDirHdlr.cleanup();			
		} catch (InterruptedException | IOException e) {
		}
	}

}
