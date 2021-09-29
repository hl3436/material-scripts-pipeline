/**
 * 
 */
package edu.columbia.dsi;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;

import org.apache.log4j.Logger;

import edu.columbia.dsi.containers.Corpora;
import edu.columbia.dsi.containers.DataStoreMgr;
import edu.columbia.dsi.utils.IoHdlr;
import edu.columbia.dsi.utils.ResourceFactory;
import edu.columbia.dsi.utils.WorkDirHdlr;

/**
 * @author badrashiny
 * Mar 9, 2018
 */
public class Indexer {
	private static Logger logger = Logger.getLogger(Indexer.class.getSimpleName());
	private ResourceFactory resourceFactory = new ResourceFactory();
	private String umdIndexerVersion;
	private File umdIndexerScript;
	//private File umdIndexerParameters;
	private ArrayList<File>umdIndriParametersFiles;
	/**
	 * 
	 */
	public Indexer() {
		logger.debug("Initializing the UMD-Indexer...");
		this.umdIndexerVersion=resourceFactory.getUmdIndexerVersion();
		this.umdIndexerScript=resourceFactory.getUmdIndexerScript();
		File umdIndexerParameters=resourceFactory.getUmdIndexerParametersPath();
		this.umdIndriParametersFiles = IoHdlr.getInstance().getListOfFiles(umdIndexerParameters);		
	}
	
	public Indexer(ArrayList<File>indriParamFiles) {
		logger.debug("Initializing the UMD-Indexer...");
		this.umdIndexerVersion=resourceFactory.getUmdIndexerVersion();
		this.umdIndexerScript=resourceFactory.getUmdIndexerScript();
		this.umdIndriParametersFiles=new ArrayList<File>();
		this.umdIndriParametersFiles.addAll(indriParamFiles);
		
	}
	public String index(Corpora corpora) {
		DataStoreMgr dataStoreMgr = new DataStoreMgr(corpora);
		File dataStoreStructure = dataStoreMgr.getDataStoreHdlr();
		File corporaRootLocation = corpora.getAbsolutePathLocation();

		String indexFile = null;
		for(int i=0; i<umdIndriParametersFiles.size(); i++) {
			indexFile = runIndexer(corporaRootLocation, dataStoreStructure, umdIndriParametersFiles.get(i)); 
		}
		return indexFile;
//		dataStoreMgr.cleanUp();
	}

	private String runIndexer(File corporaRootLocation, File dataStoreStructure, File indriParameters) {
		try {
			logger.debug("Indexing input corpora using this indri parameters file: "+ indriParameters.getCanonicalPath());
			
//			//version 1.0			
//			String cmd="sh"+" "+umdIndexerScript.getCanonicalPath()+" "+dataStoreStructure.getCanonicalPath()+" "+indriParameters.getCanonicalPath()+" "+corporaRootLocation.getCanonicalPath()+" "+umdIndexerVersion;
			
			//version 1.1		
			ArrayList<String>folders=new ArrayList<String>();
			String wrkDirName="indexerWorkDir"+UUID.randomUUID().toString()+ (new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()));
			// String wrkDirName="indexerWorkDir"+UUID.randomUUID().toString()+ "20210713_101010";

			logger.debug("workDir name: " + wrkDirName);

			folders.add(wrkDirName);				
			WorkDirHdlr workDirHdlr = new WorkDirHdlr(folders);
			File logDirPath=workDirHdlr.getSubFolderHdlr(wrkDirName);
			File outLogFile=new File(logDirPath.getCanonicalPath()+File.separator+"output.log");	
			File outLogFile2=new File(logDirPath.getCanonicalPath()+File.separator+"data_store_structire_output.log");	
			
			String cmd="sh"+" "+umdIndexerScript.getCanonicalPath()+" "+dataStoreStructure.getCanonicalPath()+" "+" "+indriParameters.getName()+" "+indriParameters.getParent()+" "+corporaRootLocation.getCanonicalPath()+" "+logDirPath.getCanonicalPath()+" "+umdIndexerVersion;
			logger.debug(cmd);
			Process process = Runtime.getRuntime().exec(cmd);
			BufferedReader reader =	new BufferedReader(new InputStreamReader(process.getInputStream()));
			while (reader.readLine() != null) {}
			process.waitFor();	
			
			if(outLogFile.exists()) {
				logger.debug(IoHdlr.getInstance().readFile(outLogFile));
			}
			
			String indexFile = null;
			if(outLogFile2.exists()) {
				logger.debug(IoHdlr.getInstance().readFile(outLogFile2));
				for (String line : IoHdlr.getInstance().readFileLines(outLogFile2)) {
					if (line.contains("outputfile: ")) {
						int ia = line.lastIndexOf("/data_store_structure.");
						int ib = line.indexOf(".txt", ia) + 4;
						indexFile = line.substring(ia, ib);
					}
				}
			}
			workDirHdlr.cleanup();
			return indexFile;
		}catch (IOException | InterruptedException e) {
			logger.error(e.getMessage());			
			return null;
		}
		
	}
}
