/**
 * 
 */
package edu.columbia.dsi;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.UUID;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import edu.columbia.dsi.SCRIPTS.Language;
import edu.columbia.dsi.containers.UmdCLIRConfig;
import edu.columbia.dsi.containers.UmdMatcherConfig;
import edu.columbia.dsi.ir.UmdCLIR;
import edu.columbia.dsi.ir.UmdCLIR.UmdCLIRCombFormat;
import edu.columbia.dsi.ir.UmdQueryProcessor;
import edu.columbia.dsi.summarization.CUsummarizer;
import edu.columbia.dsi.utils.IoHdlr;
import edu.columbia.dsi.utils.WorkDirHdlr;

/**
 * @author badrashiny
 * Feb 6, 2019
 */
public class DemoMode {

	private static Logger logger = Logger.getLogger(CLIRExperimenter.class.getSimpleName());
	private UmdCLIR umdCLIR;
	

	
	private String matcherVersion=null;
	private String evidenceCombVersion=null;
	

	private WorkDirHdlr workDirHdlr;
	private String workDir="Demo-WorkDir"+UUID.randomUUID().toString()+ (new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()));
	private String qpTmpDir="qp-tmp-dir"+UUID.randomUUID().toString()+ (new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()));
	private String inputQpTmpDir="input-qp-tmp-dir"+UUID.randomUUID().toString()+ (new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()));
	private String ecTmpDir="ec-tmp-dir"+UUID.randomUUID().toString()+ (new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()));
	private String summarizerTmpDir="summarizer-tmp-dir"+UUID.randomUUID().toString()+ (new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()));
	private File summarizerintermediateOutDir=null;
	private String othersTmpDir="others-tmp-dir"+UUID.randomUUID().toString()+ (new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()));
	private CUsummarizer summarizer=null;
	private File expConfigFilePath=null;
	private File expConfigFileOut=null;
	private UmdCLIRConfig expConfig=null;
	private File outDir=null;
	private  boolean stream=false;// if true, we set the summarizerintermediateOutDir to be the output directory. Otherwise, we set it to a temp directory in the work directory  
	
	/**
	 * 
	 */
	public DemoMode(JSONObject expConfigJson, File outRootDir, String nistDataDir, boolean stream) {
		logger.debug("Initializing the CLIR-Experimenter...");
		umdCLIR=new UmdCLIR(false);
		try {
			this.stream=stream;
			ArrayList<String>folders=new ArrayList<String>();
			folders.add(workDir);
			folders.add(qpTmpDir);
			folders.add(inputQpTmpDir);
			folders.add(ecTmpDir);
			folders.add(othersTmpDir);
			folders.add(summarizerTmpDir);
			workDirHdlr = new WorkDirHdlr(folders);
			
			String expPathTmp=workDirHdlr.getSubFolderHdlr(this.workDir).getCanonicalPath()+File.separator+"exp-config.json";		
			BufferedWriter writer = new BufferedWriter(new FileWriter(expPathTmp));
			writer.write(expConfigJson.toString(4));
			writer.close();		
			this.expConfigFilePath= new File(expPathTmp);
			this.expConfig=parseconfig(expConfigFilePath);
			if(expConfig!=null) {
				if(matcherVersion!=null) {
					umdCLIR.setUmdQMVersion(matcherVersion);
				}
				if(evidenceCombVersion!=null) {
					umdCLIR.setUmdEviCombVersion(evidenceCombVersion);
				}	
				
				this.outDir=new File(outRootDir.getCanonicalPath()+File.separator+expConfig.getConfigurationName());
				if(!this.outDir.exists()) {
					this.outDir.mkdir();
				}
				this.expConfigFileOut= new File(this.outDir.getCanonicalPath()+File.separator+expConfigFilePath.getName());
				initializeCLIROutDir();
				
				
				if(this.stream) {				
					this.summarizerintermediateOutDir=new File(this.outDir.getCanonicalPath()+File.separator+"CU-summaryDir");
					if(!this.summarizerintermediateOutDir.exists()) {
						this.summarizerintermediateOutDir.mkdir();
					}else {
						IoHdlr.getInstance().emptyDir(this.summarizerintermediateOutDir);
					}				
				}else {
					this.summarizerintermediateOutDir=workDirHdlr.getSubFolderHdlr(summarizerTmpDir);
				}
				
				
				summarizer= new CUsummarizer(nistDataDir, workDirHdlr.getSubFolderHdlr(this.ecTmpDir), workDirHdlr.getSubFolderHdlr(this.qpTmpDir),  this.expConfigFilePath, this.summarizerintermediateOutDir, true);
//				summarizer =new CUsummarizer(summarizerPortNumber, this.expConfigFilePath, workDirHdlr.getSubFolderHdlr(this.qpTmpDir), workDirHdlr.getSubFolderHdlr(this.ecTmpDir), workDirHdlr.getSubFolderHdlr(this.summarizerTmpDir));
				summarizer.startServer();
				logger.debug("The Demo-mode is ready.");
			}			
		} catch (IOException e) {
			logger.fatal(e.getMessage());
		}	
	}
	public void run(String query) {
		try {
			IoHdlr.getInstance().emptyDir(workDirHdlr.getSubFolderHdlr(othersTmpDir));
			UmdQueryProcessor qp=new UmdQueryProcessor(true);//i.e. fast mode
			File queryAnalysis=qp.processQuery(query, expConfig.getTgtLang(), workDirHdlr.getSubFolderHdlr(othersTmpDir));
			if(queryAnalysis!=null && queryAnalysis.exists()) {
				Hashtable<String, HashSet<File>> qFilesGroup = IoHdlr.getInstance().getListOfFilesGrouped(workDirHdlr.getSubFolderHdlr(othersTmpDir));
				for(Entry<String, HashSet<File>> qNameSet : qFilesGroup.entrySet()) {// this set will always have just one entry; which is the input query. So the for loop will run just once
					run(qNameSet);
				}				
			}		
			qp.shutdown();
			IoHdlr.getInstance().emptyDir(workDirHdlr.getSubFolderHdlr(othersTmpDir));
		} catch (IOException e) {
			logger.fatal(e.getMessage());
		}
		
	}
	public void run(Entry<String, HashSet<File>> processedQuery) {	
		try {
			initializeSummarizerWorkDir();
			IoHdlr.getInstance().emptyDir(this.summarizerintermediateOutDir);
			initializeCLIROutDir();	
			for(File f:processedQuery.getValue()) {				             
				File tmpPath=new File(workDirHdlr.getSubFolderHdlr(inputQpTmpDir).getCanonicalPath()+File.separator+f.getName());
				FileUtils.copyFile(f, tmpPath);
			}
			umdCLIR.searchQuery(expConfigFileOut, expConfig.getMathcerConfig(), expConfig.getQueriesListsPaths(), expConfig.getTgtLang(), expConfig.getEviCombCutoff(), expConfig.getEviCombFormat(), outDir);
			
			ArrayList<File> ecFiles=IoHdlr.getInstance().getListOfFiles(umdCLIR.getECOutDir());
			for(File f:ecFiles) {
				String oFile=workDirHdlr.getSubFolderHdlr(ecTmpDir).getCanonicalPath()+File.separator+f.getName();
				FileUtils.copyFile(f, new File(oFile));
			}
			
			ArrayList<File> qFiles=IoHdlr.getInstance().getListOfFiles(umdCLIR.getQPOutDir());
			for(File f:qFiles) {
				String oFile=workDirHdlr.getSubFolderHdlr(qpTmpDir).getCanonicalPath()+File.separator+f.getName();
				FileUtils.copyFile(f, new File(oFile));
			}
			ArrayList<String> queries=IoHdlr.getInstance().getListOfFilesNames(workDirHdlr.getSubFolderHdlr(qpTmpDir));		
			
			summarizer.summarizeQuerySearchResults(queries);

			if(!this.stream) {
				File summarizerOutDir=new File(this.outDir.getCanonicalPath()+File.separator+"CU-summaryDir");
				if(!summarizerOutDir.exists()){
					summarizerOutDir.mkdir();
				}
					
				ArrayList<File> summDirs=IoHdlr.getInstance().getListOfDirs(this.summarizerintermediateOutDir);
				for(File d:summDirs) {
					String oDir=summarizerOutDir.getCanonicalPath()+File.separator+d.getName();
					FileUtils.copyDirectory(d, new File(oDir));				
				}
				IoHdlr.getInstance().emptyDir(this.summarizerintermediateOutDir);
			}			
			initializeSummarizerWorkDir();			
			
		} catch (IOException e) {
			logger.fatal(e.getMessage());
		}	
		
	}
	
	private void initializeCLIROutDir() throws IOException {
		IoHdlr.getInstance().emptyDir(outDir);
		FileUtils.copyFile(expConfigFilePath, expConfigFileOut);
	}
	
	private void initializeSummarizerWorkDir() throws IOException {
		IoHdlr.getInstance().emptyDir(workDirHdlr.getSubFolderHdlr(inputQpTmpDir));
		IoHdlr.getInstance().emptyDir(workDirHdlr.getSubFolderHdlr(qpTmpDir));
		IoHdlr.getInstance().emptyDir(workDirHdlr.getSubFolderHdlr(ecTmpDir));
		//IoHdlr.getInstance().emptyDir(workDirHdlr.getSubFolderHdlr(summarizerTmpDir));		
	}
	
	private UmdCLIRConfig parseconfig(File expConfigFilePath) {
		String jsonStr=IoHdlr.getInstance().readFile(expConfigFilePath);
		JSONObject expConfig = new JSONObject(jsonStr);
		
		if(expConfig.has("query_processor") && expConfig.has("evidence_combination") && expConfig.has("matcher")) {								
			if(expConfig.getJSONObject("evidence_combination").has("version")) {
				evidenceCombVersion=expConfig.getJSONObject("evidence_combination").getString("version").trim();
			}else {
				evidenceCombVersion=null;
			}
			
			if(expConfig.getJSONObject("matcher").has("version")) {
				matcherVersion=expConfig.getJSONObject("matcher").getString("version").trim();
			}else {
				matcherVersion=null;
			}
			
			Language tgtLang=Language.valueOf(expConfig.getJSONObject("query_processor").getString("target_language").trim());
			

			
			
			
			ArrayList<File>queriesListsPaths=new ArrayList<File>();			
			queriesListsPaths.add(workDirHdlr.getSubFolderHdlr(inputQpTmpDir));

			
			Hashtable<String,String>collectionNames=new Hashtable<String,String>();
			Hashtable<String,String>indexNames=new Hashtable<String,String>();
					
			int eviComCutoff=expConfig.getJSONObject("evidence_combination").getInt("cutoff");
			
			JSONArray matcherExps = expConfig.getJSONObject("matcher").getJSONArray("configurations");
			ArrayList<UmdMatcherConfig> matcherConfigs=new ArrayList<UmdMatcherConfig>();
			for (int i = 0; i < matcherExps.length(); i++){
				JSONArray indexes = matcherExps.getJSONObject(i).getJSONArray("indexes");
				ArrayList<String>indexesTmp=new ArrayList<String>();
				for (int j = 0; j < indexes.length(); j++){
					indexesTmp.add(indexes.getString(j).trim());
					String collectionName=indexes.getString(j).trim().replaceAll(".*/(EVAL(\\d+)?)/.*", "$1").replaceAll(".*/(DEV(\\d+)?)/.*", "$1").replaceAll(".*/(ANALYSIS(\\d+)?)/.*", "$1");
					if(!collectionNames.containsKey(collectionName)) {
						collectionNames.put(collectionName, "1");
					}
									
					String indexName=new File(new File(indexes.getString(j).trim()).getParent()).getName();
					if(!indexNames.containsKey(indexName)) {
						indexNames.put(indexName, "1");
					}				
				}
				String type = matcherExps.getJSONObject(i).getString("type").trim();
				int cutoff=matcherExps.getJSONObject(i).getInt("cutoff");
				UmdCLIRCombFormat format = UmdCLIRCombFormat.valueOf(matcherExps.getJSONObject(i).getString("format").trim());
				String configName=matcherExps.getJSONObject(i).getString("config_name");
				
				matcherConfigs.add(new UmdMatcherConfig(indexesTmp, type, cutoff, format, configName, expConfigFilePath));			
			}

			String expName="demo-out";
			return new UmdCLIRConfig(expName, matcherConfigs, queriesListsPaths, tgtLang, eviComCutoff, UmdCLIRCombFormat.tsv);
		}else {
			return null;
		}
	}
	public void shutdown() {
		workDirHdlr.cleanup();
		umdCLIR.shutdown();
		summarizer.shutdown();
		
	}

}
