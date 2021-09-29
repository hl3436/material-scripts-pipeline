/**
 * 
 */
package edu.columbia.dsi.containers;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;

import org.apache.log4j.Logger;

import edu.columbia.dsi.SCRIPTS.Language;
import edu.columbia.dsi.containers.Component.CompNames;
import edu.columbia.dsi.containers.DocInfo.Type;
import edu.columbia.dsi.utils.ResourceFactory;

/**
 * @author badrashiny
 * Sep 5, 2019
 */
public class Corpus {
	private static Logger logger = Logger.getLogger(Corpus.class.getSimpleName());
	private ResourceFactory resourceFactory = new ResourceFactory();
	
	private File inputSourceLocation; //this is the source entered by the user. It could be text or audio
	private Type type;
	private Language lang;
	private String corpusName;
	private String sourceFilesLocation;
	private File metaDataPath=null;
	private String manualTranslationLocation=null;
	private String manualTranscriptionLocation=null;
	
	private File indexOutLocation;
	private String indexDirName="index_store";
	
	private Component asr=null;
	private Component kws=null;
	private Component docExp=null;
	private Component sentSplitter=null;
	private Component audioSentSplitter=null;
	private Component langId=null;
	private Component audioLangId=null;
	private Component mt=null;
	private Component morphology=null;
	private Component domainId=null;
	private Component stemmer=null;
	private Component postedit=null;
	
	private Hashtable<CompNames,Component> pipelineComponents=new Hashtable<CompNames,Component>();
	

	private String toolsVerSeparator=";;";	
	
	/**
	 * EX: /storage/data/NIST-data/1B/IARPA_MATERIAL_BASE-1B/ANALYSIS1/text/src
	 * absolutePathLocation=/storage/data
	 * relativeDirectory=NIST-data
	 * corpusName=1B/IARPA_MATERIAL_BASE-1B/ANALYSIS1
	 * sourceLocation=text/src
	 */
	public Corpus(String absolutePathLocation, String relativeDirectory, String corpusName, String sourceFilesLocation, Type type, Language lang, String metaDataPath, String manualTranslationLocation, String manualTranscriptionLocation) {
		initialize(absolutePathLocation, relativeDirectory, corpusName, sourceFilesLocation, type, lang, metaDataPath, manualTranslationLocation, manualTranscriptionLocation);
		
		Component mtParent=null;
		Component morphParent=null;
		if(type==Type.audio) {
			asr.setParent(null, null);
			asr.build("", true, null, null);
			if(this.manualTranscriptionLocation!=null) {
				asr.addManualVer(this.manualTranscriptionLocation, "iarpa-manual", this.sourceFilesLocation);
			}			
				
			docExp.setParent(asr, lang);
			docExp.build(resourceFactory.getUmdQueryProcessorVersion().replaceAll("[/_:]", "-"), false, null, null);
			
			audioLangId.setParent(asr, lang);
			audioLangId.build("", false, null, null);
			
			audioSentSplitter.setParent(asr, lang);
			ArrayList<String> asrFilter= new ArrayList<String>();
			String asrVerTmp=resourceFactory.getASRVersion(lang);
			if(asrVerTmp!=null) {
				asrFilter.add(asrVerTmp);// the audio sentence splitter and kws run on the ASR output only. We don't want it to run on the manual transcription
			}
			audioSentSplitter.build("", true, asrFilter, null);	
			
			kws.setParent(asr, lang);
			ArrayList<String> asrKwsFilter= new ArrayList<String>();
			String kwsVerTmp=resourceFactory.getASRVersion(lang);
			if(kwsVerTmp!=null) {
				asrKwsFilter.add(kwsVerTmp);// the audio kws run on the ASR output only. We don't want it to run on the manual transcription
			}
			kws.build("", true, asrKwsFilter, null);
		
			if(audioSentSplitter.getVerToOutDirName().isEmpty()) {//i.e. the audio sentence splitter doesn't support this language 
				mtParent=asr;
				morphParent=asr;
			}else {
				mtParent=audioSentSplitter;
				morphParent=audioSentSplitter;
			}			
			
		}else if(type==Type.text) {
			sentSplitter.setParent(null, lang);
			sentSplitter.build("", true, null, null);
			
			langId.setParent(sentSplitter, lang);
			langId.build("", true, null, null);	
			
			mtParent=sentSplitter;
			if(langId.getVerToOutDirName().isEmpty()) {//i.e. the LanguageID doesn't support this language 
				morphParent=sentSplitter;
			}else {
				morphParent=langId;
			}			
		}
		
		mt.setParent(mtParent, lang);
		mt.build("", true, null, null);
//		if(type==Type.audio) {
//			mt.setParent(audioSentSplitter, lang);
//			ArrayList<String> mtFilter= new ArrayList<String>();// Only UMD-nmt and Edi-nmt systems can run on the audio sentence split output
//			String tmp=resourceFactory.getUmdNMTVersion(lang, type);
//			if(tmp!=null) {
//				mtFilter.add(tmp);
//			}
//			tmp=resourceFactory.getEdiNMTVersion(lang, type);
//			if(tmp!=null) {
//				mtFilter.add(tmp);
//			}
//			mt.build("", true, null, mtFilter);
//		}	
		
		morphology.setParent(morphParent, lang);//to analyze the foreign language 
		morphology.build("", true, null, null);
		morphology.setParent(mt, Language.en); // to analyze the English translation
		// the morphology component should be used only with the UMD-NMT, UMD-SMT, EDI-NMT, and UMD-NMT-PSQ systems
		ArrayList<String> morphFilter= new ArrayList<String>();
		String morphVerTmp=resourceFactory.getUmdNMTVersion(lang, type);
		if(morphVerTmp!=null) {
			morphFilter.add(morphVerTmp);
		}
		morphVerTmp=resourceFactory.getUmdSMTVersion(lang, type);
		if(morphVerTmp!=null) {
			morphFilter.add(morphVerTmp);
		}
		morphVerTmp=resourceFactory.getEdiNMTVersion(lang, type);
		if(morphVerTmp!=null) {
			morphFilter.add(morphVerTmp);
		}
		morphVerTmp=resourceFactory.getUmdNMTPsqVersion(lang, type);
		if(morphVerTmp!=null) {
			morphFilter.add(morphVerTmp);
		}
		morphology.build("", true, morphFilter, null);		
//		if(type==Type.audio) {
//			morphology.setParent(audioSentSplitter, lang);//to analyze the audio sentence split output of the foreign language 
//			morphology.build("", true, null, null);
//			
//		}
		
		
		postedit.setParent(mt, Language.en);
		ArrayList<String> mtPostEditFilter= new ArrayList<String>();
		// the MT-post edit component should be used only with the UMD-NMT, UMD-SMT, EDI-NMT systems
		String mtPostEditVerTmp=resourceFactory.getUmdNMTVersion(lang, type);
		if(mtPostEditVerTmp!=null) {
			mtPostEditFilter.add(mtPostEditVerTmp);
		}
		mtPostEditVerTmp=resourceFactory.getUmdSMTVersion(lang, type);
		if(mtPostEditVerTmp!=null) {
			mtPostEditFilter.add(mtPostEditVerTmp);
		}
		mtPostEditVerTmp=resourceFactory.getEdiNMTVersion(lang, type);
		if(mtPostEditVerTmp!=null) {
			mtPostEditFilter.add(mtPostEditVerTmp);
		}
//		mtPostEditFilter.add("No Version");// We want to block the MT-post edit for now. So we are sending a fake parent version that doesn't exist
		postedit.build("", true, mtPostEditFilter, null);	
			
		
		domainId.setParent(postedit, Language.en);
		ArrayList<String> domIdFilter= new ArrayList<String>();
		String domVerTmp=resourceFactory.getUmdNMTVersion(lang, type);
		if(domVerTmp!=null) {
			domIdFilter.add(domVerTmp);// the domain identification component should be used only with the UMD-NMT system
		}
		domainId.build("", true, domIdFilter, null);

		stemmer.setParent(postedit, Language.en);
		ArrayList<String> stemmerFilter= new ArrayList<String>();
		stemmerFilter.add("No Version"); // We want to block the stemmer for now. So we are sending a fake parent version that doesn't exist
		stemmer.build("", true, stemmerFilter, null);
	}
	
	private void initialize(String absolutePathLocation, String relativeDirectory, String corpusName, String sourceFilesLocation, Type type, Language lang, String metaDataPath, String manualTranslationLocation, String manualTranscriptionLocation) {
		this.type=type;
		this.lang=lang;	
		this.corpusName=corpusName;
		this.sourceFilesLocation=sourceFilesLocation;
		if(metaDataPath!=null) {
			this.metaDataPath=new File (metaDataPath);
		}	
		this.manualTranslationLocation=manualTranslationLocation;
		this.manualTranscriptionLocation=manualTranscriptionLocation;
		
		
		this.inputSourceLocation=new File(absolutePathLocation+File.separator+relativeDirectory+File.separator+this.corpusName+File.separator+this.sourceFilesLocation);
		logger.debug("Preparing corpus: "+this.inputSourceLocation.getAbsolutePath()+" ["+lang.toString()+"::"+type.toString()+"]");
		
		this.indexDirName=this.sourceFilesLocation.replaceAll(new File(this.sourceFilesLocation).getName()+"$", indexDirName);
		this.indexOutLocation=new File(absolutePathLocation+File.separator+relativeDirectory+File.separator+this.corpusName+File.separator+this.indexDirName);
		if (! this.indexOutLocation.exists()){
			this.indexOutLocation.mkdir();
	    }
		

		if(type==Type.audio) {
			asr=new Component(absolutePathLocation, relativeDirectory, this.corpusName, this.sourceFilesLocation, this.lang, this.type, CompNames.asr, toolsVerSeparator);
			pipelineComponents.put(CompNames.asr, asr);
			
			kws=new Component(absolutePathLocation, relativeDirectory, this.corpusName, this.sourceFilesLocation, this.lang, this.type, CompNames.kws, toolsVerSeparator);
			pipelineComponents.put(CompNames.kws, kws);
			
			
			docExp=new Component(absolutePathLocation, relativeDirectory, this.corpusName, this.sourceFilesLocation, this.lang, this.type, CompNames.docExp, toolsVerSeparator);
			pipelineComponents.put(CompNames.docExp, docExp);
			
			audioLangId=new Component(absolutePathLocation, relativeDirectory, this.corpusName, this.sourceFilesLocation, this.lang, this.type, CompNames.audLangId, toolsVerSeparator);		
			pipelineComponents.put(CompNames.audLangId, audioLangId);
			
			audioSentSplitter=new Component(absolutePathLocation, relativeDirectory, this.corpusName, this.sourceFilesLocation, this.lang, this.type, CompNames.audSentSp, toolsVerSeparator);
			pipelineComponents.put(CompNames.audSentSp, audioSentSplitter);
			
		}else if(type==Type.text) {
			sentSplitter=new Component(absolutePathLocation, relativeDirectory, this.corpusName, this.sourceFilesLocation, this.lang, this.type, CompNames.sentSp, toolsVerSeparator);
			pipelineComponents.put(CompNames.sentSp, sentSplitter);
			
			langId=new Component(absolutePathLocation, relativeDirectory, this.corpusName, this.sourceFilesLocation, this.lang, this.type, CompNames.langId, toolsVerSeparator);
			pipelineComponents.put(CompNames.langId, langId);
		}
		
		mt=new Component(absolutePathLocation, relativeDirectory, this.corpusName, this.sourceFilesLocation, this.lang, this.type, CompNames.mt, toolsVerSeparator);
		pipelineComponents.put(CompNames.mt, mt);
		
		morphology=new Component(absolutePathLocation, relativeDirectory, this.corpusName, this.sourceFilesLocation, this.lang, this.type, CompNames.morph, toolsVerSeparator);
		pipelineComponents.put(CompNames.morph, morphology);
		
		domainId=new Component(absolutePathLocation, relativeDirectory, this.corpusName, this.sourceFilesLocation, this.lang, this.type, CompNames.domId, toolsVerSeparator);
		pipelineComponents.put(CompNames.domId, domainId);
		
		stemmer=new Component(absolutePathLocation, relativeDirectory, this.corpusName, this.sourceFilesLocation, this.lang, this.type, CompNames.stmr, toolsVerSeparator);
		pipelineComponents.put(CompNames.stmr, stemmer);
		
		postedit=new Component(absolutePathLocation, relativeDirectory, this.corpusName, this.sourceFilesLocation, this.lang, this.type, CompNames.postEd, toolsVerSeparator);
		pipelineComponents.put(CompNames.postEd, postedit);
	}
	
	public Type getType() {
		return this.type;
	}
	
	public Language getLanguage() {
		return this.lang;
	}
	
	public File getSourceLocation() {
		return this.inputSourceLocation;
	}
	
	public String getIndexLocation() {
		return this.indexDirName;
	}
	
	public String getCorpusName() {
		return this.corpusName;
	}
	
	public File getMetaDataPath() {
		return this.metaDataPath;
	}
	
	public String getSourceFilesLocation() {
		return this.sourceFilesLocation;
	}
	
	public String getManualTranslationLocation() {;
		return this.manualTranslationLocation;
	}
	
	public String getManualTranscriptionLocation() {;
		return this.manualTranscriptionLocation;
	}
	
	public String getToolsVerSeparator() {
		return toolsVerSeparator;
	}
	
	public Component getComponent(CompNames name) {
		if(pipelineComponents.containsKey(name)) {
			return pipelineComponents.get(name);
		}else {
			return null;
		}		
	}
	
	
	
	
//	public File getAsrAbsLocation(String asrVersion) {
//		return asr.getAbsLocation(asrVersion);
//	}
//	
//	public String getAsrShortLocation(String asrVersion) {
//		return asr.getShortLocation(asrVersion);
//	}
//	
//	
//	public File getDocExpAbsLocation(String docExpVersion) {
//		return docExp.getAbsLocation(docExpVersion);
//	}
//	
//	public String getDocExpShortLocation(String docExpVersion) {
//		return docExp.getShortLocation(docExpVersion);
//	}
//	
//	public File getSentSplitterAbsLocation(String sentSplitterVersion) {
//		return sentSplitter.getAbsLocation(sentSplitterVersion);
//	}
//	
//	public String getSentSplitterShortLocation(String sentSplitterVersion) {
//		return sentSplitter.getShortLocation(sentSplitterVersion);
//	}
//	
//	public File getLangIdAbsLocation(String langIdVersion) {
//		return langId.getAbsLocation(langIdVersion);
//	}
//	
//	public String getLangIdShortLocation(String langIdVersion) {
//		return langId.getShortLocation(langIdVersion);
//	}
//	
//	public File getAudioLangIdAbsLocation(String audioLangIdVersion) {
//		return audioLangId.getAbsLocation(audioLangIdVersion);
//	}
//	
//	public String getAudioLangIdShortLocation(String audioLangIdVersion) {
//		return audioLangId.getShortLocation(audioLangIdVersion);
//	}
//	
//	public File getMorphAbsLocation(String morphAnalyzerVersion) {
//		return morphology.getAbsLocation(morphAnalyzerVersion);
//	}
//
//	public File getMTAbsLocation(String mtVersion) {
//		return mt.getAbsLocation(mtVersion);
//	}
//	
//	public String getMTShortLocation(String mtVersion) {
//		return mt.getShortLocation(mtVersion);
//	}
//	
//	public File getDomainIdAbsLocation(String domainIdVersion) {
//		return domainId.getAbsLocation(domainIdVersion);
//	}
//	
//	public String getDomainIdShortLocation(String domainIdVersion) {
//		return domainId.getShortLocation(domainIdVersion);
//	}
//	
//	public File getStemmerAbsLocation(String stemmerVersion) {
//		return stemmer.getAbsLocation(stemmerVersion);
//	}
//	
//	public String getStemmerShortLocation(String stemmerVersion) {
//		return stemmer.getShortLocation(stemmerVersion);
//	}
//	
//	public File getPosteditAbsLocation(String posteditVersion) {
//		return postedit.getAbsLocation(posteditVersion);
//	}
//	
//	public String getPosteditShortLocation(String posteditVersion) {
//		return postedit.getShortLocation(posteditVersion);
//	}
//	
//	
//	
//	
//	
//	
//	
//	public Hashtable<String, String> getAsrVerToShortLocationMap() {
//		return asr.getVerToShortLocationMap();
//	}
//	
//	public Hashtable<String, String> getDocExpVerToShortLocationMap() {
//		return docExp.getVerToShortLocationMap();
//	}
//	
//	public Hashtable<String, String> getSentSplitterVerToShortLocationMap() {
//		return sentSplitter.getVerToShortLocationMap();
//	}
//	
//	public Hashtable<String, String> getLangIdVerToShortLocationMap() {
//		return langId.getVerToShortLocationMap();
//	}
//	
//	public Hashtable<String, String> getAudioLangIdVerToShortLocationMap() {
//		return audioLangId.getVerToShortLocationMap();
//	}
//	
//	public Hashtable<String, String> getMorphologyVerToShortLocationMap() {
//		return morphology.getVerToShortLocationMap();
//	}
//	
//	public Hashtable<String, String> getMtVerToShortLocationMap() {
//		return mt.getVerToShortLocationMap();
//	}	
//	
//	public Hashtable<String, String> getDomainIdVerToShortLocationMap() {
//		return domainId.getVerToShortLocationMap();
//	}	
//	
//	public Hashtable<String, String> getStemmerVerToShortLocationMap() {
//		return stemmer.getVerToShortLocationMap();
//	}
//	
//	public Hashtable<String, String> getPosteditVerToShortLocationMap() {
//		return postedit.getVerToShortLocationMap();
//	}
}
