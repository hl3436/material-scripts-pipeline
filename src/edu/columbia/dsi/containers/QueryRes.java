/**
 * 
 */
package edu.columbia.dsi.containers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import edu.columbia.dsi.containers.DocInfo.Type;
import edu.columbia.dsi.utils.ResourceFactory;

/**
 * @author badrashiny
 * Mar 1, 2018
 */
public class QueryRes {
	private static Logger logger = Logger.getLogger(QueryRes.class.getSimpleName());
	private ArrayList<String>collections=new ArrayList<String>();
	private String query=null;
	private String queryWords=null;
    private ArrayList<DocInfo> docsInfo=new ArrayList<DocInfo>();
    private ResourceFactory resourceFactory = new ResourceFactory();
   
    
	public QueryRes(String corpora, String query, String queryWords) {
		logger.debug("ClirOutObj created for: "+corpora+ "And queryWords: "+ queryWords);
		this.query=query;
		this.queryWords="\""+queryWords+"\"";
		String corporaPath = resourceFactory.getScriptsCorporaPath();
		String[] tmp=corpora.split("\\+");			
		for(int i=0;i<tmp.length;i++) {
			/**
			 * TODO: This is an addhoc solution. the clir should return paths ends with /text or /audio
			 */	
			tmp[i]=tmp[i].replaceAll("/$", "");
			if(tmp[i].endsWith("/asr-transcript")) {
				tmp[i]=tmp[i].replaceAll("/asr-transcript$", "");
			}else if(tmp[i].endsWith("/src")) {
				tmp[i]=tmp[i].replaceAll("/src$", "");
			}
			//=========================================================================================
			File  collection= new File(corporaPath+File.separator+tmp[i]);
			if(collection.exists()) {
				try {
					collections.add(collection.getCanonicalPath());
					logger.debug("Added collection: "+collection.getCanonicalPath());
					
				} catch (IOException e) {
					logger.error("Not found Collection: "+corporaPath+File.separator+tmp[i]);
				}
			}else {
				logger.error("Not found Collection: "+corporaPath+File.separator+tmp[i]);
			}			
		}		
	}
	
	public void addDocument(String docID, int rank, double score) {
		File src = null;
		File transcribed=null;
		File translated = null;
		Type type=null;
		boolean found=false;
		for(int i =0;i<collections.size();i++){
			if(collections.get(i).endsWith("audio")){
				src=new File (collections.get(i)+File.separator+"src"+File.separator+docID+".wav");
				transcribed=new File (collections.get(i)+File.separator+"asr-transcript"+File.separator+docID+".txt");
				translated=new File (collections.get(i)+File.separator+"mt-translation"+File.separator+docID+".txt");		
				type=Type.audio;
				if(src.exists() && transcribed.exists() && translated.exists()) {
					try {
						logger.debug("Added document: "+src.getCanonicalPath()+"\t"+score+"\t"+rank);
					} catch (IOException e) {
						logger.error(e.getMessage());
					}
					found=true;
					DocInfo docInfo = new DocInfo();
					docInfo.setType(type);
					docInfo.setRank(rank);
					docInfo.setScore(score);
					docInfo.setSrc(src);
					docInfo.setTranslation(translated);
					docInfo.setTranscript(transcribed);
					docsInfo.add(docInfo);
				}
            }else{//i.e. text
            		src=new File (collections.get(i)+File.separator+"src"+File.separator+docID+".txt");
				transcribed=null;
				translated=new File (collections.get(i)+File.separator+"mt-translation"+File.separator+docID+".txt");
				type=Type.text;
				if(src.exists()  && translated.exists()) {
					try {
						logger.debug("Added document: "+src.getCanonicalPath()+"\t"+score+"\t"+rank);
					} catch (IOException e) {
						logger.error(e.getMessage());
					}
					found=true;
					DocInfo docInfo = new DocInfo();
					docInfo.setType(type);
					docInfo.setRank(rank);
					docInfo.setScore(score);
					docInfo.setSrc(src);
					docInfo.setTranslation(translated);
					docsInfo.add(docInfo);
				}
            }			
		}
		if(!found) {
			logger.error("Documnet couldn't be found in any collection: "+docID);
		}else {
			
		}
	}
	
	public String getQuery() {
		return query;
	}

	public String getQueryWords() {
		return queryWords;
	}
	
	public int size() {
		return docsInfo.size();
	}
	
	public DocInfo getResult(int index) {
		if(index>=0 && index<size()) {
			return docsInfo.get(index);
		}else {
			return null;
		}		
	}
}
