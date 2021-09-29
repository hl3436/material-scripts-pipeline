/**
 * 
 */
package edu.columbia.dsi.containers;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;

import edu.columbia.dsi.SCRIPTS.Language;
import edu.columbia.dsi.containers.DocInfo.Type;

/**
 * @author badrashiny
 * Apr 2, 2018
 */
public class Corpora {
	//private static Logger logger = Logger.getLogger(Corpora.class.getSimpleName());
	private String absolutePathLocation;
	private String relativeDirectory;
	
	ArrayList<Corpus> corpora=new ArrayList<Corpus>();
	ArrayList<Query> queries=new ArrayList<Query>();
	Hashtable<Language,File>langToQPOutMap=new Hashtable<Language,File>();
	

	/**
	 * EX: /storage/data/NIST-data/1B/IARPA_MATERIAL_BASE-1B/ANALYSIS1/text/src
	 * absolutePathLocation=/storage/data
	 * relativeDirectory=NIST-data
	 * corpusName=1B/IARPA_MATERIAL_BASE-1B/ANALYSIS1
	 * sourceLocation=text/src
	 */
	public Corpora(String absolutePathLocation, String relativeDirectory) {
		this.absolutePathLocation=absolutePathLocation;
		this.relativeDirectory=relativeDirectory;
	}
	
	/**
	 * EX: /storage/data/NIST-data/1B/IARPA_MATERIAL_BASE-1B/ANALYSIS1/text/src
	 * absolutePathLocation=/storage/data
	 * relativeDirectory=NIST-data
	 * corpusName=1B/IARPA_MATERIAL_BASE-1B/ANALYSIS1
	 * sourceLocation=text/src
	 */
	public void addCorpus(String corpusName, String sourceLocation, Type type, Language lang, String metaDataPath, String manualTranslationLocation, String manualTranscriptionLocation) {
		corpora.add(new Corpus(absolutePathLocation, relativeDirectory, corpusName, sourceLocation, type, lang, metaDataPath, manualTranslationLocation, manualTranscriptionLocation));		
	}
	
	/**
	 * EX: /storage/data/NIST-data/1B/IARPA_MATERIAL_BASE-1B/QUERY1/query_list.tsv
	 * absolutePathLocation=/storage/data
	 * relativeDirectory=NIST-data
	 * queryName=1B/IARPA_MATERIAL_BASE-1B/QUERY1/query_list.tsv
	 */
	public void addQuery(String queryName, Language lang) {
		Query q=new Query(absolutePathLocation, relativeDirectory, queryName, lang);
		queries.add(q);		
		langToQPOutMap.put(lang, q.getQPVersionDir());
	}
	
	public File getQPOut(Language lang){
		if(langToQPOutMap.containsKey(lang)) {
			return langToQPOutMap.get(lang);
		}else {
			return null;
		}
	}
	
	public ArrayList<Corpus> getAllCorpora(){
		return corpora;
	}
	
	public ArrayList<Query> getAllQueries(){
		return queries;
	}
	
	public String getRelativeDirectory() {
		return relativeDirectory;
	}
	
	public File getAbsolutePathLocation() {
		return new File (absolutePathLocation);
	}
	
	

}
