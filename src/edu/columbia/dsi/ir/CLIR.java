/**
 * 
 */
package edu.columbia.dsi.ir;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import edu.columbia.dsi.SCRIPTS.Language;
import edu.columbia.dsi.containers.DocInfo;
import edu.columbia.dsi.containers.DocInfo.Type;
import edu.columbia.dsi.containers.QueryRes;
import edu.columbia.dsi.utils.IoHdlr;
import edu.columbia.dsi.utils.ResourceFactory;


/**
 * @author badrashiny
 * Feb 28, 2018
 */
public class CLIR {
	private static Logger logger = Logger.getLogger(CLIR.class.getSimpleName());
	private boolean validityFlag=false;
	private int portNum;
	private String indexDB;
	private String instanceName=UUID.randomUUID().toString(); //Generate a random docker instance name
	private ResourceFactory resourceFactory = new ResourceFactory();
	/**
	 * 
	 */
	public CLIR(Language lang) {
		logger.debug("Loading CLIR-Docker image ...");
		if(lang!=Language.tl && lang!=Language.sw) {
			logger.error("Unsupported langauage: "+lang.toString());
			validityFlag=false;
		}else {
			try {
				startServer(lang);
			} catch (IOException | InterruptedException e) {
				logger.error("Couldn't start the CLIR server for this language: "+lang.toString());
				validityFlag=false;
			}
		}
		validityFlag=true;
		logger.debug("CLIR is loaded sucessfuly..."); 
	}
	public boolean isValid() {
		return validityFlag;
	}
	
	private void startServer(Language lang) throws IOException, InterruptedException {		
		String clirVersion=resourceFactory.getCLIRVersion();
		File tgtLangPath=null;
		if(lang==Language.tl) {
			tgtLangPath=resourceFactory.getCLIRTagalogResourcePath();
			this.portNum=resourceFactory.getCLIRTagalogPortNumber();
			this.indexDB=resourceFactory.getCLIRTagalogIndexDB();			
		}else if(lang==Language.sw) {
			tgtLangPath=resourceFactory.getCLIRSwahiliResourcePath();
			this.portNum=resourceFactory.getCLIRSwahiliPortNumber();
			this.indexDB=resourceFactory.getCLIRSwahiliIndexDB();	
		}
		
		
		
		/**
		 * docker run -p 7070:7070 -itd -v tgtLangPath:/media/data --name instanceName indri-network-entry:v2.0 /bin/bash
		 */	
		ProcessBuilder processBuilderCLIR = new ProcessBuilder("docker", "run", 
															  "-p", String.valueOf(portNum)+":"+String.valueOf(portNum), 
															  "-itd", 
															  "-v", tgtLangPath.getCanonicalPath()+":/media/data", 
															  "--name", instanceName,
															  "-e", "PORT="+String.valueOf(portNum),
															  clirVersion, "/bin/bash");
        Process processCLIR = processBuilderCLIR.start();
        BufferedReader reader =	new BufferedReader(new InputStreamReader(processCLIR.getInputStream()));
		while ((reader.readLine()) != null) {}
        processCLIR.waitFor();
        
        /**
         * docker start instanceName
         */
        processBuilderCLIR = new ProcessBuilder("docker", "start", instanceName);
        processCLIR = processBuilderCLIR.start();
        BufferedReader reader2 =	new BufferedReader(new InputStreamReader(processCLIR.getInputStream()));
		while ((reader2.readLine()) != null) {}
        processCLIR.waitFor();
        
        /**
         * docker attach instanceName &
         */
        processBuilderCLIR = new ProcessBuilder("docker", "attach", instanceName, "&");
        processCLIR = processBuilderCLIR.start();
        BufferedReader reader3 =	new BufferedReader(new InputStreamReader(processCLIR.getInputStream()));
		while ((reader3.readLine()) != null) {}
//        processCLIR.waitFor();
        
        TimeUnit.SECONDS.sleep(10);//CLIR needs ~10 sec. to load the models into the memory  
	}
	@SuppressWarnings("deprecation")
	public QueryRes runQuery(String query, int maxSize) {
		String query_id="-1";
		String format="json";//{json, tsv}
		Socket clirClient = null;
		String host=resourceFactory.getCLIRServerHostName();
		DataInputStream receive = null;
		PrintStream send = null;
	    try {
	    		clirClient = new Socket(host, portNum);
	    		receive = new DataInputStream(clirClient.getInputStream());
	    		send = new PrintStream(clirClient.getOutputStream());
	    		String request = "query=" + query + "~id=" + query_id + "~format=" + format + "~properties=" + indexDB;
	    		send.print(request);
	    		logger.debug("Request sent to CLIR server: "+request);
	    		String responseLine=receive.readLine();
	    		logger.debug(responseLine);

	    		send.close();
	    		receive.close();
	    		clirClient.close();
	    		return getOutObj(responseLine,maxSize);
	    }catch (IOException e) {
	        System.out.println(e);
	        logger.error(e);
	        
    			try {
    				send.close();
				receive.close();
				clirClient.close();
    			} catch (IOException e1) {
    				System.out.println(e1);
				logger.error(e1);
    			}			
	    }
		return null;		
	}
	private QueryRes getOutObj(String jsonStr, int maxSize) {
		JSONObject obj = new JSONObject(jsonStr);
		String queryWords = obj.getString("query words").trim();
		String query = obj.getString("query").trim();
		String corpora = obj.getString("collection").trim();
		QueryRes clirOutObj= new QueryRes(corpora, query, queryWords);
		
		JSONArray arr = obj.getJSONArray("results");
		for (int i = 0; i < arr.length(); i++){
			String docID = arr.getJSONObject(i).getString("docno").trim();
			double score = arr.getJSONObject(i).getDouble("score");
			int  rank = Integer.valueOf(arr.getJSONObject(i).getString("rank").trim()).intValue();
			if(clirOutObj.size()<maxSize) {
				clirOutObj.addDocument(docID, rank, score);
			}
		}	
		return clirOutObj;
	}
	public void exportSearch(String query, int maxSize, File outFile) {	
		logger.debug("Processing Query: "+query);
		try {
			if(!outFile.exists()) {
				QueryRes res =runQuery(query.toLowerCase(), maxSize);
				PrintWriter writer = new PrintWriter(outFile.getCanonicalPath());
				writer.println("Source_Text_File"+"\t"+"Translated_file"+"\t"+"Type"+"Relevance_score"+"\t"+"Rank");
				for(int i=0;i<res.size();i++) {
					DocInfo doc =res.getResult(i);
					if(doc.getType()==Type.audio) {
						writer.println(doc.getTranscriptionAsFile()+"\t"+doc.getTranslationAsFile()+"\t"+doc.getType().toString()+"\t"+doc.getScore()+"\t"+doc.getRank());
					}else if(doc.getType()==Type.text) {
						writer.println(doc.getSrcAsFile()+"\t"+doc.getTranslationAsFile()+"\t"+doc.getType().toString()+"\t"+doc.getScore()+"\t"+doc.getRank());
					}
				} 
				writer.close();
			}			
		} catch (IOException e) {
			logger.fatal(e.getMessage());
		}		
	}
	public void batchExportSearch(File querListPath, int maxSize, String outDir) {
		logger.debug("Processing Query List: "+ querListPath.getAbsolutePath());
		ArrayList<String>queries=IoHdlr.getInstance().readFileLines(querListPath);
		for(int i=1;i<queries.size();i++) {//skip the header line
			String [] columns= queries.get(i).split("\t");//columns[0]: query_id
														  //columns[1]: query_string
														  //columns[2]: domain_id
			if(columns.length!=3) {
				logger.error("Wrong formatted line: "+ queries.get(i) +" in the input query list: "+querListPath.getAbsolutePath());
				continue;
			}
			String outFile=outDir+"/"+columns[0];
			exportSearch(columns[1],  maxSize, new File(outFile));					
		}		
	}
	
	/**
	 * takes a tab seprated file where the first row is the header (query_id	query_string	domain_id)
	 * @param querListPath
	 */
	public Hashtable<String, QueryRes> batchRun(File querListPath, int maxSize) {
		Hashtable<String, QueryRes>patchOut=new Hashtable<String, QueryRes>();
		ArrayList<String>queries=IoHdlr.getInstance().readFileLines(querListPath);
		for(int i=1;i<queries.size();i++) {//skip the header line
			String [] columns= queries.get(i).split("\t");//columns[0]: query_id
														  //columns[1]: query_string
														  //columns[2]: domain_id
			if(columns.length!=3) {
				logger.error("Wrong formatted line: "+ queries.get(i) +" in the input query list: "+querListPath.getAbsolutePath());
				continue;
			}
			QueryRes res =runQuery(columns[1], maxSize);
			patchOut.put(columns[0], res);			
		}
		return patchOut;
	}
 	public void shutdown() {
		try {
			/**
			 * docker stop instanceName
			 * docker remove instanceName
			 */
			ProcessBuilder processBuilderCLIR = new ProcessBuilder("docker", "stop", instanceName);
			Process processCLIR = processBuilderCLIR.start();        
			processCLIR.waitFor();
			TimeUnit.SECONDS.sleep(10);
			/**
	         * docker remove instanceName
	         */
	        processBuilderCLIR = new ProcessBuilder("docker", "rm", instanceName);
	        processCLIR = processBuilderCLIR.start();
	        processCLIR.waitFor();	
	        logger.debug("CLIR image has been closed successfully...");
		} catch (InterruptedException | IOException e) {
		}
	}

}
