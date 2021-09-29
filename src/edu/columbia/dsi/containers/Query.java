/**
 * 
 */
package edu.columbia.dsi.containers;

import java.io.File;

import edu.columbia.dsi.SCRIPTS.Language;
import edu.columbia.dsi.utils.ResourceFactory;

/**
 * @author badrashiny
 * Jul 10, 2018
 */
public class Query {
	private ResourceFactory resourceFactory = new ResourceFactory();
	private Language lang;
	
	private String queryName;
	private File queryList=null;
	private String queryStoreName="query_store";
	private String qpVersion;
	
	private File qpVersionDir=null;
	private File qpOutDir=null;
	
	/**
	 * EX: /storage/data/NIST-data/1B/IARPA_MATERIAL_BASE-1B/QUERY1/query_list.tsv
	 * absolutePathLocation=/storage/data
	 * relativeDirectory=NIST-data
	 * queryName=1B/IARPA_MATERIAL_BASE-1B/QUERY1/query_list.tsv
	 */
	public Query(String absolutePathLocation, String relativeDirectory, String queryName, Language lang) {
		this.lang=lang;			
		this.queryName=queryName;
		this.queryList=new File(absolutePathLocation+File.separator+relativeDirectory+File.separator+queryName);
		String parentName=new File(this.queryList.getParent()).getName();
		File queryStoreLocation=new File(this.queryList.getParent().replaceAll(parentName+"$", this.queryStoreName));
		
		if (! queryStoreLocation.exists()){
			queryStoreLocation.mkdir();
	    }
		
		this.qpVersion=resourceFactory.getUmdQueryProcessorVersion();
		this.qpVersionDir=new File(queryStoreLocation.getAbsolutePath()+File.separator+qpVersion.replaceAll("[/_:]", "-"));
		if (! qpVersionDir.exists()){
			qpVersionDir.mkdir();
	    }
		
		this.qpOutDir=new File(qpVersionDir.getAbsolutePath()+File.separator+parentName);
		if (! this.qpOutDir.exists()){
			this.qpOutDir.mkdir();
	    }		
	}
	
	public File getInputQueriesList() {
		return this.queryList;
	}
	
	public Language getQueriesLang() {
		return this.lang;
	}
	
	public File getOutQueryProcessingDir() {
		return this.qpOutDir;
	}
	
	public File getQPVersionDir() {
		return this.qpVersionDir;
	}
	
	public String getQueryName() {
		return new File(this.queryName).getParent();
	}
	
	public String getQuerProcessorVerison() {
		return this.qpVersion;
	}

}
