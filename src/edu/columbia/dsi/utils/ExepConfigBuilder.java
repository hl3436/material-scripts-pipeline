/**
 * 
 */
package edu.columbia.dsi.utils;

import org.json.JSONObject;
public class ExepConfigBuilder {
	public ExepConfigBuilder() {
	}
	public enum QuerySet {QUERY1, QUERY2, QUERY1QUERY2, QUERY2QUERY3}
	public enum SubmissionDataset {DEV, ANALYSIS1, DEVANALYSIS1, EVAL1EVAL2, EVAL1EVAL2EVAL3, ANALYSIS1ANALYSIS2;}
	 public enum Language {tl, sw;}
	public JSONObject build(Language lang, SubmissionDataset dataset, QuerySet querySet ) {
		JSONObject configFile = new JSONObject();
		/**
		 * TODO: Please fill the gaps
		 * 
		 * 
		 * 
		 * 
		 * 
		 * 
		 * 
		 * 
		 * 
		 * 
		 */	
		return configFile;
	}
}
