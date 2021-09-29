/**
 * 
 */
package edu.columbia.dsi.containers;

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

import edu.columbia.dsi.utils.IoHdlr;
import edu.columbia.dsi.utils.ResourceFactory;

/**
 * @author badrashiny
 * Oct 2, 2019
 */
public class ProcessingUnit {
	protected ResourceFactory resourceFactory = new ResourceFactory();
	protected String version=null;
	protected boolean validityFlag=false;
	protected boolean outputValidityFlag=true;

	
	protected void exportStdOutputLog(Process process, Logger logger) {
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

	protected boolean isProcessedDir(File inputDirPath, String inputExtintion,  File outputDirPath, String outputExtintion) throws IOException {
		if(inputDirPath==null || !inputDirPath.exists() || outputDirPath==null|| !outputDirPath.exists() || inputExtintion==null || outputExtintion==null || inputExtintion.isEmpty() || outputExtintion.isEmpty()) {
			return false;
		}
		ArrayList<File> srcFiles = IoHdlr.getInstance().getListOfFiles(inputDirPath, inputExtintion);
		ArrayList<File> tgtFiles= IoHdlr.getInstance().getListOfFiles(outputDirPath, outputExtintion);
		boolean isProcessed=true;
		if(srcFiles.size()==tgtFiles.size()) {
			for(int i=0;i<tgtFiles.size();i++) {
				if(isEmptyFile(tgtFiles.get(i)) && !isEmptyFile(srcFiles.get(i))) {
					isProcessed=false;
					break;
				}
			}		
		}else {
			isProcessed=false;
		}
		return isProcessed;
	}
	
	protected boolean isEmptyFile(File file){
		if(file==null || !file.exists()) {
			return true;
		}
		String f=IoHdlr.getInstance().readFile(file);
		if(f==null || f.trim().isEmpty()) {
			return true;
		}else {
			return false;
		}
		
	}
	
	
	public boolean isValid() {
		return validityFlag;
	}
	public boolean isCorrectOutput() {
		return outputValidityFlag;
	}
	public void run(File inputDir, File outDir) {
	}
	
	public String getVersion() {
		return version;		
	}
	public void shutdown() {
	}
}
