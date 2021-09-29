/**
 * 
 */
package edu.columbia.dsi.containers;

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

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import edu.columbia.dsi.ir.UmdCLIR.UmdCLIRCombFormat;
import edu.columbia.dsi.utils.IoHdlr;
//import edu.columbia.dsi.ir.UmdCLIR.UmdCLIRMatchingType;
import edu.columbia.dsi.utils.ResourceFactory;
import edu.columbia.dsi.utils.WorkDirHdlr;

/**
 * @author badrashiny
 * May 8, 2018
 */
public class UmdMatcherConfig {
	private static Logger logger = Logger.getLogger(UmdMatcherConfig.class.getSimpleName());
	private ResourceFactory resourceFactory = new ResourceFactory();
	private ArrayList<String>indexes;
	private String matchingType;
	private int matchingCutoff;
	private UmdCLIRCombFormat matchingFormat; 
	private String configName; // A name for this configuration
	private String configMatcherUniqueIdentifier; // A unique identifier for this matcher configuration
	private String configRankerUniqueIdentifier; // A unique identifier for this ranker configuration
	private enum HashingType {matcher, ranker;}

	/**
	 * 
	 */
	public UmdMatcherConfig(ArrayList<String>indexes, String matchingType, int matchingCutoff, UmdCLIRCombFormat matchingFormat, String configName, File configFile) {
		this.indexes=new ArrayList<String>();
		this.indexes.addAll(indexes);
		this.matchingType=matchingType;
		this.matchingCutoff=matchingCutoff;
		this.matchingFormat=matchingFormat;
		this.configName=configName;
		this.configMatcherUniqueIdentifier=generateMathcerUniqueID(configName,configFile, HashingType.matcher);
		this.configRankerUniqueIdentifier=generateMathcerUniqueID(configName,configFile, HashingType.ranker);
	}
	
	private String generateMathcerUniqueID(String configName, File configFile, HashingType type) {
		String uniqueID=configName;
		try {
			String uniqueIdVersion=resourceFactory.getUmdQueryMatcherUniqueIdVersion();
			File uniqueIdScript=resourceFactory.getUmdQueryMatcherUniqueIdScript();
			
			String workDir="UMD-Matcher-UniqueID-WorkDir"+UUID.randomUUID().toString()+ (new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()));
			ArrayList<String>folders=new ArrayList<String>();
			folders.add(workDir);		
			WorkDirHdlr workDirHdlr = new WorkDirHdlr(folders);
			
			File expConfigFileTmp= new File(workDirHdlr.getSubFolderHdlr(workDir).getCanonicalPath()+File.separator+configFile.getName());
			String matcherUniqueID_outFileName="matcher_unique_id.txt";
			
			FileUtils.copyFile(configFile, expConfigFileTmp);
			
			String cmd="sh"+" "+uniqueIdScript.getCanonicalPath()+" "+workDirHdlr.getSubFolderHdlr(workDir).getCanonicalPath()+" "+expConfigFileTmp.getName();
			cmd+=" "+configName+" "+matcherUniqueID_outFileName+" "+type.toString()+" "+uniqueIdVersion;
			logger.debug(cmd);
			
			Process process = Runtime.getRuntime().exec(cmd);
			exportLog(process);
			process.waitFor();	
			
			File expectedOutputFilePath=new File(workDirHdlr.getSubFolderHdlr(workDir).getCanonicalPath()+File.separator+matcherUniqueID_outFileName);
			if(expectedOutputFilePath.exists()) {
				uniqueID=IoHdlr.getInstance().readFile(expectedOutputFilePath).trim().replaceAll("\n", "").trim();				
			}
			workDirHdlr.cleanup();
		
		}catch (IOException | InterruptedException e) {
			return uniqueID;
		}		
		return uniqueID;		
	}
	
	public ArrayList<String> getMatchingIndexes(){
		return indexes;
	}
	
	public String getMatchingType(){
		return matchingType;
	}
	
	
	public int getMatchingCutOff(){
		return matchingCutoff;
	}
	
	public UmdCLIRCombFormat getMatchingFormat(){
		return matchingFormat;
	}
	
	public String getConfigName(){
		return configName;
	}
	
	public String getConfigMatcherUniqueIdentifier() {
		return this.configMatcherUniqueIdentifier;
	}
	
	public String getConfigRankerUniqueIdentifier() {
		return this.configRankerUniqueIdentifier;
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
}
