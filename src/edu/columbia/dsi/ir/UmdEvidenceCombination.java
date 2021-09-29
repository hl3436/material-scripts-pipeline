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

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import edu.columbia.dsi.ir.UmdCLIR.UmdCLIRCombFormat;
import edu.columbia.dsi.utils.IoHdlr;
import edu.columbia.dsi.utils.ResourceFactory;
import edu.columbia.dsi.utils.WorkDirHdlr;

/**
 * @author badrashiny
 * May 7, 2018
 */
public class UmdEvidenceCombination {
	private static Logger logger = Logger.getLogger(UmdEvidenceCombination.class.getSimpleName());
	private ResourceFactory resourceFactory = new ResourceFactory();
	private String ecVersion;
//	private double ecNumericVersion;
	private File ecScript;
	private File ecStandaloneScript;
	
	WorkDirHdlr workDirHdlr;
	private String workOutDir="UMD-Evidence-combination-WorkOutDir"+UUID.randomUUID().toString()+ (new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()));
	
	

	/**
	 * 
	 */
	public UmdEvidenceCombination() {
		logger.debug("Initializing the UMD-Evidence-Combiner...");
		this.ecVersion=resourceFactory.getUmdEvidenceCombinationVersion();		
//		this.ecNumericVersion=Double.valueOf(this.ecVersion.split(":")[1].replace("v", "")).doubleValue();
		this.ecScript=resourceFactory.getUmdEvidenceCombinationScript();
		this.ecStandaloneScript=resourceFactory.getUmdEvidenceCombinationStandaloneScript();		
		logger.debug("UMD-Evidence-Combiner is ready.");
	}
//	/**
//	 * This function should be used with version 1 only. It is supporting patch mode. It combines all the files under the queryMatchedDirecotries
//	 * @param inputRootDir: is the parent directory of any output directory from a UMD-Query-Matcher
//	 * @param queryMatchedDirecotries: a list of any child directory of inputRootDir that are required to be combined 
//	 */
//	public void evidenceCombine(File inputRootDir, ArrayList<String> queryMatchedDirecotriesList, File outputDir, int cutOff, UmdCLIRCombFormat inFormat, UmdCLIRCombFormat outFormat) {
//		try {
//			String  queryMatchedDirecotries=queryMatchedDirecotriesList.get(0);
//			for(int i=1; i<queryMatchedDirecotriesList.size();i++) {
//				 queryMatchedDirecotries+="+"+queryMatchedDirecotriesList.get(i);
//			}
//			if(ecNumericVersion > 1.0) {
//				logger.error("The used Evedence combiantion version doesn't support patch mode. Please use the other function or change the Evidence combination version");
//				
//			}else {
//				logger.debug("combining evidance of: "+inputRootDir.getCanonicalPath()+File.separator+queryMatchedDirecotries);
//				
//				String cmd="sh"+" "+ecScript.getCanonicalPath()+" "+queryMatchedDirecotries+" "+String.valueOf(cutOff);
//				cmd+=" "+inFormat.toString()+" "+outFormat.toString()+" "+inputRootDir.getCanonicalPath()+" "+outputDir.getCanonicalPath()+" "+ecVersion;
//				logger.debug(cmd);
//				
//				Process process = Runtime.getRuntime().exec(cmd);
//				exportLog(process);
//				process.waitFor();	
//			}
//			
//			
//		}catch (IOException | InterruptedException  e) {
//			logger.error(e.getMessage());			
//		}
//	}
	
//	/**
//	 * This function should be used with version 2 and up. It doesn't support patch mode. It combines the files related to the input query ID under the queryMatchedDirecotries
//	 * @param QueryFileName: the name of the file that is required to be combined
//	 * @param inputRootDir: is the parent directory of any output directory from a UMD-Query-Matcher
//	 * @param queryMatchedDirecotries: a list of any child directory of inputRootDir that are required to be combined 
//	 */
//	public void evidenceCombine(String queryFileName, File inputRootDir, ArrayList<String> queryMatchedDirecotriesList, File outputDir, int cutOff, UmdCLIRCombFormat inFormat, UmdCLIRCombFormat outFormat) {
//		try {
//			String  queryMatchedDirecotries=queryMatchedDirecotriesList.get(0);
//			for(int i=1; i<queryMatchedDirecotriesList.size();i++) {
//				 queryMatchedDirecotries+="+"+queryMatchedDirecotriesList.get(i);
//			}
//			if(ecNumericVersion < 2.0) {
//				logger.error("The used Evedence combiantion version doesn't support query by query call. Please use the other function or change the Evidence combination version");
//				
//			}else {
//				logger.debug("combining evidance of: "+inputRootDir.getCanonicalPath()+File.separator+queryMatchedDirecotries+ "for this query: "+ queryFileName);
//				String cmd="sh"+" "+ecStandaloneScript.getCanonicalPath()+" "+queryMatchedDirecotries+" "+String.valueOf(cutOff);
//				cmd+=" "+inFormat.toString()+" "+outFormat.toString()+" "+queryFileName+" "+inputRootDir.getCanonicalPath()+" "+outputDir.getCanonicalPath()+" "+ecVersion;
//				logger.debug(cmd);
//				
//				Process process = Runtime.getRuntime().exec(cmd);
//				exportLog(process);
//				process.waitFor();	
//			}		
//		}catch (IOException | InterruptedException  e) {
//			logger.error(e.getMessage());			
//		}
//	}
	
	/**
	 * This function should be used with version 3 and up. It doesn't support patch mode. It combines the files related to the input query ID under the queryMatchedDirecotries
	 * @param QueryFileName: the name of the file that is required to be combined
	 * @param inputRootDir: is the parent directory of any output directory from a UMD-Query-Matcher
	 * @param queryMatchedDirecotries: a list of any child directory of inputRootDir that are required to be combined 
	 */
	public void evidenceCombine(File queryProcessorOutDir, File configFile, String queryFileName, File inputRootDir, ArrayList<String> queryMatchedDirecotriesList, File outputDir) {
		try {
			String  queryMatchedDirecotries=queryMatchedDirecotriesList.get(0);
			for(int i=1; i<queryMatchedDirecotriesList.size();i++) {
				 queryMatchedDirecotries+="+"+queryMatchedDirecotriesList.get(i);
			}
			logger.debug("combining evidance of: "+inputRootDir.getCanonicalPath()+File.separator+queryMatchedDirecotries+ "for this query: "+ queryFileName);
			ArrayList<String>folders=new ArrayList<String>();
			folders.add(workOutDir);
			workDirHdlr = new WorkDirHdlr(folders);			
			
			String cmd="sh"+" "+ecStandaloneScript.getCanonicalPath()+" "+queryMatchedDirecotries+" "+queryFileName+" "+configFile.getName();
			cmd+=" "+inputRootDir.getCanonicalPath()+" "+queryProcessorOutDir.getCanonicalPath()+" "+configFile.getParent();
			cmd+=" "+resourceFactory.getScriptsCorporaPath()+" "+workDirHdlr.getSubFolderHdlr(workOutDir).getCanonicalPath()+" "+ecVersion;
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
			
			/**copy  all other remaining files
			 * 
			 */
			ArrayList<File>remFiles=IoHdlr.getInstance().getListOfFiles(workDirHdlr.getSubFolderHdlr(workOutDir));
//			if(remFiles.size()<3) {
//				System.out.println(queryFileName+":"+remFiles.size());
//				logger.fatal(queryFileName+":"+remFiles.size());
//			}
			for(File f:remFiles) {
				File outFile=new File(outputDir.getCanonicalPath()+File.separator+f.getName());
				FileUtils.copyFile(f, outFile);	
				f.delete();
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
		if(version!=null && !version.equals(ecVersion)) {
			this.ecVersion=version;
		}		
	}
	
	
	public String getVersion() {
		return ecVersion;
	}

}
