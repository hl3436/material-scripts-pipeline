/**
 * 
 */
package edu.columbia.dsi.utils;

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

/**
 * @author badrashiny
 * Jul 19, 2018
 */
public class CLIRPackager {
	private static Logger logger = Logger.getLogger(CLIRPackager.class.getSimpleName());
	private ResourceFactory resourceFactory = new ResourceFactory();
	
	private String clirPackagerVersion=null;
	private File clirPackagerScriptPath=null;
	private String workDir="CLIR-Packager-WorkDir"+UUID.randomUUID().toString()+ (new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()));

	/**
	 * 
	 */
	public CLIRPackager() {
		logger.debug("Initializing the evaluation docker image...");
		this.clirPackagerScriptPath=resourceFactory.getCLIRPackagerScript();
		this.clirPackagerVersion=resourceFactory.getCLIRPackagerVersion();
	}
	
	/**
	 * The parent directory of evidenceCombinationPath and expConfigFilePath must be the same
	 * @param evidenceCombinationPath
	 * @param expConfigFilePath
	 * @param outDir
	 */
	public void createPackage(File evidenceCombinationPath, File expConfigFilePath, File outDir) {		
		try {
			String ecName=evidenceCombinationPath.getName();
			String expConfName=expConfigFilePath.getName();
			String parentDir=evidenceCombinationPath.getParent();// The parent directory of ecName and expConfName must be the same
			
//			WorkDirHdlr workDirHdlr;
//			ArrayList<String>folders=new ArrayList<String>();
//			folders.add(workDir);
//			workDirHdlr = new WorkDirHdlr(folders);	
//			
//			
//			String cmd="sh"+" "+clirPackagerScriptPath.getCanonicalPath()+" "+ecName+" "+expConfName+" "+parentDir+" "+workDirHdlr.getSubFolderHdlr(workDir).getCanonicalPath()+" "+clirPackagerVersion;
//			logger.debug(cmd);
//			Process process = Runtime.getRuntime().exec(cmd);
//			exportLog(process);
//			process.waitFor();
//			
//			File outPkgPathTmp=new File(workDirHdlr.getSubFolderHdlr(this.workDir).getCanonicalPath());
//			ArrayList<File>pakagePaths=IoHdlr.getInstance().getListOfFiles(outPkgPathTmp);
//			for(File pkg:pakagePaths) {
//				if(pkg.getName().equals("results.tgz")) {
////					continue;
//					File pkgOutPath=new File(outDir+File.separator+pkg.getName());
//					FileUtils.copyFile(pkg, pkgOutPath);
//				}else if(pkg.getName().equals("output.log")) {
//					logger.debug(IoHdlr.getInstance().readFile(pkg));
//				}else {
//					File pkgOutPath=new File(outDir+File.separator+pkg.getName());
//					logger.debug("CLIR output package: "+pkgOutPath.getCanonicalPath());
//					FileUtils.copyFile(pkg, pkgOutPath);
//				}								
//			}
//			
//			workDirHdlr.cleanup();
			
			String cmd="sh"+" "+clirPackagerScriptPath.getCanonicalPath()+" "+ecName+" "+expConfName+" "+parentDir+" "+outDir.getCanonicalPath()+" "+clirPackagerVersion;
			logger.debug(cmd);
			Process process = Runtime.getRuntime().exec(cmd);
			exportLog(process);
			process.waitFor();
			
		} catch (IOException | InterruptedException e) {
			logger.fatal(e.getMessage());
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
