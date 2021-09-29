/**
 * 
 */
package edu.columbia.dsi.containers;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import edu.columbia.dsi.SCRIPTS.Language;
import edu.columbia.dsi.containers.DocInfo.Type;
import edu.columbia.dsi.utils.ResourceFactory;

/**
 * @author badrashiny
 * Sep 5, 2019
 */
public class Component {
	public enum CompNames {asr, kws, docExp, sentSp, audSentSp, langId, audLangId, mt, morph, domId, stmr, postEd;}
	private static Logger logger = Logger.getLogger(Component.class.getSimpleName());
	private ResourceFactory resourceFactory = new ResourceFactory();
	
	private String absolutePathLocation;
	private String relativeDirectory;
	private String corpusName;
	private File inputSourceLocation;
	private String sourceFilesLocation;
	private Language corpusLang;
	private Type corpusType;
	private ArrayList<String>versions=new ArrayList<String>();
	private Hashtable<String,String> verToOutShortLocationMap=new  Hashtable<String,String>();
	private Hashtable<String,String> verToSourceShortLocationMap=new  Hashtable<String,String>();
	private Hashtable<String,String> verToOutDirName=new Hashtable<String,String>();
	private Hashtable<String,File> verToOutDirPathMap=new  Hashtable<String,File>();
	private Hashtable<String,File> verToSourceDirPathMap=new  Hashtable<String,File>();
	private Hashtable<String,String> verToParentNamehMap=new  Hashtable<String,String>();
	private Hashtable<String,Language> verToSourceLanguageMap=new  Hashtable<String,Language>();
	
	private File outLocation=null; 
	private String dirName;
	
	private CompNames name=null;
	
	private Component parent=null;
	private Language parentLang=null;
	
	private Hashtable<CompNames,String> compnameTodirname=getNameToDir();
	private String toolsVerSeparator=";;";

	/**
	 * EX: /storage/data/NIST-data/1B/IARPA_MATERIAL_BASE-1B/ANALYSIS1/text/src
	 * absolutePathLocation=/storage/data
	 * relativeDirectory=NIST-data
	 * corpusName=1B/IARPA_MATERIAL_BASE-1B/ANALYSIS1
	 * sourceLocation=text/src
	 */
	public Component(String absolutePathLocation, String relativeDirectory, String corpusName, String sourceFilesLocation, Language lang, Type type, CompNames name, String toolsVerSeparator) {
		this.absolutePathLocation=absolutePathLocation;
		this.relativeDirectory=relativeDirectory;
		this.corpusName=corpusName;
		this.toolsVerSeparator=toolsVerSeparator;
		this.corpusLang=lang;
		this.corpusType=type;
		this.sourceFilesLocation=sourceFilesLocation;
		this.name=name;
		String drName=null;
		if(compnameTodirname.containsKey(name)) {
			drName=compnameTodirname.get(name);
		}else {
			System.out.println("Unidentified component: "+ name.toString());
			logger.fatal("Unidentified component: "+ name.toString());
			System.exit(0);
		}
		this.dirName=sourceFilesLocation.replaceAll(new File(this.sourceFilesLocation).getName()+"$", drName);
		this.outLocation=new File(absolutePathLocation+File.separator+relativeDirectory+File.separator+corpusName+File.separator+this.dirName);		
		this.inputSourceLocation=new File(this.absolutePathLocation+File.separator+this.relativeDirectory+File.separator+this.corpusName+File.separator+sourceFilesLocation);
		
		if (! this.outLocation.exists()){
			this.outLocation.mkdir();
	    }
		this.versions.addAll(fillVersions(this.name, this.corpusLang, this.corpusType));		
	}
	/**
	 * 
	 * @param parent
	 * @param lang: the language that the parent speaks. i.e. the output language from the parent component
	 */
	public void setParent(Component parent, Language lang) {
		this.parent=parent;
		this.parentLang=lang;
	}
	
//	/**
//	 * Some components could takes inputs from 2 parents. EX: the morphological analyzer, takes the output of the sentence splitter/ASR in addition to the output of the MT
//	 * In this case the sentence splitter/ASR is the parent and the MT is the foster parent 
//	 * @param fosterParent
//	 * @param lang: the language that the foster parent speaks. i.e. the output language from the parent component
//	 */
//	public void setFosterParent(Component fosterParent, Language lang) {
//		this.fosterParent=fosterParent;
//		this.fosterParentLang=lang;
//	}
	
//	public Component getParent() {
//		return this.parent;
//	}
	
//	public Component getFosterParent() {
//		return this.fosterParent;
//	}
	
//	public Language getParentLang() {
//		return this.parentLang;
//	}
	
//	public Language getFosterParentLang() {
//		return this.fosterParentLang;
//	}
	
	/**
	 * 
	 * @param outDirSuffix: if you want to add a fixed suffix to the output directory name 
	 * @param creatOutDir: create the output directory if true. 
	 * @param parentFilter: This is an inclusive filter. if not null, use only the  parent versions that are in the filter. All other parent versions are ignored
	 * @param childFilter: This is an inclusive filter. if not null, use only the  child versions that are in the filter. All other child versions are ignored
	 */
	public void build(String outDirSuffix, boolean creatOutDir, ArrayList<String> parentFilter, ArrayList<String> childFilter) {
		Hashtable<String,String>parentVerToOutDir=null;
		String parentName="null";
		if(parent!=null) {
			parentVerToOutDir=parent.getVerToOutDirName();
			parentName=parent.getName().toString();
		}
		ArrayList<String> childVersions=applyFilter(versions, childFilter);
		if(parentVerToOutDir==null) {//i.e. no parent. So the parent is the inputSourceLocation			
			for(String ver:childVersions) {
				String outDirName=ver.replaceAll("[/_:]", "-");
				if(outDirSuffix!=null && !outDirSuffix.trim().isEmpty()) {
					outDirName=outDirName+"-"+outDirSuffix.trim();
				}
				verToOutShortLocationMap.put(ver, dirName+File.separator+outDirName);
				verToSourceShortLocationMap.put(ver, this.sourceFilesLocation);
				File verOutPath=new File(absolutePathLocation+File.separator+relativeDirectory+File.separator+this.corpusName+File.separator+this.dirName+File.separator+outDirName);
				if (creatOutDir && !verOutPath.exists()){
					verOutPath.mkdir();
			    }
				verToOutDirPathMap.put(ver, verOutPath);
				verToSourceDirPathMap.put(ver, this.inputSourceLocation);
				verToSourceLanguageMap.put(ver, this.corpusLang);
				verToOutDirName.put(ver, outDirName);
				verToParentNamehMap.put(ver, parentName);
			}
		}else {
			ArrayList<String> tmp = new ArrayList<String>();
			tmp.addAll(parentVerToOutDir.keySet());
			ArrayList<String> parentVersions=applyFilter(tmp, parentFilter);
			for(String baseVer:childVersions) {
				String baseOutDirName=baseVer.replaceAll("[/_:]", "-");
				for(String parentVer:parentVersions) {
					String outDirName=baseOutDirName+"_"+parentVerToOutDir.get(parentVer);
					if(outDirSuffix!=null && !outDirSuffix.trim().isEmpty()) {
						outDirName=outDirName+"-"+outDirSuffix.trim();
					}
					String ver=baseVer+toolsVerSeparator+parentVer;
					
					File verSourcePath=parent.getOutDirectoryPath(parentVer);
					String verSourceShortLocation=parent.getOutShortLocation(parentVer);
					verToOutShortLocationMap.put(ver, dirName+File.separator+outDirName);
					verToSourceShortLocationMap.put(ver, verSourceShortLocation);
					File verOutPath=new File(absolutePathLocation+File.separator+relativeDirectory+File.separator+this.corpusName+File.separator+this.dirName+File.separator+outDirName);
					if (creatOutDir && !verOutPath.exists()){
						verOutPath.mkdir();
				    }
					
					verToOutDirPathMap.put(ver, verOutPath);
					verToSourceDirPathMap.put(ver, verSourcePath);
					verToSourceLanguageMap.put(ver, parentLang);
					verToOutDirName.put(ver, outDirName);
					verToParentNamehMap.put(ver, parentName);
				}						
			}	
		}		
	}
	
	/**
	 * This is inclusive filtering.  if the filter is not null, it uses only the  parent versions that are in the filter. All other versions are ignored
	 * @param ver
	 * @param filter
	 * @return
	 */
	private ArrayList<String> applyFilter(ArrayList<String> ver, ArrayList<String> filter){
		ArrayList<String> retVer=new ArrayList<String>();
		for(String v:ver) {
			if(filter!=null && filter.size()>0 ){
				for(String f:filter) {
					if(v.contains(f)) {
						retVer.add(v);
						break;
					}
				}
			}else {
				retVer.add(v);
			}
		}	
		return retVer;		
	}		
	
	public void addManualVer(String manualOutLocation, String ver, String sourceLocation ) {
		File manualOutPath=new File(absolutePathLocation+File.separator+relativeDirectory+File.separator+this.corpusName+File.separator+manualOutLocation);
		
		if(manualOutPath.exists()) {
			versions.add(ver);
			String outDirName=new File(manualOutLocation).getName();
			verToOutShortLocationMap.put(ver, manualOutLocation);
			verToSourceShortLocationMap.put(ver, sourceLocation);
			verToOutDirPathMap.put(ver, manualOutPath);
			verToOutDirName.put(ver, outDirName);
			verToParentNamehMap.put(ver, "null");
		}	
	}
	
	public CompNames getName() {
		return this.name;
	}
	
	public Hashtable<String,String> getVerToShortLocationMap(){
		return this.verToOutShortLocationMap;
	}
	
	public Hashtable<String,String> getVerToShortLocationMap(String version) {
		Hashtable<String,String> filteredVerToOutShortLocationMap= new Hashtable<String,String>();
		for(Entry<String, String> e:this.verToOutShortLocationMap.entrySet()) {		
			String[]toolsVersions=e.getKey().split(toolsVerSeparator,2);
			if(version!=null && version.equals(toolsVersions[0])) {
				filteredVerToOutShortLocationMap.put(e.getKey(), e.getValue());
			}
		}
		return filteredVerToOutShortLocationMap;
	}
	
	public CompNames getParentName(String ver){
		if(this.verToParentNamehMap.containsKey(ver) && !this.verToParentNamehMap.get(ver).equals("null")) {
			return CompNames.valueOf(this.verToParentNamehMap.get(ver));
		}else {
			return null;
		}
	}
	
	public String getOutShortLocation(String ver){
		if(this.verToOutShortLocationMap.containsKey(ver)) {
			return this.verToOutShortLocationMap.get(ver);
		}else {
			return null;
		}
	}
	
	public String getSourceShortLocation(String ver){
		if(this.verToSourceShortLocationMap.containsKey(ver)) {
			return this.verToSourceShortLocationMap.get(ver);
		}else {
			return null;
		}
	}
	
	public Hashtable<String,String> getVerToOutDirName(){
		return this.verToOutDirName;
	}
	
//	public Hashtable<String,File> getVerToAbsLocationMap(){
//		return this.verToOutDirPathMap;
//	}
	
	public File getOutDirectoryPath(String ver) {
		if(this.verToOutDirPathMap.containsKey(ver)) {
			return this.verToOutDirPathMap.get(ver);
		}else {
			return null;
		}
	}
	
	public File getSourceDirectoryPath(String ver) {
		if(this.verToSourceDirPathMap.containsKey(ver)) {
			return this.verToSourceDirPathMap.get(ver);
		}else {
			return null;
		}
	}
	
	public Language getSourceDirectoryLanguage(String ver) {
		if(this.verToSourceLanguageMap.containsKey(ver)) {
			return this.verToSourceLanguageMap.get(ver);
		}else {
			return null;
		}
	}
	
	
	private Hashtable<CompNames,String> getNameToDir(){
		Hashtable<CompNames,String> cTd=new Hashtable<CompNames,String>();
		cTd.put(CompNames.asr, "asr_store");
		cTd.put(CompNames.kws, "kws_store");
		cTd.put(CompNames.docExp, "asr_document_expansion_store");
		cTd.put(CompNames.sentSp, "sentSplitter_store");
		cTd.put(CompNames.audSentSp, "sentSplitter_store");
		cTd.put(CompNames.langId, "languageIdentification_store");
		cTd.put(CompNames.audLangId, "languageIdentification_store");
		cTd.put(CompNames.mt, "mt_store");
		cTd.put(CompNames.morph, "morphology_store");
		cTd.put(CompNames.domId, "domainIdentification_store");
		cTd.put(CompNames.stmr, "stemmer_store");
		cTd.put(CompNames.postEd, "mt_postedit_store");
		return cTd;
	}
	private ArrayList<String> fillVersions(CompNames name, Language lang, Type type){
		ArrayList<String> ver =new ArrayList<String>();
		if(name==CompNames.asr) {
			ver.addAll(resourceFactory.getAllASRVersions(lang));
		}else if(name==CompNames.kws) {
			ver.addAll(resourceFactory.getAllKWSVersions(lang));
		}else if(name==CompNames.docExp) {
			// if(lang==Language.sw) {
			// 	ver.addAll(resourceFactory.getAllDocExpSwahiliVersions());
			// }else if(lang==Language.tl) {
			// 	ver.addAll(resourceFactory.getAllDocExpTagalogVersions());
			// }else if(lang==Language.so) {
			// 	ver.addAll(resourceFactory.getAllDocExpSomaliVersions());
			// }
		}else if(name==CompNames.sentSp) {
			ver.addAll(resourceFactory.getAllSentenceSplitterVersions());
		}else if(name==CompNames.audSentSp) {
			ver.addAll(resourceFactory.getAllAudioSentenceSplitterVersions(lang));
		}else if(name==CompNames.langId) {
			ver.addAll(resourceFactory.getAllLangIdVersions(lang));
		}else if(name==CompNames.audLangId) {
			// if(lang==Language.sw) {
			// 	ver.addAll(resourceFactory.getAllSWAudioLangIdVersions());
			// }else if(lang==Language.tl) {
			// 	ver.addAll(resourceFactory.getAllTLAudioLangIdVersions());
			// }else if(lang==Language.so) {
			// 	ver.addAll(resourceFactory.getAllSOAudioLangIdVersions());
			// }
		}else if(name==CompNames.morph) {
			ver.addAll(resourceFactory.getAllMorphologyVersions(lang));
		}else if(name==CompNames.mt) {
			ver.addAll(resourceFactory.getAllMTVersions(lang, type));
		}else if(name==CompNames.domId) {
			ver.addAll(resourceFactory.getAllDomainIdVersions(lang));
		}else if(name==CompNames.stmr) {
			ver.addAll(resourceFactory.getAllStemmerVersions());
		}else if(name==CompNames.postEd) {
			ver.addAll(resourceFactory.getAllMtPosteditVersions());
		}else {
			System.out.println("Unidentified component: "+ name.toString());
			logger.fatal("Unidentified component: "+ name.toString());
			System.exit(0);
		}	
		return ver;
	}	
}
