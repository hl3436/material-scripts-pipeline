/**
 * 
 */
package edu.columbia.dsi.ir;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import edu.columbia.dsi.SCRIPTS.Language;
import edu.columbia.dsi.containers.DocInfo.Type;
import edu.columbia.dsi.mt.EdiNMT;
import edu.columbia.dsi.mt.EdiNMTDeprecated;
import edu.columbia.dsi.mt.Translator.TranslatorMode;
import edu.columbia.dsi.utils.IoHdlr;
import edu.columbia.dsi.utils.ResourceFactory;
import edu.columbia.dsi.utils.WorkDirHdlr;

/**
 * @author badrashiny
 * May 4, 2018
 */
public class UmdQueryProcessor {
	private Logger logger = Logger.getLogger(UmdQueryProcessor.class.getSimpleName());
	private ResourceFactory resourceFactory = new ResourceFactory();
	private String qpVersion;
	private File qpStandaloneScript;
	private WorkDirHdlr workDirHdlr;
	private String workInDir="UMD-Query-Processor-WorkInDir"+UUID.randomUUID().toString()+ (new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()));
	private String workOutDir="UMD-Query-Processor-WorkOutDir"+UUID.randomUUID().toString()+ (new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()));
	private String workLogDir="UMD-Query-Processor-WorkLogDir"+UUID.randomUUID().toString()+ (new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()));
	
	private String instanceName=UUID.randomUUID().toString(); //Generate a random docker instance name
	private String ediNMTinstanceName=UUID.randomUUID().toString(); //Generate a random docker instance name for EDI-NMT
	private String networkName=UUID.randomUUID().toString(); //Generate a random network name
	private String qExpansionFlag="true"; //query expansion is enabled by default
	private boolean fastMode=false; //fast mode is disabled by default

	/**
	 * 
	 */
	public UmdQueryProcessor() {		
		try {
			logger.debug("Initializing the UMD-QueryProcessor...");
			this.qpVersion=resourceFactory.getUmdQueryProcessorVersion();
			this.qpStandaloneScript=resourceFactory.getUmdQueryProcessorStandaloneScript();
			
			ArrayList<String>folders=new ArrayList<String>();
			folders.add(workInDir);
			folders.add(workOutDir);
			folders.add(workLogDir);
			workDirHdlr = new WorkDirHdlr(folders);
			
			
			ProcessBuilder processBuilder= new ProcessBuilder("docker", "network", "create", networkName);
			Process process = processBuilder.start(); 
			exportLog(process);
			process.waitFor();
			TimeUnit.SECONDS.sleep(30);
			
			logger.debug("UMD-QueryProcessor-Server is ready.");
		} catch (InterruptedException | IOException e) {
			logger.error(e.getMessage());
		}		
		
	}
	
	public UmdQueryProcessor(boolean fastModeFlag) {		
		try {
			logger.debug("Initializing the UMD-QueryProcessor...");
			this.qpVersion=resourceFactory.getUmdQueryProcessorVersion();
			this.qpStandaloneScript=resourceFactory.getUmdQueryProcessorStandaloneScript();
			
			ArrayList<String>folders=new ArrayList<String>();
			folders.add(workInDir);
			folders.add(workOutDir);
			folders.add(workLogDir);
			workDirHdlr = new WorkDirHdlr(folders);
			
			if(fastModeFlag) {
				this.fastMode=true;
				this.qExpansionFlag="false";	
				this.networkName=null;
			}else {
				this.fastMode=false;
				this.qExpansionFlag="true";
				ProcessBuilder processBuilder= new ProcessBuilder("docker", "network", "create", networkName);
				Process process = processBuilder.start(); 
				exportLog(process);
				process.waitFor();
				TimeUnit.SECONDS.sleep(30);
			}
			
			
			logger.debug("UMD-QueryProcessor-Server is ready.");
		} catch (InterruptedException | IOException e) {
			logger.error(e.getMessage());
		}		
		
	}
	
	
	
	public File processQuery(String query, Language tgtLang, File outDir) {
		Random rand = new Random();
		String queryId="queryTmp"+String.valueOf(rand.nextInt(10000));
		String randName=UUID.randomUUID().toString()+".tsv";
		workDirHdlr.prepareWorkDir();
		try {
			File tmpInFile=new File(workDirHdlr.getSubFolderHdlr(workInDir).getCanonicalPath()+File.separator+randName);
			PrintWriter writer= new PrintWriter(tmpInFile.getCanonicalPath());
			writer.println(queryId+"\t"+query);
			
			writer.close();
			boolean res= processQuery(tmpInFile, tgtLang, outDir);
			File expectedOutFilePath=new File(outDir.getCanonicalPath()+File.separator+queryId);
			if(res &&expectedOutFilePath.exists()) {
				return expectedOutFilePath;
			}else {
				return null;
			}			
		} catch (IOException e) {
			logger.fatal(e.getMessage());
			return null;
		}		
	}
	
	public boolean processQuery(String queryId, String query, String domainId, Language tgtLang, File outDir) {
		String randName=UUID.randomUUID().toString()+".tsv";
		workDirHdlr.prepareWorkDir();
		try {
			File tmpInFile=new File(workDirHdlr.getSubFolderHdlr(workInDir).getCanonicalPath()+File.separator+randName);
			PrintWriter writer= new PrintWriter(tmpInFile.getCanonicalPath());
			writer.println(queryId+"\t"+query+"\t"+domainId);
			
			writer.close();
			boolean res= processQuery(tmpInFile, tgtLang, outDir);
			return res;
		} catch (IOException e) {
			logger.fatal(e.getMessage());
			return false;
		}
		
	}
	/**
	 * @param inputQueries
	 * @param tgtLang
	 * @param outDir
	 * @return
	 */
	public boolean processQuery(File inputQueries, Language tgtLang, File outDir) {
		try {
			File expectedInFilePath=new File(workDirHdlr.getSubFolderHdlr(workInDir).getCanonicalPath()+File.separator+inputQueries.getName());
			if(!expectedInFilePath.exists()) {//i.e. this function is not called by processQuery(String queryId, String query, String domainId) 
				workDirHdlr.prepareWorkDir();
				ArrayList<String>lines=IoHdlr.getInstance().readFileLines(inputQueries);
				PrintWriter writer= new PrintWriter(expectedInFilePath.getCanonicalPath());
				if(!lines.get(0).trim().replaceAll("\t", " ").equals("query_id query_string domain_id") &&
				   !lines.get(0).trim().replaceAll("\t", " ").equals("query_id query_string")){//check if it has a header
					writer.println(lines.get(0).trim());
				}
				for(int i=1;i<lines.size();i++) {
					writer.println(lines.get(i).trim());
				}				
				writer.close();				
//				FileUtils.copyFile(inputQueries, expectedInFilePath);
			}
			logger.debug("Processing the input queries...");
			
			
			EdiNMT ediNMT=null;
			String mtServerSpecs="null";
			if(!this.fastMode) {
				String ediNMTPort="8081";
				int[] gpuIdx= {0};
				ediNMT=new EdiNMT(Language.en, tgtLang, Type.text, TranslatorMode.accurate, gpuIdx, networkName, ediNMTinstanceName);
				mtServerSpecs=ediNMTinstanceName+":"+ediNMTPort;
			}
			

			String cmd=null;
			cmd="sh"+" "+qpStandaloneScript.getCanonicalPath()+" "+expectedInFilePath.getName()+" "+tgtLang.toString()+" "+workDirHdlr.getSubFolderHdlr(workInDir).getCanonicalPath();
			cmd+=" "+workDirHdlr.getSubFolderHdlr(workOutDir).getCanonicalPath()+" "+workDirHdlr.getSubFolderHdlr(workLogDir).getCanonicalPath();
			cmd+=" "+instanceName+" "+qpVersion;			
			cmd+=" "+networkName+" "+mtServerSpecs;
			cmd+=" "+qExpansionFlag;
			
			logger.debug(cmd);
			
			Process process = Runtime.getRuntime().exec(cmd);
			exportLog(process);
			process.waitFor();	
			
			ArrayList<File> outFiles = IoHdlr.getInstance().getListOfFiles(workDirHdlr.getSubFolderHdlr(workOutDir));
			for(File f:outFiles) {
				String oFile=outDir.getCanonicalPath()+File.separator+f.getName();
				FileUtils.copyFile(f, new File(oFile));
			}
						
			ArrayList<File> logs = IoHdlr.getInstance().getListOfFiles(workDirHdlr.getSubFolderHdlr(workLogDir));
			for(File log:logs) {
				logger.debug(IoHdlr.getInstance().readFile(log));
			}				
			workDirHdlr.prepareWorkDir();
			
			if(ediNMT!=null) {
				ediNMT.shutdown();
			}			
			return true;
		} catch (IOException | InterruptedException e) {
			logger.fatal(e.getMessage());
			return false;
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
		if(version!=null && !version.equals(qpVersion)) {
			this.qpVersion=version;
			restart();
		}		
	}

	public String getVersion() {
		return qpVersion;
	}
	
	private void restart() {
		try {
			shutdown();
			ArrayList<String>folders=new ArrayList<String>();
			folders.add(workInDir);
			folders.add(workOutDir);
			folders.add(workLogDir);
			workDirHdlr = new WorkDirHdlr(folders);		
			if(!this.fastMode) {
				ProcessBuilder processBuilder= new ProcessBuilder("docker", "network", "create", networkName);
				Process process = processBuilder.start(); 
				exportLog(process);
				process.waitFor();
				TimeUnit.SECONDS.sleep(30);
			}			
		} catch (InterruptedException | IOException e) {
			logger.error(e.getMessage());
		}
		
		
	}
	
	public void shutdown() {
		try {	
			if(!this.fastMode) {
				ProcessBuilder processBuilder= new ProcessBuilder("docker", "network", "rm", networkName);
				Process process = processBuilder.start(); 
				exportLog(process);
				process.waitFor();
				TimeUnit.SECONDS.sleep(30);
			}			
					
			ArrayList<File> logs = IoHdlr.getInstance().getListOfFiles(workDirHdlr.getSubFolderHdlr(workLogDir));
			for(File log:logs) {
				logger.debug(IoHdlr.getInstance().readFile(log));
			}			
			workDirHdlr.cleanup();
			
		} catch (InterruptedException | IOException e) {
		}
	}

}
