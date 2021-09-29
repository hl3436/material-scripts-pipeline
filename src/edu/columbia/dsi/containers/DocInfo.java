/**
 * 
 */
package edu.columbia.dsi.containers;

import java.io.File;

import edu.columbia.dsi.utils.IoHdlr;

/**
 * @author badrashiny
 * Mar 1, 2018
 */
public class DocInfo {
	public enum Type {text, audio;}
	private File src=null;
	private File translated=null; //stays null if type is not audio
	private File transcribed=null;
	private Type type=null;
	private String summary;
	private double score=0;
	private int rank=-1;
	/**
	 * 
	 */
	public DocInfo() {	
	}
	
	public void setType(Type type) {
		this.type=type;
	}
	
	public void setRank(int rank) {
		this.rank=rank;
	}
	
	public void setScore(double score) {
		this.score=score;
	}
	
	public void setSrc(File src) {
		this.src=src;
	}
	
	public void setTranslation(File translated) {
		this.translated=translated;
	}
	
	public void setTranscript(File transcribed) {
		this.transcribed=transcribed;
	}
	
	
	
	
	
	
	public void setSummary(String summary) {
		this.summary=summary;
	}
	
	public Type getType() {
		return type;	
	}
	
	public double getScore() {
		return score;	
	}
	
	public int getRank() {
		return rank;	
	}
	
	public File getSrcAsFile() {
		return src;	
	}
	
	public File getTranslationAsFile() {
		return translated;	
	}
	
	public File getTranscriptionAsFile() {
		return transcribed;	
	}
	
	public String getSrc() {
		if(type==Type.text)	{
			return IoHdlr.getInstance().readFile(src);			
		}else {//i.e. audio
			return IoHdlr.getInstance().readFile(transcribed);	
		}
	}
	
	public String getTranslation() {
		 return IoHdlr.getInstance().readFile(translated);
	}
	
	public String getSummary() {
		return summary;
	}
}
