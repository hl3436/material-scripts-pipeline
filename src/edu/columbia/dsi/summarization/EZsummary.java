/**
 * 
 */
package edu.columbia.dsi.summarization;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.UUID;

import org.apache.log4j.Logger;

import edu.columbia.dsi.containers.QueryRes;
import edu.columbia.dsi.utils.IoHdlr;
import edu.columbia.dsi.utils.ResourceFactory;
import edu.columbia.dsi.utils.WorkDirHdlr;

/**
 * @author badrashiny
 * Mar 2, 2018
 */
public class EZsummary {
	private static Logger logger = Logger.getLogger(EZsummary.class.getSimpleName());
	private boolean validityFlag=false;
	private String instanceName=UUID.randomUUID().toString(); //Generate a random docker instance name
	private WorkDirHdlr workDirHdlr;
	private static final String inputDir="inputEZsum";
	private static final String outputDir="outputEZsum";
	private static final String queryDir="queryEZsum";
	
	private ResourceFactory resourceFactory = new ResourceFactory();

	/**
	 * 
	 */
	public EZsummary() {
		logger.debug("Initializing EZsummary ...");
		ArrayList<String>folders=new ArrayList<String>();
		folders.add(inputDir);
		folders.add(outputDir);
		folders.add(queryDir);
		workDirHdlr = new WorkDirHdlr(folders);
		validityFlag=true;
		logger.debug("EZsummary is initialized sucessfuly..."); 
	}
	
	public boolean isValid() {
		return validityFlag;
	}
	
	public void summarize(QueryRes docs) {
		String query=docs.getQueryWords();
		logger.debug("Summarizing files for this query: "+ query);
		workDirHdlr.prepareWorkDir();
		
		
		String ezsumVersion = resourceFactory.getEZsummaryVersion();
		
		File inputDirHdlr = workDirHdlr.getSubFolderHdlr(inputDir);
		File outputDirHdlr = workDirHdlr.getSubFolderHdlr(outputDir);
		File queryDirHdlr = workDirHdlr.getSubFolderHdlr(queryDir);
		try {
			PrintWriter writer = new PrintWriter(workDirHdlr.getSubFolderHdlr(queryDir).getCanonicalPath()+File.separator+"queries.txt");
			writer.println(query);
		    writer.close();
		    ArrayList<File> summariesPaths =new ArrayList<File>();
		    for(int i=0; i<docs.size();i++) {
		    		File from = docs.getResult(i).getTranslationAsFile();
		    		File to = new File(workDirHdlr.getSubFolderHdlr(inputDir).getCanonicalPath()+File.separator+from.getName());
		    		File summaryPath = new File(workDirHdlr.getSubFolderHdlr(outputDir).getCanonicalPath()+File.separator+from.getName());
		    		summariesPaths.add(summaryPath);
		    		Files.copy(from.toPath(), to.toPath(), REPLACE_EXISTING);		    	
		    }
		    
		    /**
			 * docker run -v $1:/app/input -v $2:/app/output  -v $3:/app/query --name ezsummary-instance ezsummary
			 */	
			ProcessBuilder processBuilderEzSum = new ProcessBuilder("docker", "run",  
																  "-v", inputDirHdlr.getCanonicalPath()+":/app/input",
																  "-v", outputDirHdlr.getCanonicalPath()+":/app/output",
																  "-v", queryDirHdlr.getCanonicalPath()+":/app/query",
																  "--name", instanceName,
																  ezsumVersion, "/bin/bash");
	        Process processEzSum = processBuilderEzSum.start();
	        processEzSum.waitFor();
		    
	        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(processEzSum.getInputStream()));
	        String retStr=null;
	        while ((retStr = bufferedReader.readLine()) != null) {
	        		logger.debug(retStr);
	        }
	        bufferedReader.close();
		      
    			logger.debug("Summarization results are ready!");
    			
    			
    			for(int i=0;i<summariesPaths.size();i++) {
    				String summary=IoHdlr.getInstance().readFile(summariesPaths.get(i));
    				docs.getResult(i).setSummary(summary);			
    			}
    			workDirHdlr.prepareWorkDir();
    			
		} catch (IOException | InterruptedException e) {
	        logger.error(e);	
	    }		
	}
	
	public void shutdown() {
		try {
			/**
	         * docker remove instanceName
	         */
			ProcessBuilder processBuilderEzSum = new ProcessBuilder("docker", "rm", instanceName);
			Process processEzSum = processBuilderEzSum.start();
	        processEzSum.waitFor();	
			workDirHdlr.cleanup();
			logger.debug("EZsummary has been closed successfully...");			
		} catch (InterruptedException | IOException e) {
			logger.error(e.getMessage());
		}		
	}

}
