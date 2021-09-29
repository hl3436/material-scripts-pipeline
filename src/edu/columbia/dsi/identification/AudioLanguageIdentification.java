/**
 * 
 */
package edu.columbia.dsi.identification;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import edu.columbia.dsi.SCRIPTS.Language;
import edu.columbia.dsi.containers.ProcessingUnit;

/**
 * @author badrashiny
 * Oct 26, 2018
 */
public class AudioLanguageIdentification extends ProcessingUnit {
	private static Logger logger = Logger.getLogger(LanguageIdentification.class.getSimpleName());
	private File langIdScript;
	private ArrayList<Double> nbThresholds=new ArrayList<Double>();//Default value
	private ArrayList<Double> wbThresholds=new ArrayList<Double>();//Default value
	private File metaDataPath=null;
	

	/**
	 * 
	 */
	public AudioLanguageIdentification(Language lang, File metaDataPath) {
		logger.debug("Initializing the audio language identification docker image...");			
		this.langIdScript=resourceFactory.getAudioLangIdScript();
		this.metaDataPath=metaDataPath;
		if(lang==Language.sw) {
			this.version=resourceFactory.getSWAudioLangIdVersion();
			this.nbThresholds=resourceFactory.getSWNBAudioLangIdThrsholds();
			this.wbThresholds=resourceFactory.getSWWBAudioLangIdThrsholds();
			validityFlag=true;
		}else if(lang==Language.tl) {
			this.version=resourceFactory.getTLAudioLangIdVersion();
			this.nbThresholds=resourceFactory.getTLNBAudioLangIdThrsholds();
			this.wbThresholds=resourceFactory.getTLWBAudioLangIdThrsholds();
			validityFlag=true;
		}else if(lang==Language.so) {
			this.version=resourceFactory.getSOAudioLangIdVersion();
			this.nbThresholds=resourceFactory.getSONBAudioLangIdThrsholds();
			this.wbThresholds=resourceFactory.getSOWBAudioLangIdThrsholds();
			validityFlag=true;
		}else {
			logger.fatal("Unsupported language: "+lang.toString());
			validityFlag=false;
		}
		if(this.nbThresholds.size()!=this.nbThresholds.size()) {
			logger.fatal("missmatch in NB and WB thresholds sizes");
			validityFlag=false;
		}
		
	}
	
	public void run(File inputDir, File outDir){
		try {
			logger.debug("Identifying  directory: "+ inputDir.getCanonicalPath());	
			if(this.nbThresholds.size()!=this.nbThresholds.size()) {
				logger.fatal("missmatch in NB and WB thresholds sizes");
				return;
			}
			for(int i=0;i<nbThresholds.size();i++) {
				double nbThreshold=nbThresholds.get(i).doubleValue();
				double wbThreshold=wbThresholds.get(i).doubleValue();
				File outDirPath=new File(outDir.getAbsolutePath()+"-nbThreshold-"+String.valueOf(nbThreshold)+"-wbThreshold-"+String.valueOf(wbThreshold));
				if (! outDirPath.exists()){
					outDirPath.mkdir();
				}			
				String cmd="sh"+" "+this.langIdScript.getCanonicalPath()+" "+inputDir.getCanonicalPath()+" "+outDirPath.getCanonicalPath()+" "+metaDataPath.getCanonicalPath();
				cmd+=" "+String.valueOf(nbThreshold)+" "+String.valueOf(wbThreshold)+" "+this.version;
				logger.debug(cmd);
				Process process = Runtime.getRuntime().exec(cmd);	
				exportStdOutputLog(process, logger);				
				process.waitFor();
				logger.debug(inputDir.getCanonicalPath()+ " is identified in: "+ outDirPath.getCanonicalPath());	
			}
		} catch (IOException | InterruptedException e) {
			logger.error(e.getMessage());
		}		
	}
	
}
