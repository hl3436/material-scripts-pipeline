/**
 * 
 */
package edu.columbia.dsi.asr;

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

import edu.columbia.dsi.SCRIPTS.Language;
import edu.columbia.dsi.utils.IoHdlr;
import edu.columbia.dsi.utils.ResourceFactory;

/**
 * @author badrashiny
 * Jul 21, 2018
 */
public class ASRDocExpansion {
	private static Logger logger = Logger.getLogger(ASRDocExpansion.class.getSimpleName());
	private ResourceFactory resourceFactory = new ResourceFactory();
	private File docExpScriptPath=null;
	private ArrayList<Double> thresholds=new ArrayList<Double>();//Default value

	/**
	 * 
	 */
	public ASRDocExpansion() {
		docExpScriptPath=resourceFactory.getDocExpScript();
		thresholds=resourceFactory.getDocExpThreshold();
	}
	
	
	public void expand(File asrPath, File queryAnalysisStore, File outDirBasePath, Language src) {
		try {
			logger.debug("Loading ASR-Document-Expansion-Docker image for "+src.toString());
			logger.debug("Running the ASR-Document-Expansion-Docker image on "+asrPath.getCanonicalPath());
			String docExpVersion=null;
			if(src==Language.sw) {
				docExpVersion=resourceFactory.getDocExpSwahiliVersion();
			}else if(src==Language.tl) {
				docExpVersion=resourceFactory.getDocExpTagalogVersion();
			}else if(src==Language.so) {
				docExpVersion=resourceFactory.getDocExpSomaliVersion();
			}				
			
			for(Double threshold:thresholds) {
				File outDirPath=new File(outDirBasePath.getAbsolutePath()+"-threshold-"+threshold.toString());
				if (! outDirPath.exists()){
					outDirPath.mkdir();
				}
				if(!isExpandedDir(asrPath, outDirPath)) {
					String cmd="sh"+" "+docExpScriptPath.getCanonicalPath()+" "+threshold.toString()+" "+asrPath.getCanonicalPath()+" "+queryAnalysisStore.getCanonicalPath();
					cmd+=" "+outDirPath.getCanonicalPath()+" "+docExpVersion;
					logger.debug(cmd);
					Process process = Runtime.getRuntime().exec(cmd);	
					exportLog(process);				
					process.waitFor();
					logger.debug(asrPath.getCanonicalPath()+" is expanded to: "+ outDirPath.getCanonicalPath());
				}else {
					logger.debug("This expansion is already created: "+ outDirPath.getCanonicalPath());
				}				
			}			
		} catch (IOException | InterruptedException e) {
			logger.error(e.getMessage());
		}	
	}
	
	
	
	private boolean isExpandedDir(File inputDirPath, File outputDirPath) {
		boolean expanded=false;
		ArrayList<File> srcFiles = IoHdlr.getInstance().getListOfFiles(inputDirPath,".txt");
		if(outputDirPath.exists()) {
			ArrayList<File> tgtFiles = IoHdlr.getInstance().getListOfFiles(outputDirPath,".txt");		
			if(srcFiles.size()==tgtFiles.size()) {
				expanded=true;	
			}	
		}		
		return expanded;	
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
	
	public String getASRDocExpVersion(Language lang) {
		String docExpVersion=null;
		if(lang==Language.sw) {
			docExpVersion=resourceFactory.getDocExpSwahiliVersion();
		}else if(lang==Language.tl) {
			docExpVersion=resourceFactory.getDocExpTagalogVersion();
		}
		return docExpVersion;		
	}

}
