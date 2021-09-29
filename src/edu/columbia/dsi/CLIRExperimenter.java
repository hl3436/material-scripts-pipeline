/**
 * 
 */
package edu.columbia.dsi;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
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
import edu.columbia.dsi.ir.UmdDomainCombination;
import edu.columbia.dsi.summarization.CUsummarizer;
import edu.columbia.dsi.utils.CLIRPackager;
import edu.columbia.dsi.utils.Evaluator;
import edu.columbia.dsi.utils.IoHdlr;
import edu.columbia.dsi.utils.LangIdPackager;
import edu.columbia.dsi.utils.ResourceFactory;

/**
 * @author badrashiny
 * May 9, 2018
 */
public class CLIRExperimenter {
	private static Logger logger = Logger.getLogger(CLIRExperimenter.class.getSimpleName());
	private ResourceFactory resourceFactory = new ResourceFactory();
	private UmdCLIR umdCLIR;
	private Evaluator evaluator;
	private CLIRPackager clirPackager;
	private UmdDomainCombination umdDomainCombination;
	private LangIdPackager langIdPackager;
	private ArrayList<String>domainIdCollections=null;
	private ArrayList<File>langIdCollections=null;
	private Language langIdTgtLang=null;
	
	
	
//	public enum Task {CLIR, DomainID, E2E;}
//	private Task clirTask= Task.CLIR;
//	private Task domainIdTask= Task.DomainID;
	
//	public enum SubmissionType {primary, contrastive;}
//	public enum SubmissionSet {QUERY1, QUERY2, QUERY1QUERY2, QUERY2QUERY3, DX, DXY, DXYZ, D12, D1234;}
//	public enum SubmissionDataset {DEV, ANALYSIS1, DEVANALYSIS1, EVAL1EVAL2, EVAL1EVAL2EVAL3, ANALYSIS1ANALYSIS2;}
	//EVAL1, EVAL2, EVAL3, ANALYSIS2, ANALYSIS3
	
//	private File renameSubmissionScript;
//	private String team="SCRIPTS";
//	private SubmissionType subType;
//	private SubmissionSet clirQuerySet;
//	private String clirSubLang;
	
	
	private String domainIdSet=null;
	private Language domainIdSubLang;
//	private SubmissionDataset clirSubDset;
	private String domainIdSubDset;
	private String relevJudgmentsPath=null;
	private boolean evaluationFlag=false;//i.e. don't evaluate by default
	
	private String domIdVersion=null;
	private String queryProcessorVersion=null;
	private String evaluatorVersion=null;
	private String matcherVersion=null;
	private String evidenceCombVersion=null;
	private String rerankerVersion=null;
	
	private boolean runSummarizer=false;
	private String summarizerVersion=null;
	
	private boolean queryProcessorStatus=false;//i.e don't call the query processor. This will change based on the input config file 
	private File cacheFolder=null;
	private boolean overrideFlag=false;
	/**
	 * 
	 */
	public CLIRExperimenter() {
		logger.debug("Initializing the CLIR-Experimenter...");
//		umdCLIR=new UmdCLIR();
		clirPackager=new CLIRPackager();
		evaluator=new Evaluator();
		umdDomainCombination=new UmdDomainCombination();
		langIdPackager= new LangIdPackager();
//		renameSubmissionScript=resourceFactory.getCreateSubmissionNameScript();
		logger.debug("The CLIR-Experimenter is ready.");
	}
	
	public void setCachingPath(File cacheFolder, boolean overrideFlag) {
		this.cacheFolder=cacheFolder;
		this.overrideFlag=overrideFlag;
	}

	private void copyConfigFile(File from, File to, String generatedIndexPath) throws IOException {

		if (generatedIndexPath == null) {
			FileUtils.copyFile(from, to);
			return;
		}

		ArrayList<String> lines = IoHdlr.getInstance().readFileLines(from, false);
		ArrayList<String> lines2 = new ArrayList<>();

		for (String line : lines) {
			if (line.contains("/data_store_structure.")) {
				int ia = line.lastIndexOf("/data_store_structure.");
				int ib = line.indexOf(".txt", ia) + 4;
				line = line.substring(0, ia) + generatedIndexPath + line.substring(ib);
			}
			lines2.add(line);
		}

		IoHdlr.getInstance().exportFile(lines2, to, false);

	}
	
	public void runExperiment(File expConfigFilePath, File outRootDir, String generatedIndexPath) {		
		try {
			UmdCLIRConfig expConfig=parseconfig(expConfigFilePath);
			umdCLIR=new UmdCLIR(queryProcessorStatus);
			umdCLIR.setCachingPath(cacheFolder, overrideFlag);
			
			if(langIdCollections!=null && langIdTgtLang != null) {
				File outParentDir= new File(outRootDir.getCanonicalPath()+File.separator+"LanguageID");
				if(!outParentDir.exists()) {
					outParentDir.mkdir();
				}
				File outDir=new File(outParentDir.getCanonicalPath()+File.separator+langIdTgtLang.toString().toUpperCase());
				int counter =1;
				while(outDir.exists()) {
					outDir=new File(outDir.getCanonicalPath()+"_"+String.valueOf(counter));
					counter++;
				}
				outDir.mkdir();
				langIdPackager.createPackage(langIdCollections, langIdTgtLang, outDir.getCanonicalPath());				
			}
			
			if(domainIdCollections!=null) {
				if(domIdVersion!=null) {
					umdDomainCombination.setVersion(domIdVersion);
				}
				File outParentDir= new File(outRootDir.getCanonicalPath()+File.separator+umdDomainCombination.getVersion().replaceAll(":", "-"));
				if(!outParentDir.exists()) {
					outParentDir.mkdir();
				}
				File outDir=new File(outParentDir.getCanonicalPath()+File.separator+domainIdSubLang.toString().toUpperCase()+"_"+domainIdSet.toString()+"_"+domainIdSubDset.toString());
				int counter =1;
				while(outDir.exists()) {
					outDir=new File(outDir.getCanonicalPath()+"_"+String.valueOf(counter));
					counter++;
				}
				outDir.mkdir();
				// FileUtils.copyFile(expConfigFilePath, new File(outDir.getCanonicalPath()+File.separator+expConfigFilePath.getName()));				
				copyConfigFile(expConfigFilePath, new File(outDir.getCanonicalPath()+File.separator+expConfigFilePath.getName()), generatedIndexPath);
				umdDomainCombination.domainCombine(new File(resourceFactory.getScriptsCorporaPath()), domainIdCollections, outDir, domainIdSubLang);
			}
			
			/**
			 * Create CLIR submission
			 */
			if(expConfig!=null) {
				if(queryProcessorVersion!=null) {
					umdCLIR.setUmdQPVersion(queryProcessorVersion);
				}
				if(matcherVersion!=null) {
					umdCLIR.setUmdQMVersion(matcherVersion);
				}
				if(evidenceCombVersion!=null) {
					umdCLIR.setUmdEviCombVersion(evidenceCombVersion);
				}	
				if(rerankerVersion!=null) {
					umdCLIR.setUmdRerankerVersion(rerankerVersion);
				}	
				
				
				umdCLIR.setQPCopyFlag(false);// We don;t want the pipeline to copy the QP output to the CLIR output directory
				
				File outParentDir= new File(outRootDir.getCanonicalPath()+File.separator+umdCLIR.getVersion().replaceAll(":", "-"));
				if(!outParentDir.exists()) {
					outParentDir.mkdir();
				}
				File clirOutDir=new File(outParentDir.getCanonicalPath()+File.separator+expConfig.getConfigurationName());
				int counter =1;
				while(clirOutDir.exists()) {
					clirOutDir=new File(outParentDir.getCanonicalPath()+File.separator+expConfig.getConfigurationName()+"_"+String.valueOf(counter));
					counter++;
				}
				clirOutDir.mkdir();
				File expConfigFileOut= new File(clirOutDir.getCanonicalPath()+File.separator+expConfigFilePath.getName());
				// FileUtils.copyFile(expConfigFilePath, expConfigFileOut);
				copyConfigFile(expConfigFilePath, expConfigFileOut, generatedIndexPath);
				umdCLIR.searchQuery(expConfigFileOut, expConfig.getMathcerConfig(), expConfig.getQueriesListsPaths(), expConfig.getTgtLang(), expConfig.getEviCombCutoff(), expConfig.getEviCombFormat(), clirOutDir);

//				boolean finishedExp=true;
//				if(!clirOutDir.exists()) {
//					clirOutDir.mkdir();
//					finishedExp=false;
//				}
//				File expConfigFileOut= new File(clirOutDir.getCanonicalPath()+File.separator+expConfigFilePath.getName());
//				FileUtils.copyFile(expConfigFilePath, expConfigFileOut);
//				
//				
//				if(!finishedExp) {
//					umdCLIR.searchQuery(expConfigFileOut, expConfig.getMathcerConfig(), expConfig.getQueriesListsPaths(), expConfig.getTgtLang(), expConfig.getEviCombCutoff(), expConfig.getEviCombFormat(), clirOutDir);
//				}else {
//					umdCLIR.setECOutDir(new File(clirOutDir.getAbsolutePath()+File.separator+"UMD-CLIR-workECDir"));
//					umdCLIR.setQPOutDir(new File(clirOutDir.getAbsolutePath()+File.separator+"UMD-CLIR-workQPDir"));
//				}
								
				if(runSummarizer) {
					String clirRunName=umdCLIR.getVersion().replaceAll("_", "").replaceAll("-", "").replaceAll(":", "").replaceAll("\\.", "");
					String nistDataDir=resourceFactory.getScriptsCorporaPath();
					File summOutDir=new File(clirOutDir.getCanonicalPath()+File.separator+"CU-summaryDir");
					if(!summOutDir.exists()) {
						summOutDir.mkdir();
					}
					logger.debug(clirRunName);
					logger.debug(umdCLIR.getECOutDir().getAbsolutePath());
					logger.debug(umdCLIR.getQPOutDir().getAbsolutePath());
					logger.debug(expConfigFilePath.getAbsolutePath());
					logger.debug(summOutDir.getAbsolutePath());
					
//					ArrayList<String> queriesIDs=IoHdlr.getInstance().getListOfFilesNames(umdCLIR.getQPOutDir());	
					Hashtable<String, HashSet<File>> queriesFilesGroup = IoHdlr.getInstance().getListOfFilesGrouped(umdCLIR.getQPOutDir());
					Set<Entry<String, HashSet<File>>> queriesNamesSet = queriesFilesGroup.entrySet();
					ArrayList<String> queriesIDs =new ArrayList<String>();
					for(Entry<String, HashSet<File>> qNameSet : queriesNamesSet) {
						queriesIDs.add(qNameSet.getKey());
					}
					
//					//============================================================================================================
//					/**
//					 * Workaround. Must be removed
//					 */ 
//					File annotation=new File(summOutDir.getCanonicalPath()+File.separator+"annotations");
//					ArrayList<String> summarizedQueries=new ArrayList<String>();
//					if(annotation.exists()) {
//						summarizedQueries.addAll(IoHdlr.getInstance().getListOfDirsNames(annotation));
//					}
//					ArrayList<String> queriesIDsTmp=new ArrayList<String>();
//					for(String q:queriesIDs) {
//						if(!summarizedQueries.contains(q)) {
//							queriesIDsTmp.add(q);
//						}
//					}
//					queriesIDs=queriesIDsTmp;
//					//============================================================================================================
									
					CUsummarizer summarizer= new CUsummarizer(nistDataDir, umdCLIR.getECOutDir(), umdCLIR.getQPOutDir(),  expConfigFilePath, summOutDir, false);// run in accurate mode
					if(summarizerVersion!=null) {
						summarizer.setVersion(summarizerVersion);// it sets the version and restart the server 
					}else {
						summarizer.startServer();
					}
					if(summarizer.isValid()) {
						summarizer.summarizeQuerySearchResults(queriesIDs);		
						summarizer.createPackage(clirRunName);
					}else {
						logger.error("Summarizer is not responding ...");
					}
					summarizer.shutdown();
				}
				if(evaluationFlag) {
					if(evaluatorVersion!=null) {
						evaluator.setEvalVersion(evaluatorVersion);
					}		
					String refDataLocation=resourceFactory.getScriptsCorporaPath();
					if(relevJudgmentsPath!=null && !relevJudgmentsPath.isEmpty()) {
						refDataLocation=relevJudgmentsPath;
					}
					evaluator.evaluate(umdCLIR.getECOutDir(), expConfigFileOut, refDataLocation, clirOutDir);
					
				}
				
				clirPackager.createPackage(umdCLIR.getECOutDir(), expConfigFileOut, clirOutDir);				
			}
			
		} catch (IOException e) {
			logger.fatal(e.getMessage());
		}		
	}
	
//	private void renameSumbission(File submissionPath, Task task, SubmissionType type, SubmissionSet set, String lang, SubmissionDataset dataset, File outDir) {
//		try {
//			String cmd="python"+" "+renameSubmissionScript.getCanonicalPath()+" "+"--submission_file "+submissionPath.getCanonicalPath()+" "+"--team "+team+" "+"--task "+task.toString();
//			cmd+=" "+"--sub_type "+type.toString()+" "+" --set "+set.toString()+" "+"--lang "+lang+" "+"--dataset "+ dataset.toString()+" "+"--outpath "+outDir.getCanonicalPath();
//			
//			logger.debug(cmd);
//			
//			Process process = Runtime.getRuntime().exec(cmd);
//			BufferedReader reader =	new BufferedReader(new InputStreamReader(process.getInputStream()));
//			while ((reader.readLine()) != null) {}
//			process.waitFor();
//			
//		} catch (IOException | InterruptedException  e) {
//			logger.error(e.getMessage());			
//		}
//		
//	}
	
	
	private UmdCLIRConfig parseconfig(File expConfigFilePath) {
		String jsonStr=IoHdlr.getInstance().readFile(expConfigFilePath);
		JSONObject expConfig = new JSONObject(jsonStr);
//		subType=SubmissionType.valueOf(expConfig.getString("submission_type").trim());
		
		if(expConfig.has("summarizer")) {
			runSummarizer=true;
			if(expConfig.getJSONObject("summarizer").has("version")) {
    			summarizerVersion=expConfig.getJSONObject("summarizer").getString("version").trim();    			
    		}else {
    			summarizerVersion=null;
    		}
		}
		
		if(expConfig.has("language_id")) {
			if(expConfig.getJSONObject("language_id").has("target_language")) {
				langIdTgtLang=Language.valueOf(expConfig.getJSONObject("language_id").getString("target_language").trim());
			}else {
				langIdTgtLang=null;
			}
			
			if(expConfig.getJSONObject("language_id").has("set")){
				JSONArray langIdset = expConfig.getJSONObject("language_id").getJSONArray("set");
				langIdCollections=new ArrayList<File>();
				for (int i = 0; i < langIdset.length(); i++){
					langIdCollections.add(new File(langIdset.getString(i).trim()));					
				}				
			}else {
				langIdCollections=null;
			}			
		}
		
		
		
		if(expConfig.has("domain_modeling")) {
			if(expConfig.getJSONObject("domain_modeling").has("version")) {        
				domIdVersion=expConfig.getJSONObject("domain_modeling").getString("version").trim();
			}else {
				domIdVersion=null;
			}
//			domainIdSet=SubmissionSet.valueOf(expConfig.getJSONObject("domain_modeling").getString("set").trim());
			domainIdSet=expConfig.getJSONObject("domain_modeling").getString("set").trim();
			
			domainIdSubLang=Language.valueOf(expConfig.getJSONObject("domain_modeling").getString("target_language").trim());
//			Language lang=Language.valueOf(expConfig.getJSONObject("domain_modeling").getString("target_language").trim());
//			if(lang==Language.sw) {
//				domainIdSubLang="1A";
//			}else if(lang==Language.tl) {
//				domainIdSubLang="1B";
//			}else if(lang==Language.so) {
//				domainIdSubLang="1S";
//			}		
			JSONArray domIdCollections = expConfig.getJSONObject("domain_modeling").getJSONArray("collections");
			StringBuilder uniqueCollectionNameDomId= new StringBuilder();
			
			domainIdCollections=new ArrayList<String>();
			for (int i = 0; i < domIdCollections.length(); i++){
				domainIdCollections.add(domIdCollections.getString(i).trim());
				String collectionName=domIdCollections.getString(i).trim().replaceAll(".*/(EVAL(\\d+)?)/.*", "$1").replaceAll(".*/(DEV(\\d+)?)/.*", "$1").replaceAll(".*/(ANALYSIS(\\d+)?)/.*", "$1");
				if(!uniqueCollectionNameDomId.toString().trim().contains(collectionName)) {
					uniqueCollectionNameDomId.append(" "+collectionName);
				}
			}
//			domainIdSubDset=SubmissionDataset.valueOf(uniqueCollectionNameDomId.toString().trim().replaceAll("\\s+", ""));
			domainIdSubDset=uniqueCollectionNameDomId.toString().trim().replaceAll("\\s+", "");
			
		}else {
			domainIdSet=null;
			domainIdCollections=null;
			domainIdSubLang=null;
			domainIdSubDset=null;
			domIdVersion=null;
		}
		
		if(expConfig.has("evaluator")) {
			evaluationFlag=true;
			if(expConfig.getJSONObject("evaluator").has("version")) {
				evaluatorVersion=expConfig.getJSONObject("evaluator").getString("version").trim();
			}else {
				evaluatorVersion=null;
			}
			if(expConfig.getJSONObject("evaluator").has("relevance_judgments")) {
				relevJudgmentsPath=expConfig.getJSONObject("evaluator").getString("relevance_judgments").trim();
			}else {
				relevJudgmentsPath=null;
			}
						
		}
		
		
		if(expConfig.has("reranker")) {
			if(expConfig.getJSONObject("reranker").has("version")) {
				rerankerVersion=expConfig.getJSONObject("reranker").getString("version").trim();
			}else {
				rerankerVersion=null;
			}			
		}
		
		if(expConfig.has("query_processor") && expConfig.has("evidence_combination") && expConfig.has("matcher")) {					
			if(expConfig.getJSONObject("query_processor").has("version")) {
				queryProcessorVersion=expConfig.getJSONObject("query_processor").getString("version").trim();
			}else {
				queryProcessorVersion=null;
			}
			
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
//			if(tgtLang==Language.sw) {
//				clirSubLang="1A";
//			}else if(tgtLang==Language.tl) {
//				clirSubLang="1B";
//			}else if(tgtLang==Language.so) {
//				clirSubLang="1S";
//			}
			StringBuilder uniqueExpName=new StringBuilder(tgtLang.toString().toUpperCase());
			
//			File queryListPath=new File(expConfig.getJSONObject("query_processor").getString("query_list_path").trim());
//			uniqueExpName.append("_"+new File(queryListPath.getParent()).getName());
//			clirQuerySet=SubmissionSet.valueOf(new File(queryListPath.getParent()).getName());
			
			JSONArray qLists = expConfig.getJSONObject("query_processor").getJSONArray("query_list_path");
			StringBuilder qListNames = new StringBuilder();
			ArrayList<File>queriesListsPaths=new ArrayList<File>();
			String nistDataDir= resourceFactory.getScriptsCorporaPath();
			for (int i = 0; i < qLists.length(); i++){
				File queryPathTmp = new File(qLists.getString(i).trim());
				if(!queryPathTmp.exists()) {// i.e. the input is a relative path not an absolute path
					queryPathTmp = new File(nistDataDir+File.separator+qLists.getString(i).trim());
				}
				queriesListsPaths.add(queryPathTmp);
				if(queriesListsPaths.get(i).isDirectory()) {// i.e. already processed queries
					qListNames.append(queriesListsPaths.get(i).getName());
				}else {// i.e. new queries that need to be processed 
					qListNames.append(new File(queriesListsPaths.get(i).getParent()).getName());
					queryProcessorStatus=true;
				}								
			}
			uniqueExpName.append("_"+qListNames.toString());			
//			clirQuerySet=SubmissionSet.valueOf(qListNames.toString());
			
			Hashtable<String,String>collectionNames=new Hashtable<String,String>();
			Hashtable<String,String>indexNames=new Hashtable<String,String>();
			StringBuilder uniqueConfigName= new StringBuilder();
			StringBuilder uniqueCollectionName= new StringBuilder();
			StringBuilder uniqueIndexName= new StringBuilder();	
					
			int eviComCutoff=expConfig.getJSONObject("evidence_combination").getInt("cutoff");
//			UmdCLIRCombFormat eviComFormat=UmdCLIRCombFormat.valueOf(expConfig.getJSONObject("evidence_combination").getString("format").trim());
			
			JSONArray matcherExps = expConfig.getJSONObject("matcher").getJSONArray("configurations");
			ArrayList<UmdMatcherConfig> matcherConfigs=new ArrayList<UmdMatcherConfig>();
			for (int i = 0; i < matcherExps.length(); i++){
				JSONArray indexes = matcherExps.getJSONObject(i).getJSONArray("indexes");
				ArrayList<String>indexesTmp=new ArrayList<String>();
				for (int j = 0; j < indexes.length(); j++){
					indexesTmp.add(indexes.getString(j).trim());
					String collectionName=indexes.getString(j).trim().replaceAll(".*/(EVAL(\\d+)?)/.*", "$1").replaceAll(".*/(DEV(\\d+)?)/.*", "$1").replaceAll(".*/(ANALYSIS(\\d+)?)/.*", "$1");
					if(!collectionNames.containsKey(collectionName)) {
						uniqueCollectionName.append(" "+collectionName);
						collectionNames.put(collectionName, "1");
					}
									
					String indexName=new File(new File(indexes.getString(j).trim()).getParent()).getName();
					if(!indexNames.containsKey(indexName)) {
						uniqueIndexName.append(" "+indexName);
						indexNames.put(indexName, "1");
					}				
				}
//				UmdCLIRMatchingType type = UmdCLIRMatchingType.valueOf(matcherExps.getJSONObject(i).getString("type").trim());
				String type = matcherExps.getJSONObject(i).getString("type").trim();
				int cutoff=matcherExps.getJSONObject(i).getInt("cutoff");
				UmdCLIRCombFormat format = UmdCLIRCombFormat.valueOf(matcherExps.getJSONObject(i).getString("format").trim());
				String configName=matcherExps.getJSONObject(i).getString("config_name");
				uniqueConfigName.append(" "+configName);
				
				matcherConfigs.add(new UmdMatcherConfig(indexesTmp, type, cutoff, format, configName, expConfigFilePath));
			}
//			clirSubDset=SubmissionDataset.valueOf(uniqueCollectionName.toString().trim().replaceAll("\\s+", ""));
					
			uniqueConfigName.append(" "+"Cutoff"+String.valueOf(eviComCutoff));
			
			uniqueExpName.append("_"+uniqueCollectionName.toString().trim().replaceAll("\\s+", "_"));
//			uniqueExpName.append("_"+uniqueIndexName.toString().trim().replaceAll("\\s+", "_"));
			uniqueExpName.append("_"+uniqueConfigName.toString().trim().replaceAll("\\s+", "_"));
//			return new UmdCLIRConfig(uniqueExpName.toString().trim().replaceAll(":", "-"), matcherConfigs, queryListPath, tgtLang, eviComCutoff, eviComFormat);
//			return new UmdCLIRConfig(uniqueExpName.toString().trim().replaceAll(":", "-"), matcherConfigs, queriesListsPaths, tgtLang, eviComCutoff, eviComFormat);
			return new UmdCLIRConfig(uniqueExpName.toString().trim().replaceAll(":", "-"), matcherConfigs, queriesListsPaths, tgtLang, eviComCutoff, UmdCLIRCombFormat.tsv);
		}else {
//			clirSubLang=null;
//			clirQuerySet=null;
//			clirSubDset=null;
			return null;
		}
	}
	public void shutdown() {
		umdCLIR.shutdown();
	}

}
