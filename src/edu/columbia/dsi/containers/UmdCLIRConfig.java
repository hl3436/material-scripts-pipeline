/**
 * 
 */
package edu.columbia.dsi.containers;

import java.io.File;
import java.util.ArrayList;

import edu.columbia.dsi.SCRIPTS.Language;
import edu.columbia.dsi.ir.UmdCLIR.UmdCLIRCombFormat;

/**
 * @author badrashiny
 * May 9, 2018
 */
public class UmdCLIRConfig {
	private ArrayList<UmdMatcherConfig> matcherConfigs=new ArrayList<UmdMatcherConfig>();
	private String configurationName;
	private ArrayList<File> queriesListsPaths=new ArrayList<File>();
	private Language tgtLang;
	private int eviComCutoff;
	private UmdCLIRCombFormat eviComFormat;
	
	

	/**
	 * 
	 */
	public UmdCLIRConfig(String configurationName, ArrayList<UmdMatcherConfig> matcherConfigs, ArrayList<File> queriesListsPaths, Language tgtLang, int eviComCutoff, UmdCLIRCombFormat eviComFormat) {
		this.configurationName=configurationName;
		this.matcherConfigs.addAll(matcherConfigs);
		this.queriesListsPaths.addAll(queriesListsPaths);
		this.tgtLang=tgtLang;
		this.eviComCutoff=eviComCutoff;
		this.eviComFormat=eviComFormat;		
	}
	
	public String getConfigurationName() {
		return configurationName;
	}
	
	public ArrayList<UmdMatcherConfig> getMathcerConfig() {
		return matcherConfigs;
	}
	
	public ArrayList<File> getQueriesListsPaths() {
		return queriesListsPaths;
	}
	
	public Language getTgtLang() {
		return tgtLang;
	}
	
	public int getEviCombCutoff() {
		return eviComCutoff;
	}
	
	public UmdCLIRCombFormat getEviCombFormat() {
		return eviComFormat;
	}

}
