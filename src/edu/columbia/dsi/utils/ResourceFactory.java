/**
 * 
 */
package edu.columbia.dsi.utils;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;

import org.apache.log4j.Logger;

import edu.columbia.dsi.SCRIPTS.Language;
import edu.columbia.dsi.containers.DocInfo.Type;

/**
 * @author badrashiny
 * Mar 8, 2018
 */
public class ResourceFactory {
	private String CONFIG_FILE = "scripts.properties";
	private static Logger logger = Logger.getLogger(ResourceFactory.class.getSimpleName());
	private Properties props = Properties.getInstance();
	private String resourcesDir=null;
	private String asrResourcesDir=null;
	private String clirResourcesDir=null;
	private String mtResourcesDir=null;
	private String morphAnalyzerResourcesDir=null;
	private String mtPosteditResourcesDir=null;
	private String stemmerResourcesDir=null;
	private String identificationResourcesDir=null;
	private String utilsResourcesDir=null;
	private String indexerResourcesDir=null;
	private String summarizationResourcesDir=null;
	private String testingResourcesDir=null;
	private String demoResourcesDir=null;
	
	private Hashtable<String,String>asrVersionsMap=new Hashtable<String,String>();
	private ArrayList<Language> asrSupportedLanguages=new ArrayList<Language>();
	
	private Hashtable<String,String>kwsVersionsMap=new Hashtable<String,String>();
	private ArrayList<Language> kwsSupportedLanguages=new ArrayList<Language>();

	private ArrayList<Language> morphAnalyzerSupportedLanguages=new ArrayList<Language>();
	private ArrayList<Language> langIDSupportedLanguages=new ArrayList<Language>();
	private ArrayList<Language> domIDSupportedLanguages=new ArrayList<Language>();
	private ArrayList<Language> audioSentSplitterSupportedLanguages=new ArrayList<Language>();
	
	private Hashtable<Language, ArrayList<Type>> ediNMTSupportedLanguagesAndTypes=new Hashtable<Language, ArrayList<Type>>();
	private Hashtable<Language, ArrayList<Type>> umdSMTSupportedLanguagesAndTypes=new Hashtable<Language, ArrayList<Type>>();
	private Hashtable<Language, ArrayList<Type>> umdNMTSupportedLanguagesAndTypes=new Hashtable<Language, ArrayList<Type>>();
	private Hashtable<Language, ArrayList<Type>> umdNNltmSupportedLanguagesAndTypes=new Hashtable<Language, ArrayList<Type>>();
	private Hashtable<Language, ArrayList<Type>> umdNMTPsqSupportedLanguagesAndTypes=new Hashtable<Language, ArrayList<Type>>();
	private HashSet<Language> mtSupportedLanguages=new HashSet<Language>();
	
	/**
	 * 
	 */
	public ResourceFactory() {
		resourcesDir=Resources.getInstance().getResourcesDirCanonicalPath();
		File propF=Resources.getInstance().getPropFilePath();
		if(propF==null) {//i.e. use default properties file
			CONFIG_FILE = "scripts.properties";
			clirResourcesDir=resourcesDir+File.separator+props.getValue(CONFIG_FILE, "scripts.clir.resources.path");
		}else {//i.e. use external properties file
			CONFIG_FILE = propF.getName();
			clirResourcesDir=resourcesDir+File.separator+props.getValue(propF, "scripts.clir.resources.path");
		}
		asrResourcesDir=resourcesDir+File.separator+props.getValue(CONFIG_FILE, "scripts.asr.resources.path");	
		mtResourcesDir=resourcesDir+File.separator+props.getValue(CONFIG_FILE, "scripts.mt.resources.path");
		morphAnalyzerResourcesDir=resourcesDir+File.separator+props.getValue(CONFIG_FILE, "scripts.morph.analyzer.resources.path");
		identificationResourcesDir=resourcesDir+File.separator+props.getValue(CONFIG_FILE, "scripts.identification.resources.path");
		utilsResourcesDir=resourcesDir+File.separator+props.getValue(CONFIG_FILE, "scripts.utils.resources.path");
		indexerResourcesDir=resourcesDir+File.separator+props.getValue(CONFIG_FILE, "scripts.indexing.resources.path");
		summarizationResourcesDir=resourcesDir+File.separator+props.getValue(CONFIG_FILE, "scripts.summarizers.resources.path");
		mtPosteditResourcesDir=resourcesDir+File.separator+props.getValue(CONFIG_FILE, "scripts.mt.postedit.resources.path");
		stemmerResourcesDir=resourcesDir+File.separator+props.getValue(CONFIG_FILE, "scripts.stemmer.resources.path");
		testingResourcesDir=resourcesDir+File.separator+props.getValue(CONFIG_FILE, "scripts.testing.resources.path");
		demoResourcesDir=resourcesDir+File.separator+props.getValue(CONFIG_FILE, "scripts.demo.resources.path");
		
		
		String[] asrVersions=props.getValue(CONFIG_FILE, "scripts.asr.docker.versions").trim().split(";;");
		String[] asrLangs=props.getValue(CONFIG_FILE, "scripts.asr.docker.langs").trim().split(";;");
		if(asrVersions.length==asrLangs.length) {
			for(int i=0;i<asrLangs.length;i++) {
				try {
					Language lang = Language.valueOf(asrLangs[i]);
				    asrVersionsMap.put(asrLangs[i], asrVersions[i]);
					asrSupportedLanguages.add(lang);				      
				    } catch (IllegalArgumentException ex) { 
				    	logger.fatal("input ASR language is not a valid system language: "+ asrLangs[i]);
				    }				
			}
		}	
		
		String[] kwsVersions=props.getValue(CONFIG_FILE, "scripts.asr.kws.docker.version").trim().split(";;");
		String[] kwsLangs=props.getValue(CONFIG_FILE, "scripts.asr.kws.docker.langs").trim().split(";;");
		if(kwsVersions.length==kwsLangs.length||kwsVersions.length==1) {//i.e. either a separate version for each language or just one version for all languages
			for(int i=0;i<kwsLangs.length;i++) {
				try {
					Language lang = Language.valueOf(kwsLangs[i]);
					String ver=null;
					if(kwsVersions.length==1) {//i.e one version for all languages
						ver=kwsVersions[0];
					}else {//i.e separate version for each language
						ver=kwsVersions[i];
					}
				    kwsVersionsMap.put(kwsLangs[i], ver);
					kwsSupportedLanguages.add(lang);				      
				    } catch (IllegalArgumentException ex) { 
				    	logger.fatal("input KWS language is not a valid system language: "+ kwsLangs[i]);
				    }				
			}
		}	
		

		
		
		morphAnalyzerSupportedLanguages.addAll(fillSupportedLangauges("scripts.morph.analyzer.docker.langs"));
		langIDSupportedLanguages.addAll(fillSupportedLangauges("scripts.language.identifcation.docker.langs"));
		domIDSupportedLanguages.addAll(fillSupportedLangauges("scripts.domain.identifcation.docker.langs"));
		audioSentSplitterSupportedLanguages.addAll(fillSupportedLangauges("scripts.utils.audio.sentence.splitter.langs"));
		
		ediNMTSupportedLanguagesAndTypes.putAll(fillSupportedLangsAndTypes("scripts.translator.nmt.docker.langs.types"));
		mtSupportedLanguages.addAll(ediNMTSupportedLanguagesAndTypes.keySet());
		umdNMTSupportedLanguagesAndTypes.putAll(fillSupportedLangsAndTypes("scripts.translator.umd.nmt.docker.langs.types"));
		mtSupportedLanguages.addAll(umdNMTSupportedLanguagesAndTypes.keySet());
		umdSMTSupportedLanguagesAndTypes.putAll(fillSupportedLangsAndTypes("scripts.translator.umd.smt.docker.langs.types"));
		mtSupportedLanguages.addAll(umdSMTSupportedLanguagesAndTypes.keySet());
		umdNNltmSupportedLanguagesAndTypes.putAll(fillSupportedLangsAndTypes("scripts.translator.umd.nnltm.docker.langs.types"));
		mtSupportedLanguages.addAll(umdNNltmSupportedLanguagesAndTypes.keySet());	
		umdNMTPsqSupportedLanguagesAndTypes.putAll(fillSupportedLangsAndTypes("scripts.translator.umd.nmt.psq.docker.langs.types"));
		mtSupportedLanguages.addAll(umdNMTPsqSupportedLanguagesAndTypes.keySet());
	}	
	//=========================================SCRIPTS=====================================================================
	
	private HashSet<Language> fillSupportedLangauges(String propString){
		String[] langs=props.getValue(CONFIG_FILE, propString).trim().split(";;");
		HashSet<Language>langSet=new HashSet<Language>();
		for(int i=0;i<langs.length;i++) {
			try {
				Language lang = Language.valueOf(langs[i]);
				langSet.add(lang);				      
			    } catch (IllegalArgumentException ex) { 
			    	logger.fatal("input audSentSplitter language is not a valid system language: "+ langs[i]);
			    }				
		}
		return langSet;
	}
	
	private Hashtable<Language, ArrayList<Type>> fillSupportedLangsAndTypes(String propString) {
		Hashtable<Language, ArrayList<Type>> supportedLanguagesAndTypes= new Hashtable<Language, ArrayList<Type>>();
		String[] langsTypes=props.getValue(CONFIG_FILE, propString).trim().split(";;");
		for(int i=0;i<langsTypes.length;i++) {
			try {
				if(!langsTypes.equals("null")) {
					String[] tmp=langsTypes[i].split("@@");
					if(tmp.length!=2) {// it must be in this format lang@@types
						logger.fatal("input language or type is not in the expected format [language@@types]: "+ langsTypes[i]);
						continue;
					}
					Language lang = Language.valueOf(tmp[0]);
					ArrayList<Type> types=new ArrayList<Type>(); 
					String []typesTmp=tmp[1].split("::");
					for(String t:typesTmp) {
						types.add(Type.valueOf(t));
					}
					
					supportedLanguagesAndTypes.put(lang,types);	
				}
				
			} catch (IllegalArgumentException ex) { 
				logger.fatal("input language or type is not a valid system variable: "+ langsTypes[i]);
			}				
		}
		return supportedLanguagesAndTypes;		
	}
	
	public String getScriptsCorporaPath() {
		return props.getValue(CONFIG_FILE, "scripts.corpora.path");
	}
	public File getCreateSubmissionNameScript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.create.submission.filename");
		return new File (resourcesDir+File.separator+scriptName);
	}
	
	//=========================================Demo============================================================================	
	
	public File getDemoStartServerScript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.demo.startserver.script.name");
		return new File (demoResourcesDir+File.separator+scriptName);
	}
	
	public File getDemoShutdownServerScript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.demo.shutdownserver.script.name");
		return new File (demoResourcesDir+File.separator+scriptName);
	}
	
	public File getDemoSearchQueryScript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.demo.searchquery.script.name");
		return new File (demoResourcesDir+File.separator+scriptName);
	}
	
	public File getDemoDeployFilePath() {
		String fileName=props.getValue(CONFIG_FILE, "scripts.demo.deploy.file.name");
		return new File (demoResourcesDir+File.separator+fileName);		
	}
	
	public String getDemoMaxNumOfDocumnets() {
		return props.getValue(CONFIG_FILE, "scripts.demo.max.num.docs");	
	}
	
	//=========================================Indexer==========================================================================
	public String getUmdIndexerVersion() {
		return props.getValue(CONFIG_FILE, "scripts.indexing.umd.docker.version");
	}

	public String getIndexerResourceDir() {
		return indexerResourcesDir;
	}
	
	public File getUmdIndexerScript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.indexing.umd.script.name");
		return new File (indexerResourcesDir+File.separator+scriptName);		
	}

	public String getUmdIndexerParameters() {
		return props.getValue(CONFIG_FILE, "scripts.indexing.umd.parameters");
	}
	
	public File getUmdIndexerParametersPath(){
		String parametersDirName=props.getValue(CONFIG_FILE, "scripts.indexing.umd.parameters");
		return new File (indexerResourcesDir+File.separator+parametersDirName);
	}
	
	//=========================================Preprocessor=====================================================================	
	public int getRamPerCPU() {
//		return Integer.valueOf(props.getValue(CONFIG_FILE, "script.config.giga.ram.per.cpu")).intValue();
		return (int) (((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getTotalPhysicalMemorySize()/(1024*1024*1024));
	}
	public int getAvailableGpuNumber() {
		return Integer.valueOf(props.getValue(CONFIG_FILE, "script.config.gpu.number")).intValue();
	}
	public int getRamPerGPU() {
		return Integer.valueOf(props.getValue(CONFIG_FILE, "script.config.giga.ram.per.gpu")).intValue();
	}
	//=========================================CLIR========================================================================
	public String getCLIRServerHostName() {
		return props.getValue(CONFIG_FILE, "scripts.clir.host.name");
	}
	
	//============1-UMD query processor===================
	public String getUmdQueryProcessorVersion() {
		return props.getValue(CONFIG_FILE, "scripts.umd.query.processor.docker.version");
	}
	
	public File getUmdQueryProcessorStandaloneScript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.umd.query.processor.standalone.script.name");
		return new File (clirResourcesDir+File.separator+scriptName);
	}
	
	public File getUmdQueryProcessorServerScript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.umd.query.processor.startserver.script.name");
		return new File (clirResourcesDir+File.separator+scriptName);
	}
	
	public File getUmdQueryProcessorClientScript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.umd.query.processor.processquery.script.name");
		return new File (clirResourcesDir+File.separator+scriptName);
	}
	
	public int getUmdQueryProcessorPortNumber() {		
		return Integer.valueOf(props.getValue(CONFIG_FILE, "scripts.umd.query.processor.port.number")).intValue();
	}
	
	public int getUmdQueryProcessorMaxConcurrentTasks() {	
		int maxThreads=Runtime.getRuntime().availableProcessors();
		int userSpecifiedMaxThreads=Integer.valueOf(props.getValue(CONFIG_FILE, "scripts.umd.query.processor.max.concurrent.tasks")).intValue();
		return Math.min(maxThreads,userSpecifiedMaxThreads);
	}
	
	
	//============2-UMD query matcher===================
	public String getUmdQueryMatcherVersion() {
		return props.getValue(CONFIG_FILE, "scripts.umd.query.matcher.docker.version");
	}
	
	public File getUmdQueryMatcherStandaloneScript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.umd.query.matcher.standalone.script.name");
		return new File (clirResourcesDir+File.separator+scriptName);
	}
	
	public File getUmdQueryMatcherServerScript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.umd.query.matcher.startserver.script.name");
		return new File (clirResourcesDir+File.separator+scriptName);
	}
	
	public File getUmdQueryMatcherClientScript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.umd.query.matcher.matchquery.script.name");
		return new File (clirResourcesDir+File.separator+scriptName);
	}
	
	public int getUmdQueryMatcherPortNumber() {		
		return Integer.valueOf(props.getValue(CONFIG_FILE, "scripts.umd.query.matcher.port.number")).intValue();
	}
	
	public int getUmdQueryMatcherMaxConcurrentTasks() {	
		int maxThreads=Runtime.getRuntime().availableProcessors();
		int userSpecifiedMaxThreads=Integer.valueOf(props.getValue(CONFIG_FILE, "scripts.umd.query.matcher.max.concurrent.tasks")).intValue();
		return Math.min(maxThreads,userSpecifiedMaxThreads);
	}
	
	
	public String getUmdQueryMatcherUniqueIdVersion() {
		return props.getValue(CONFIG_FILE, "scripts.umd.matcher.unique.identifier.docker.version");
	}
	
	public File getUmdQueryMatcherUniqueIdScript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.umd.matcher.unique.identifier.script.name");
		return new File (clirResourcesDir+File.separator+scriptName);
	}
	
	
	//============3-UMD evidence combination===================
	public String getUmdEvidenceCombinationVersion() {
		return props.getValue(CONFIG_FILE, "scripts.umd.evidence.combination.docker.version");
	}
	
	public File getUmdEvidenceCombinationStandaloneScript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.umd.evidence.combination.standalone.script.name");
		return new File (clirResourcesDir+File.separator+scriptName);
	}
	
	public File getUmdEvidenceCombinationScript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.umd.evidence.combination.script.name");
		return new File (clirResourcesDir+File.separator+scriptName);
	}
	
	public int getUmdEvidenceCombinationMaxConcurrentTasks() {	
		int maxThreads=Runtime.getRuntime().availableProcessors();
		int userSpecifiedMaxThreads=Integer.valueOf(props.getValue(CONFIG_FILE, "scripts.umd.evidence.combination.max.concurrent.tasks")).intValue();
		return Math.min(maxThreads,userSpecifiedMaxThreads);
	}
	
	//============4-UMD domain combination===================
	public String getUmdDomainCombinationVersion() {
		return props.getValue(CONFIG_FILE, "scripts.umd.domain.combination.docker.version");
	}	
	
	public File getUmdDomainCombinationScript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.umd.domain.combination.script.name");
		return new File (clirResourcesDir+File.separator+scriptName);
	}
	
	//============5-UMD Reranker===================
	public String getUmdRerankerVersion() {
		return props.getValue(CONFIG_FILE, "scripts.umd.reranker.docker.version");
	}

	public File getUmdRerankerStandaloneScript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.umd.reranker.standalone.script.name");
		return new File (clirResourcesDir+File.separator+scriptName);
	}
		
		
	
	
	
	public String getCLIRVersion() {
		return props.getValue(CONFIG_FILE, "scripts.clir.docker.version");
	}
	
	
	
	public File getCLIRTagalogResourcePath() {
		String modelName=props.getValue(CONFIG_FILE, "scripts.clir.tagalog.model.name");
		return new File (clirResourcesDir+File.separator+modelName);
	}
	
	public int getCLIRTagalogPortNumber() {		
		return Integer.valueOf(props.getValue(CONFIG_FILE, "scripts.clir.tagalog.port.number")).intValue();
	}
	
	public String getCLIRTagalogIndexDB() {		
		return props.getValue(CONFIG_FILE, "scripts.clir.tagalog.index.name");
	}
	
	public File getCLIRSwahiliResourcePath() {
		String modelName=props.getValue(CONFIG_FILE, "scripts.clir.swahili.model.name");
		return new File (clirResourcesDir+File.separator+modelName);
	}
	
	public int getCLIRSwahiliPortNumber() {		
		return Integer.valueOf(props.getValue(CONFIG_FILE, "scripts.clir.swahili.port.number")).intValue();
	}
	
	public String getCLIRSwahiliIndexDB() {		
		return props.getValue(CONFIG_FILE, "scripts.clir.swahili.index.name");
	}
	
	//=========================================Summarizer========================================================================
	public String getCUSummarizerVersion() {
		return props.getValue(CONFIG_FILE, "scripts.summarizers.cu.docker.version");
	}
	public File getCUSummarizerStartServerScript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.summarizers.cu.startserver.script.name");
		return new File (summarizationResourcesDir+File.separator+scriptName);	
	}
	
	public File getCUSummarizerPackagerScript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.summarizers.cu.packager.script.name");
		return new File (summarizationResourcesDir+File.separator+scriptName);		
	}
	
	public File getCUSummarizeQueriesScript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.summarizers.cu.summarizequeries.script.name");
		return new File (summarizationResourcesDir+File.separator+scriptName);		
	}
	
	public int getCUSummarizerMaxConcurrentTasks() {	
//		int maxThreads=Runtime.getRuntime().availableProcessors()/2;
		int maxThreads=Runtime.getRuntime().availableProcessors();
		int userSpecifiedMaxThreads=Integer.valueOf(props.getValue(CONFIG_FILE, "scripts.summarizers.cu.max.concurrent.tasks")).intValue();
		return Math.min(maxThreads,userSpecifiedMaxThreads);
	}
	
	
	
	
	
	public String getEZsummaryVersion() {
		return props.getValue(CONFIG_FILE, "scripts.summarizers.ezsum.docker.version");
	}
	
	public String getRNNsummaryVersion() {
		return props.getValue(CONFIG_FILE, "scripts.summarizers.rnnsum.docker.version");
	}
	
	public String getRNNsummaryServerHostName() {
		return props.getValue(CONFIG_FILE, "scripts.summarizers.rnnsum.host.name");
	}
	
	public int getRNNsummaryPortNumber() {
		return Integer.valueOf(props.getValue(CONFIG_FILE, "scripts.summarizers.rnnsum.port.number")).intValue();
	}
	
	//=========================================ASR========================================================================	
	public String getASRVersion(Language lang) {
		if(asrVersionsMap.containsKey(lang.toString())) {
			return asrVersionsMap.get(lang.toString());
		}else {
			return null;
		}		
	}
	
	public ArrayList<Language> getASRSupportedLang() {
		return asrSupportedLanguages;
	}
	
	public ArrayList<String> getAllASRVersions(Language lang){
		ArrayList<String> asrVersions=new ArrayList<String>();
		String ver=getASRVersion(lang);
		if(ver!=null) {
			asrVersions.add(ver);
		}		
		return asrVersions;		
	}
	
	public File getASRTranscribeScript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.asr.transcribe.script.name");
		return new File (asrResourcesDir+File.separator+scriptName);
	}
	
	
	public String getASRSNbestGeneratorVersion() {
		return props.getValue(CONFIG_FILE, "scripts.asr.nbest.generator.docker.version");	
	}
	
	public int getASRNbestSize() {
		String nbest=props.getValue(CONFIG_FILE, "scripts.asr.nbest.size");
		return Integer.valueOf(nbest).intValue();
	}
	
	public File getASRSNbestGeneratorscript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.asr.nbest.generator.script.name");
		return new File (asrResourcesDir+File.separator+scriptName);		
	}
	//=========================================KWS========================================================================
	public String getKWSVersion(Language lang) {
		if(kwsVersionsMap.containsKey(lang.toString())) {
			return kwsVersionsMap.get(lang.toString());
		}else {
			return null;
		}		
	}
	
	public ArrayList<Language> getKWSSupportedLang() {
		return kwsSupportedLanguages;
	}
	
	public ArrayList<String> getAllKWSVersions(Language lang){
		ArrayList<String> kwsVersions=new ArrayList<String>();
		String ver=getKWSVersion(lang);
		if(ver!=null) {
			kwsVersions.add(ver);
		}		
		return kwsVersions;		
	}
	
	public File getKWSScript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.asr.kws.script.name");
		return new File (asrResourcesDir+File.separator+scriptName);
	}
	
	//=========================================ASR-Document-Expansion=====================================================
	public String getDocExpTagalogVersion() {
		return props.getValue(CONFIG_FILE, "scripts.asr.doc.exp.tagalog.docker.version");	
	}
	
	public String getDocExpSwahiliVersion() {
		return props.getValue(CONFIG_FILE, "scripts.asr.doc.exp.swahili.docker.version");	
	}
	
	public String getDocExpSomaliVersion() {
		return props.getValue(CONFIG_FILE, "scripts.asr.doc.exp.somali.docker.version");	
	}
	
	public File getDocExpScript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.asr.doc.exp.script.name");
		return new File (asrResourcesDir+File.separator+scriptName);
	}
	
	public ArrayList<Double> getDocExpThreshold() {
		String[]temps=props.getValue(CONFIG_FILE, "scripts.asr.doc.exp.threshold").split(";;");
		ArrayList<Double> thresholds=new ArrayList<Double>();
		for(String tmp:temps) {
			thresholds.add(Double.valueOf(tmp));
		}
		return thresholds;
	}
	
	public ArrayList<String> getAllDocExpTagalogVersions() {
		ArrayList<String> tagalogVersions=new ArrayList<String>();
		tagalogVersions.add(getDocExpTagalogVersion());
		return tagalogVersions;
	}
	
	public ArrayList<String> getAllDocExpSwahiliVersions() {
		ArrayList<String> swahiliVersions=new ArrayList<String>();
		swahiliVersions.add(getDocExpSwahiliVersion());
		return swahiliVersions;
	}
	
	public ArrayList<String> getAllDocExpSomaliVersions() {
		ArrayList<String> somaliVersions=new ArrayList<String>();
		somaliVersions.add(getDocExpSomaliVersion());
		return somaliVersions;
	}
	
	//=========================================MT=========================================================================
	public ArrayList<Language> getMTSupportedLang() {
		ArrayList<Language> supportedLangs = new ArrayList<Language>(mtSupportedLanguages);
		return supportedLangs;	
	}
	
	
	public String getEdiNMTVersion(Language lang, Type type) {
		String ver=null;
		if(ediNMTSupportedLanguagesAndTypes.containsKey(lang)) {
			ArrayList<Type> types=ediNMTSupportedLanguagesAndTypes.get(lang);
			if(types.contains(type)) {
				ver=props.getValue(CONFIG_FILE, "scripts.translator.nmt.docker.version");
			}
		}
		return ver;
	}
	
	public File getEdiNMTTranslateDirScript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.translator.nmt.translateDir.script.name");
		return new File (mtResourcesDir+File.separator+scriptName);		
	}
	
	public File getEdiNMTStartServerScript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.translator.nmt.startserver.script.name");
		return new File (mtResourcesDir+File.separator+scriptName);		
	}
	
	public File getEdiNMTStartServerWithNetworkScript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.translator.nmt.startserver.with.network.script.name");
		return new File (mtResourcesDir+File.separator+scriptName);		
	}
	
	public File getEdiNMTTranslateFileScript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.translator.nmt.translatefile.script.name");
		return new File (mtResourcesDir+File.separator+scriptName);		
	}
	
	public File getEdiNMTNbestSplitScript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.translator.nmt.split.nbest.script.name");
		return new File (mtResourcesDir+File.separator+scriptName);
	}
	
	public String getEdiNMTPortNumber(Language src, Language tgt) {
		String key="scripts.translator.nmt."+src.toString()+tgt.toString()+".port.number";
		return props.getValue(CONFIG_FILE, key);		
	}
	
	public String getUmdNMTVersion(Language lang, Type type) {
		String ver=null;
		if(umdNMTSupportedLanguagesAndTypes.containsKey(lang)) {
			ArrayList<Type> types=umdNMTSupportedLanguagesAndTypes.get(lang);
			if(types.contains(type)) {
				ver=props.getValue(CONFIG_FILE, "scripts.translator.umd.nmt.docker.version");
			}
		}
		return ver;
	}
	
	public File getUmdNMTTranslateDirScript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.translator.umd.nmt.translateDir.script.name");
		return new File (mtResourcesDir+File.separator+scriptName);		
	}
	
	public String getUmdSMTVersion(Language lang, Type type) {
		String ver=null;
		if(umdSMTSupportedLanguagesAndTypes.containsKey(lang)) {
			ArrayList<Type> types=umdSMTSupportedLanguagesAndTypes.get(lang);
			if(types.contains(type)) {
				ver=props.getValue(CONFIG_FILE, "scripts.translator.umd.smt.docker.version");
			}
		}
		return ver;
	}
	
	public File getUmdSMTTranslateDirScript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.translator.umd.smt.translateDir.script.name");
		return new File (mtResourcesDir+File.separator+scriptName);		
	}
	
	
	public String getUmdnnltmVersion(Language lang, Type type) {
		String ver=null;
		if(umdNNltmSupportedLanguagesAndTypes.containsKey(lang)) {
			ArrayList<Type> types=umdNNltmSupportedLanguagesAndTypes.get(lang);
			if(types.contains(type)) {
				ver=props.getValue(CONFIG_FILE, "scripts.translator.umd.nnltm.docker.version");
			}
		}
		return ver;
	}
	
	public File getUmdnnltmTranslateDirScript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.translator.umd.nnltm.translateDir.script.name");
		return new File (mtResourcesDir+File.separator+scriptName);		
	}
	
	public String getUmdNMTPsqVersion(Language lang, Type type) {
		String ver=null;
		if(umdNMTPsqSupportedLanguagesAndTypes.containsKey(lang)) {
			ArrayList<Type> types=umdNMTPsqSupportedLanguagesAndTypes.get(lang);
			if(types.contains(type)) {
				ver=props.getValue(CONFIG_FILE, "scripts.translator.umd.nmt.psq.docker.version");
			}
		}
		return ver;
	}
	
	public File getUmdNMTPsqTranslateDirScript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.translator.umd.nmt.psq.translateDir.script.name");
		return new File (mtResourcesDir+File.separator+scriptName);		
	}
	
	
	/**
	 * TODO: add all MT versions
	 * @return
	 */
	public ArrayList<String> getAllMTVersions(Language lang, Type type) {
		ArrayList<String> mtVersions=new ArrayList<String>();
		String ver=getEdiNMTVersion(lang, type);
		if(ver!=null) {
			mtVersions.add(ver);
		}
		ver=getUmdNMTVersion(lang, type);
		if(ver!=null) {
			mtVersions.add(ver);
		}
		ver=getUmdSMTVersion(lang, type);
		if(ver!=null) {
			mtVersions.add(ver);
		}
		ver=getUmdnnltmVersion(lang, type);
		if(ver!=null) {
			mtVersions.add(ver);
		}
		ver=getUmdNMTPsqVersion(lang, type);
		if(ver!=null) {
			mtVersions.add(ver);
		}
		return mtVersions;
	}
	//=========================================Morphological Analyzer=========================================================================
	public String getMorphAnalyzerVersion() {
		return props.getValue(CONFIG_FILE, "scripts.morph.analyzer.docker.version").trim();
	}
	
	public ArrayList<String> getAllMorphologyVersions(Language lang) {
		ArrayList<String> morphVersions=new ArrayList<String>();
		if(morphAnalyzerSupportedLanguages.contains(lang)) {
			morphVersions.add(getMorphAnalyzerVersion());
		}			
		return morphVersions;
	}	
	
	public File getMorphAnaslyzeDirScript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.morph.analyzer.analyzedir.script.name");
		return new File (morphAnalyzerResourcesDir+File.separator+scriptName);		
	}
	
	
	public ArrayList<Language> getMorphAnalyzerSupportedLang() {
		return morphAnalyzerSupportedLanguages;
	}
	
	
	//=========================================MT-Postedit===============================================================================
	public String getMtPosteditVersion() {
		return props.getValue(CONFIG_FILE, "scripts.mt.postedit.docker.version").trim();
	}
	
	public File getMtPosteditScript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.mt.postedit.script.name");
		return new File (mtPosteditResourcesDir+File.separator+scriptName);		
	}
	
	public ArrayList<String> getAllMtPosteditVersions() {
		ArrayList<String> mtPosteditVersions=new ArrayList<String>();
		mtPosteditVersions.add(getMtPosteditVersion());
		return mtPosteditVersions;
	}
	//=========================================Stemmer===================================================================================
	public String getStemmerVersion() {
		return props.getValue(CONFIG_FILE, "scripts.stemmer.docker.version").trim();
	}
	
	public File getStemDirScript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.stemmer.stemdir.script.name");
		return new File (stemmerResourcesDir+File.separator+scriptName);		
	}
	
	public ArrayList<String> getAllStemmerVersions() {
		ArrayList<String> stemmerVersions=new ArrayList<String>();
		stemmerVersions.add(getStemmerVersion());
		return stemmerVersions;
	}
	
	public String getStemmerNgrams() {
		return props.getValue(CONFIG_FILE, "scripts.stemmer.ngrams").trim();
	}
	
	//=========================================Sentence Splitter=========================================================================
	public String getSentenceSplitterVersion() {
		return props.getValue(CONFIG_FILE, "scripts.utils.sentence.splitter.version").trim();
	}
	
	public File getSplitSentScript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.utils.sentence.splitter.script.name");
		
		String scriptFolder = null;
		/**
		 * Make sure that the script has execute permission
		 */	
		try {
			scriptFolder=getSplitSentScriptDir().getCanonicalPath();
			ProcessBuilder processBuilder = new ProcessBuilder("chmod","-R","774",scriptFolder);
			Process process = processBuilder.start();
			process.waitFor();
		} catch (IOException | InterruptedException e) {
			logger.fatal(e.getMessage());
		}
		return new File (scriptFolder+File.separator+scriptName);
	}
	public File getSplitSentScriptDir() {
		String sentSplitLocation=props.getValue(CONFIG_FILE, "scripts.utils.sentence.splitter.path");
		String version=getSentenceSplitterVersion();
		
		String scriptFolder=utilsResourcesDir+File.separator+sentSplitLocation+File.separator+version;
		return new File(scriptFolder);
	}
	public ArrayList<String> getAllSentenceSplitterVersions() {
		ArrayList<String> sentSplitterVersions=new ArrayList<String>();
		sentSplitterVersions.add(getSentenceSplitterVersion());
		return sentSplitterVersions;
	}
	//=========================================Audio Sentence Splitter=========================================================================
	public String getAudSentenceSplitterVersion() {
		return props.getValue(CONFIG_FILE, "scripts.utils.audio.sentence.splitter.version").trim();
	}
	
	public File getSplitAudSentScript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.utils.audio.sentence.splitter.script.name");
		return new File (utilsResourcesDir+File.separator+scriptName);		
	}
	
	public ArrayList<String> getAllAudioSentenceSplitterVersions(Language lang) {
		ArrayList<String> audSentSplitterVersions=new ArrayList<String>();
		if(audioSentSplitterSupportedLanguages.contains(lang)) {
			audSentSplitterVersions.add(getAudSentenceSplitterVersion());
		}			
		return audSentSplitterVersions;
	}	
	
	//=========================================Domain Identification=========================================================================
	public String getDomainIdVersion() {
		return props.getValue(CONFIG_FILE, "scripts.domain.identifcation.docker.version");
	}
	
	public File getDomainIdScript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.domain.identifcation.script.name");
		return new File (identificationResourcesDir+File.separator+scriptName);		
	}
	
	public ArrayList<Language> getdomIDSupportedLang() {
		return domIDSupportedLanguages;
	}
	
	
	/**
	 * TODO: add all domain identification versions
	 * @return
	 */
	public ArrayList<String> getAllDomainIdVersions(Language lang) {
		ArrayList<String> domainIdVersions=new ArrayList<String>();
		if(domIDSupportedLanguages.contains(lang)) {
			domainIdVersions.add(getDomainIdVersion());
		}
		return domainIdVersions;
	}
	
	//=========================================Language Identification=========================================================================
	public String getLangIdVersion() {
		return props.getValue(CONFIG_FILE, "scripts.language.identifcation.docker.version");
	}
	
	public File getLangIdScript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.language.identifcation.script.name");
		return new File (identificationResourcesDir+File.separator+scriptName);		
	}
	
	public ArrayList<Language> getlangIDSupportedLang() {
		return langIDSupportedLanguages;
	}
	
	/**
	 * TODO: add all language identification versions
	 * @return
	 */
	public ArrayList<String> getAllLangIdVersions(Language lang) {
		ArrayList<String> langIdVersions=new ArrayList<String>();
		if(langIDSupportedLanguages.contains(lang)) {
			langIdVersions.add(getLangIdVersion());
		}		
		return langIdVersions;
	}	
	
	//=========================================Audio Language Identification======================================================================
	public File getAudioLangIdScript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.audio.language.identifcation.script.name");
		return new File (identificationResourcesDir+File.separator+scriptName);		
	}
	
	public String getSWAudioLangIdVersion() {
		return props.getValue(CONFIG_FILE, "scripts.audio.language.identifcation.swahili.docker.version");
	}
	
	public String getTLAudioLangIdVersion() {
		return props.getValue(CONFIG_FILE, "scripts.audio.language.identifcation.tagalog.docker.version");
	}
	
	public String getSOAudioLangIdVersion() {
		return props.getValue(CONFIG_FILE, "scripts.audio.language.identifcation.somali.docker.version");
	}
	
	public ArrayList<Double> getSWNBAudioLangIdThrsholds() {
		String[]temps=props.getValue(CONFIG_FILE, "scripts.audio.language.identifcation.swahili.nb.threshold").split(";;");
		ArrayList<Double> thresholds=new ArrayList<Double>();
		for(String tmp:temps) {
			thresholds.add(Double.valueOf(tmp));
		}
		return thresholds;
	}
	
	public ArrayList<Double> getTLNBAudioLangIdThrsholds() {
		String[]temps=props.getValue(CONFIG_FILE, "scripts.audio.language.identifcation.tagalog.nb.threshold").split(";;");
		ArrayList<Double> thresholds=new ArrayList<Double>();
		for(String tmp:temps) {
			thresholds.add(Double.valueOf(tmp));
		}
		return thresholds;
	}
	public ArrayList<Double> getSONBAudioLangIdThrsholds() {
		String[]temps=props.getValue(CONFIG_FILE, "scripts.audio.language.identifcation.somali.nb.threshold").split(";;");
		ArrayList<Double> thresholds=new ArrayList<Double>();
		for(String tmp:temps) {
			thresholds.add(Double.valueOf(tmp));
		}
		return thresholds;
	}
	
	public ArrayList<Double> getSWWBAudioLangIdThrsholds() {
		String[]temps=props.getValue(CONFIG_FILE, "scripts.audio.language.identifcation.swahili.wb.threshold").split(";;");
		ArrayList<Double> thresholds=new ArrayList<Double>();
		for(String tmp:temps) {
			thresholds.add(Double.valueOf(tmp));
		}
		return thresholds;
	}
	
	public ArrayList<Double> getTLWBAudioLangIdThrsholds() {
		String[]temps=props.getValue(CONFIG_FILE, "scripts.audio.language.identifcation.tagalog.wb.threshold").split(";;");
		ArrayList<Double> thresholds=new ArrayList<Double>();
		for(String tmp:temps) {
			thresholds.add(Double.valueOf(tmp));
		}
		return thresholds;
	}
	public ArrayList<Double> getSOWBAudioLangIdThrsholds() {
		String[]temps=props.getValue(CONFIG_FILE, "scripts.audio.language.identifcation.somali.wb.threshold").split(";;");
		ArrayList<Double> thresholds=new ArrayList<Double>();
		for(String tmp:temps) {
			thresholds.add(Double.valueOf(tmp));
		}
		return thresholds;
	}
	
	
	public ArrayList<String> getAllSWAudioLangIdVersions() {
		ArrayList<String> swahiliVersions=new ArrayList<String>();
		swahiliVersions.add(getSWAudioLangIdVersion());
		return swahiliVersions;
	}
	
	public ArrayList<String> getAllTLAudioLangIdVersions() {
		ArrayList<String> tagalogVersions=new ArrayList<String>();
		tagalogVersions.add(getTLAudioLangIdVersion());
		return tagalogVersions;
	}
	
	
	public ArrayList<String> getAllSOAudioLangIdVersions() {
		ArrayList<String> somaliVersions=new ArrayList<String>();
		somaliVersions.add(getSOAudioLangIdVersion());
		return somaliVersions;
	}
	
	//=========================================Evaluator=========================================================================
	public String getEvaluatorVersion() {
		return props.getValue(CONFIG_FILE, "scripts.utils.evaluator.version").trim();
	}
	public File getEvaluatorScript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.utils.evaluator.script.name");
		return new File (utilsResourcesDir+File.separator+scriptName);		
	}
	//=========================================CLIR-Packager=========================================================================
	public String getCLIRPackagerVersion() {
		return props.getValue(CONFIG_FILE, "scripts.utils.clir.packager.version").trim();
	}
	public File getCLIRPackagerScript() {
		String scriptName=props.getValue(CONFIG_FILE, "scripts.utils.clir.packager.script.name");
		return new File (utilsResourcesDir+File.separator+scriptName);		
	}	
	//=========================================Testing mode=========================================================================
	public ArrayList<File> getTestConfigFile() {
		String[] tmp=props.getValue(CONFIG_FILE, "scripts.testing.config").trim().split(";;");
		ArrayList<File> configs=new ArrayList<File>();
		for(String conf:tmp) {
			configs.add(new File (testingResourcesDir+File.separator+conf));
		}		
		return configs;		
	}	
	public String getTestDataRootAbsPath() {
		return testingResourcesDir;		
	}	
	public String getTestDataRelativePath() {
		return props.getValue(CONFIG_FILE, "scripts.testing.data.path").trim();		
	}
}
