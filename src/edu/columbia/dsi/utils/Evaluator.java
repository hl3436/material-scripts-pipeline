/**
 * 
 */
package edu.columbia.dsi.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

/**
 * @author badrashiny
 * Jul 19, 2018
 */
public class Evaluator {
	private static Logger logger = Logger.getLogger(Evaluator.class.getSimpleName());
	private ResourceFactory resourceFactory = new ResourceFactory();
	
	private String evalVersion=null;
	private File evalScriptPath=null;
//	private double clirBetaValue=20;
//	private double clirPlusSBetaValue=59.9;
	
	String clirEvalOutDir="CLIR-Eval";
	String clirPlusSEvalOutDir="CLIR+S-Eval";
	
	/**
	 * 
	 */
	public Evaluator() {
		logger.debug("Initializing the evaluation docker image...");
		this.evalScriptPath=resourceFactory.getEvaluatorScript();
		this.evalVersion=resourceFactory.getEvaluatorVersion();
	}
	public void setEvalVersion(String version) {
		if(version!=null) {
			this.evalVersion=version;
		}		
	}
	/**
	 * The parent directory of evidenceCombinationPath and expConfigFilePath must be the same
	 * @param evidenceCombinationPath
	 * @param expConfigFilePath
	 * @param outDir
	 */
	public void evaluate(File evidenceCombinationPath, File expConfigFilePath, String referenceDataLocation, File outDir) {
		try {
			String ecName=evidenceCombinationPath.getName();
			String expConfName=expConfigFilePath.getName();
			String parentDir=evidenceCombinationPath.getParent();// The parent directory of ecName and expConfName must be the same
			
			String clirEval=outDir.getCanonicalPath()+File.separator+clirEvalOutDir;
			String clirPlusSEval=outDir.getCanonicalPath()+File.separator+clirPlusSEvalOutDir;
			
//			String referenceDataLocation=resourceFactory.getScriptsCorporaPath();
			logger.debug("Evaluating CLIR");
//			evaluate(ecName, expConfName, parentDir, relevJudgmentsPath, this.clirBetaValue, clirEval);
			evaluate(ecName, expConfName, parentDir, referenceDataLocation, clirEval);
	
//			logger.debug("Evaluating CLIR+S");
//			evaluate(ecName, expConfName, parentDir, relevJudgmentsPath, this.clirPlusSBetaValue, clirPlusSEval);
		} catch (IOException e) {
			logger.fatal(e.getMessage());
		}	
	}
	
	
	private void evaluate(String ecName, String expConfName, String parentDir, String referenceDataLocation, String outDir) {
		try {
//			String cmd="sh"+" "+evalScriptPath.getCanonicalPath()+" "+ecName+" "+expConfName+" "+String.valueOf(beta)+" "+parentDir+" "+outDir+" "+relevJudgmentsPath+" "+evalVersion;
			String cmd="sh"+" "+evalScriptPath.getCanonicalPath()+" "+ecName+" "+expConfName+" "+parentDir+" "+outDir+" "+referenceDataLocation+" "+evalVersion;
			
			logger.debug(cmd);
			Process process = Runtime.getRuntime().exec(cmd);
			exportLog(process);
			process.waitFor();					
		} catch (IOException | InterruptedException e) {
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

}
