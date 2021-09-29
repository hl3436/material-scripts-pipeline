package edu.columbia.dsi.summarization;

import org.apache.log4j.Logger;

import edu.columbia.dsi.containers.QueryRes;

/**
 * @author badrashiny
 * Mar 3, 2018
 */
public class Summarizer {
	private static Logger logger = Logger.getLogger(Summarizer.class.getSimpleName());
	private boolean validityFlag=false;
	public enum SummarizerEngine {rnnsum, ezsummary;}
	private RNNsummary rnnsum=null;
	private EZsummary ezsum=null;
	public Summarizer() {
		logger.debug("Loading summarizer engines ...");
		rnnsum=new RNNsummary();
		ezsum=new EZsummary();
		if(rnnsum.isValid() && ezsum.isValid()) {
			validityFlag=true;
		}
		logger.debug("Summarizer engines are loaded successfully...");
	}
	
	public boolean isValid() {
		return validityFlag;
	}
	
	public void summarize(QueryRes docs, SummarizerEngine engine) {
		if(engine==SummarizerEngine.rnnsum) {
			rnnsum.summarize(docs);
		}else if(engine==SummarizerEngine.ezsummary) {
			ezsum.summarize(docs);
		}
	}
	
	
	public void shutdown() {
		if(rnnsum!=null) {
			rnnsum.shutdown();
		} 
		if(ezsum!=null) {
			ezsum.shutdown();
		} 
	}

}
