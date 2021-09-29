/**
 * 
 */
package edu.columbia.dsi.ir;

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
import edu.columbia.dsi.utils.ResourceFactory;

/**
 * @author badrashiny
 * May 15, 2018
 */
public class UmdDomainCombination {
	private static Logger logger = Logger.getLogger(UmdDomainCombination.class.getSimpleName());
	private ResourceFactory resourceFactory = new ResourceFactory();
	private String dcVersion;
	private File dcScript;

	/**
	 * 
	 */
	public UmdDomainCombination() {
		logger.debug("Initializing the UMD-Domain-Combiner...");
		this.dcVersion=resourceFactory.getUmdDomainCombinationVersion();		
		this.dcScript=resourceFactory.getUmdDomainCombinationScript();
		logger.debug("UMD-Domain-Combiner is ready.");
	}
	
	/**
	 * Override the version in the config file
	 * @param version
	 */
	public void setVersion(String version) {
		if(version!=null && !version.equals(dcVersion)) {
			this.dcVersion=version;
		}		
	}
	
	/**
	 * 
	 * @param inputRootDir: (EX:  /storage/data/NIST-data)
	 * @param domainModeledDirecotriesList: all paths required to be merged under inputRootDir to the domain-modeling-output.csv
	 * @param outputDir
	 * @param lang
	 */
	public void domainCombine(File inputRootDir, ArrayList<String> domainModeledDirecotriesList, File outputDir, Language lang) {
		try {
			String  domainModeledDirecotries=domainModeledDirecotriesList.get(0)+File.separator+"domain-modeling-output.csv";
			for(int i=1; i<domainModeledDirecotriesList.size();i++) {
				domainModeledDirecotries+="+"+domainModeledDirecotriesList.get(i)+File.separator+"domain-modeling-output.csv";
			}
			logger.debug("combining domains of: "+inputRootDir.getCanonicalPath()+File.separator+domainModeledDirecotries);
			
			String dataLang=null;
//			if(lang==Language.tl) {
//				dataLang="tagalog";
//			}else if(lang==Language.sw) {
//				dataLang="swahili";
//			}
			dataLang=lang.toString();
			
			String cmd="sh"+" "+dcScript.getCanonicalPath()+" "+inputRootDir.getCanonicalPath()+" "+domainModeledDirecotries;
			cmd+=" "+outputDir.getCanonicalPath()+" "+dataLang+" "+dcVersion;			
			logger.debug(cmd);
			
			Process process = Runtime.getRuntime().exec(cmd);
			exportLog(process);
			process.waitFor();				
		} catch (IOException | InterruptedException  e) {
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
	
	
	public String getVersion() {
		return dcVersion;
	}

}
