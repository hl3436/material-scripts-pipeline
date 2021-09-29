/**
 * 
 */
package edu.columbia.dsi.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import org.json.JSONArray;
import org.json.JSONObject;
import edu.columbia.dsi.SCRIPTS.Language;


/**
 * @author badrashiny
 * Dec 3, 2018
 */
public class TuningConfigBuilder {
//	private ResourceFactory resourceFactory = new ResourceFactory();
//	
//	public class QueryProcessObj {
//		String version=resourceFactory.getUmdQueryProcessorVersion();
//		String[] queries;
//		Language lang;
//		public QueryProcessObj(Language lang, String[] qNames) {
//			this.lang=lang;
//			String stdLang=null;
//			if(lang==Language.sw) {
//				stdLang="1A";
//			}else if(lang==Language.tl) {
//				stdLang="1B";
//			}else if(lang==Language.so) {
//				stdLang="1S";
//			}
//			String queriesPath=resourceFactory.getScriptsCorporaPath()+File.separator+stdLang+File.separator+"IARPA_MATERIAL_BASE-"+stdLang+File.separator;
//			queriesPath+="query_store"+File.separator+version.replaceAll("[/_:]", "-")+File.separator;
//			queries=new String[qNames.length];
//			for(int i=0; i<qNames.length ;i++) {
//				queries[i]=queriesPath+qNames[i]+File.separator;
//			}		
//		}
//	}
//		
//	
//	public class EveCombObj {
//		String version=resourceFactory.getUmdEvidenceCombinationVersion();
//		String cutoff; 
//		String scoreType;
//		/**
//		 * 
//		 * @param cutoff: ex: 40
//		 * @param scoreType: ex: borda
//		 */
//		public EveCombObj(String cutoff, String scoreType) {
//			this.cutoff=cutoff;
//			this.scoreType=scoreType;			
//		}
//				
//		
//	}
//
//	public class EvalObj {
//		String version=resourceFactory.getEvaluatorVersion();
//		String mode="light";
//		String relevance_judgments="/storage/data/NIST-data/relevance_judgments";
//		String beta="40";
//		public EvalObj() {			
//		}
//	}
	
	
	
	public class MatcherConfigurationObj {		
		private String configName;
		private String type;
		private String indexType;
		private String mtType;
		private ArrayList<String> indexes=new ArrayList<String>();
		/**
		 * 
		 * @param configName: ex: SMTsrp
		 * @param type: ex: indri
		 * @param indexType: ex: words
		 * @param mtType: ex: EdiNMT. if null, it will be ignored
		 */
		public MatcherConfigurationObj(String configName, String type, String indexType, String mtType, ArrayList<String> indexes) {
			this.configName=configName;
			this.type=type;
			this.indexType=indexType;
			this.mtType=mtType;
			this.indexes.addAll(indexes);			
		}
		public String getConfigName() {
			return configName;
		}
		public JSONObject getJson() {
			JSONObject configuration= new JSONObject();
			configuration.put("config_name", configName);
			configuration.put("type", type);
			configuration.put("index_type", indexType);
			if(!mtType.toLowerCase().equals("null")) {
				configuration.put("mt_type", mtType);
			}
			configuration.put("format", "tsv");
			configuration.put("cutoff", new Integer(-1));
			JSONArray jsonIndexes = new JSONArray();
			for(String index:indexes) {
				jsonIndexes.put(index);
			}
			configuration.put("indexes", jsonIndexes);
			return configuration;			
		}
	}
	
	public class DescriptionObj {
		String description;
		public DescriptionObj(String description) {
			this.description=description;			
		}
		public JSONObject getJson() {
			JSONObject desc = new JSONObject();
			desc.put("tags", description);			
			return desc;			
		}
	}
	private ResourceFactory resourceFactory = new ResourceFactory();
	JSONObject expConfigTemplate;
	ArrayList<MatcherConfigurationObj> matchersConfigsList=new ArrayList<MatcherConfigurationObj>();
	String matcherVer=resourceFactory.getUmdQueryMatcherVersion();
	StringBuilder uniqueExpName;
	public TuningConfigBuilder(File configTemplate, File mathcersList) {
		String jsonStr=IoHdlr.getInstance().readFile(configTemplate);
		expConfigTemplate = new JSONObject(jsonStr);
		if(expConfigTemplate.has("matcher")) {
			if(expConfigTemplate.getJSONObject("matcher").has("version")) {
				matcherVer=expConfigTemplate.getJSONObject("matcher").getString("version").trim();
			}
			expConfigTemplate.remove("matcher");
		}
		
		JSONArray collectionList = expConfigTemplate.getJSONObject("data_collection").getJSONArray("collections");
		ArrayList<String>collections=new ArrayList<String>();
		for (int i = 0; i < collectionList.length(); i++){
			collections.add(collectionList.getString(i).trim().replaceAll("\\/$", ""));							
		}
		
		ArrayList<String> mathcersTmp=IoHdlr.getInstance().readFileLinesWithoutHeader(mathcersList);
		
		for(String line:mathcersTmp) {
			String[]tmp=line.trim().split("\t+");
			if(tmp.length!=5) {
				continue;
			}
			String configName=tmp[0].trim();
			String	type=tmp[1].trim();
			String	indexType=tmp[2].trim();
			String mtType=tmp[3].trim();
			String[] indexesTmp=tmp[4].trim().split(";;");
			ArrayList<String> indexes=new ArrayList<String>();
			for(String collection:collections) {
				for(String indexTmp:indexesTmp) {
					String index=collection+File.separator+indexTmp;
					indexes.add(index);
				}
			}
			MatcherConfigurationObj matcherConfig=new MatcherConfigurationObj(configName, type, indexType, mtType, indexes);
			matchersConfigsList.add(matcherConfig);			
		}
		
		Language tgtLang=Language.valueOf(expConfigTemplate.getJSONObject("query_processor").getString("target_language").trim());
		uniqueExpName=new StringBuilder(tgtLang.toString().toUpperCase());
		JSONArray qLists = expConfigTemplate.getJSONObject("query_processor").getJSONArray("query_list_path");
		StringBuilder qListNames = new StringBuilder();
		ArrayList<File>queriesListsPaths=new ArrayList<File>();
		for (int i = 0; i < qLists.length(); i++){
			queriesListsPaths.add(new File(qLists.getString(i).trim()));
			if(queriesListsPaths.get(i).isDirectory()) {
				qListNames.append(queriesListsPaths.get(i).getName());
			}else {
				qListNames.append(new File(queriesListsPaths.get(i).getParent()).getName());
			}							
		}
		uniqueExpName.append("_"+qListNames.toString());		
	}
	
	public ArrayList<MatcherConfigurationObj> getMatchersList(){
		return matchersConfigsList;
	}
	

	public String build(ArrayList<MatcherConfigurationObj> matchersConfigs, String description, String outPath) throws IOException {
		if(expConfigTemplate.has("matcher")) {
			expConfigTemplate.remove("matcher");
		}
		if(expConfigTemplate.has("description")) {
			expConfigTemplate.remove("description");
		}
		JSONObject matcher=new JSONObject();
		matcher.put("version", matcherVer);
		
		JSONArray configurations = new JSONArray();
		for(MatcherConfigurationObj matchersConfig:matchersConfigs) {			
			configurations.put(matchersConfig.getJson());
		}
		matcher.put("configurations", configurations);
		
		expConfigTemplate.put("matcher", matcher);
		
		
		DescriptionObj desc=new DescriptionObj(description);
		
		expConfigTemplate.put("description", desc.getJson());	
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(outPath));
		writer.write(expConfigTemplate.toString(4));
		writer.close();
		
		
		String  umdCLIRVersion=expConfigTemplate.getJSONObject("query_processor").getString("version").trim();
		umdCLIRVersion+="_"+expConfigTemplate.getJSONObject("matcher").getString("version").trim();
		umdCLIRVersion+="_"+expConfigTemplate.getJSONObject("evidence_combination").getString("version").trim();
		
		Hashtable<String,String>collectionNames=new Hashtable<String,String>();
		StringBuilder uniqueCollectionName= new StringBuilder();
		StringBuilder uniqueConfigName= new StringBuilder();
		JSONArray matcherExps = expConfigTemplate.getJSONObject("matcher").getJSONArray("configurations");
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
			}
			String configName=matcherExps.getJSONObject(i).getString("config_name");
			uniqueConfigName.append(" "+configName);			
		}
		int eviComCutoff=expConfigTemplate.getJSONObject("evidence_combination").getInt("cutoff");
		uniqueConfigName.append(" "+"Cutoff"+String.valueOf(eviComCutoff));
			
		StringBuilder expOutDir=new StringBuilder(uniqueExpName.toString());
		expOutDir.append("_"+uniqueCollectionName.toString().trim().replaceAll("\\s+", "_"));
		expOutDir.append("_"+uniqueConfigName.toString().trim().replaceAll("\\s+", "_"));
		String clirOutDir= umdCLIRVersion.replaceAll(":", "-")+File.separator+expOutDir.toString().trim().replaceAll(":", "-");
		
		return clirOutDir;		
	}
}
