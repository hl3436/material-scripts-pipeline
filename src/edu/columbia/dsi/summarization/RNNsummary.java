/**
 * 
 */
package edu.columbia.dsi.summarization;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import edu.columbia.dsi.containers.QueryRes;
import edu.columbia.dsi.utils.IoHdlr;
import edu.columbia.dsi.utils.ResourceFactory;
import edu.columbia.dsi.utils.WorkDirHdlr;

/**
 * @author badrashiny
 * Mar 2, 2018
 */
public class RNNsummary {
	private static Logger logger = Logger.getLogger(RNNsummary.class.getSimpleName());
	private boolean validityFlag=false;
	private int portNum;
	private String instanceName=UUID.randomUUID().toString(); //Generate a random docker instance name
	private WorkDirHdlr workDirHdlr;
	private static final String inputDir="inputRnnsum";
	private static final String outputDir="outputRnnsum";
	private static final String queryDir="queryRnnsum";
	
	private static final String SUMMARIZATION_TRIGGER = "7XXASDHHCESADDFSGHHSD";
	private ResourceFactory resourceFactory = new ResourceFactory();

	/**
	 * 
	 */
	public RNNsummary() {
		logger.debug("Loading rnnsum-Docker image ...");
		try {
			ArrayList<String>folders=new ArrayList<String>();
			folders.add(inputDir);
			folders.add(outputDir);
			folders.add(queryDir);
			workDirHdlr = new WorkDirHdlr(folders);
			startServer();
		} catch (IOException | InterruptedException e) {
			logger.error("Couldn't start the rnnsum server");
			validityFlag=false;
		}
		validityFlag=true;
		logger.debug("RNNSum is loaded sucessfuly..."); 
	}

	public boolean isValid() {
		return validityFlag;
	}
	
	private void startServer() throws IOException, InterruptedException{		
		String rnnsumVersion=resourceFactory.getRNNsummaryVersion();
		this.portNum=resourceFactory.getRNNsummaryPortNumber();
		
		
		File inputDirHdlr = workDirHdlr.getSubFolderHdlr(inputDir);
		File outputDirHdlr = workDirHdlr.getSubFolderHdlr(outputDir);
		File queryDirHdlr = workDirHdlr.getSubFolderHdlr(queryDir);
		/**
		 * docker run -p 9091:9091 -itd  -v $1:/app/inputs -v $2:/app/outputs -v $3:/app/query  -e "PORT=$4" --name rnnsum-v0.2-instance rnnsum:v0.2 /bin/bash
		 */	
		ProcessBuilder processBuilderRnnSum = new ProcessBuilder("docker", "run", 
															  "-p", String.valueOf(portNum)+":"+String.valueOf(portNum), 
															  "-itd", 
															  "-v", inputDirHdlr.getCanonicalPath()+":/app/inputs",
															  "-v", outputDirHdlr.getCanonicalPath()+":/app/outputs",
															  "-v", queryDirHdlr.getCanonicalPath()+":/app/query",
															  "-e", "PORT="+String.valueOf(portNum),
															  "--name", instanceName,
															  rnnsumVersion, "/bin/bash");
        Process processRnnSum = processBuilderRnnSum.start();
        processRnnSum.waitFor();
        
        /**
         * docker start instanceName
         */
        processBuilderRnnSum = new ProcessBuilder("docker", "start", instanceName);
        processRnnSum = processBuilderRnnSum.start();
        processRnnSum.waitFor();
        
        /**
         * docker attach instanceName &
         */
        processBuilderRnnSum = new ProcessBuilder("docker", "attach", instanceName, "&");
        processRnnSum = processBuilderRnnSum.start();
        processRnnSum.waitFor();
        TimeUnit.SECONDS.sleep(100);//rnnsum needs ~100 sec. to load the models into the memory	        
	}
	

	public void summarize(QueryRes docs) {
		String query=docs.getQueryWords();
		logger.debug("Summarizing files for this query: "+ query);
		workDirHdlr.prepareWorkDir();
		Socket rnnsumClient = null;
		String host=resourceFactory.getRNNsummaryServerHostName();
		DataInputStream receive = null;
		PrintStream send = null;
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
		    rnnsumClient = new Socket(host, portNum);
    			receive = new DataInputStream(rnnsumClient.getInputStream());
    			send = new PrintStream(rnnsumClient.getOutputStream());
    			send.print(SUMMARIZATION_TRIGGER);
    			
    			byte[] bytes = new byte[1000000];
    			receive.read(bytes);
    			String response = new String(bytes, "UTF-8").trim();
    			while(!response.equals(SUMMARIZATION_TRIGGER)) {
    				bytes = new byte[1000000];
        			receive.read(bytes);
        			response = new String(bytes, "UTF-8");
    			}
    			logger.debug("Summarization results are ready!");
    			send.close();
    			receive.close();
    			rnnsumClient.close();
    			
    			for(int i=0;i<summariesPaths.size();i++) {
    				String summary=IoHdlr.getInstance().readFile(summariesPaths.get(i));
    				docs.getResult(i).setSummary(summary);			
    			}
    			workDirHdlr.prepareWorkDir();
    			
		} catch (IOException e) {
	        logger.error(e);        
    			try {
    				send.close();
				receive.close();
				rnnsumClient.close();
    			} catch (IOException e1) {
				logger.error(e1);
    			}			
	    }		
	}
	
	public void shutdown() {
		try {
			/**
			 * docker stop instanceName
			 * docker remove instanceName
			 */
			ProcessBuilder processBuilderRnnSum = new ProcessBuilder("docker", "stop", instanceName);
			Process processRnnSum = processBuilderRnnSum.start();        
			processRnnSum.waitFor();
			TimeUnit.SECONDS.sleep(10);
			/**
	         * docker remove instanceName
	         */
	        processBuilderRnnSum = new ProcessBuilder("docker", "rm", instanceName);
	        processRnnSum = processBuilderRnnSum.start();
	        processRnnSum.waitFor();	
	        
	        workDirHdlr.cleanup();
	        logger.debug("rnnsum image has been closed successfully...");
		} catch (InterruptedException | IOException e) {
			logger.error(e.getMessage());
		}
	}

}
