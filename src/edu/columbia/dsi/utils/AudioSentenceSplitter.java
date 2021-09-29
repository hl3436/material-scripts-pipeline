/**
 * 
 */
package edu.columbia.dsi.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import edu.columbia.dsi.SCRIPTS.Language;
import edu.columbia.dsi.containers.ProcessingUnit;

/**
 * @author badrashiny
 * Oct 1, 2019
 */
public class AudioSentenceSplitter extends ProcessingUnit{
	private static Logger logger = Logger.getLogger(AudioSentenceSplitter.class.getSimpleName());
	private File splitSentScript=null;
	private Language lang=null;	
	
	/**
	 * 
	 */
	public AudioSentenceSplitter(Language lang) {
		logger.debug("Initializing the audio sentence splitter docker image...");	
		
		ArrayList<String>versions=resourceFactory.getAllAudioSentenceSplitterVersions(lang);
		if(versions!=null && !versions.isEmpty()) {
			this.splitSentScript=resourceFactory.getSplitAudSentScript();
			this.lang=lang;
			this.version=versions.get(0);
			validityFlag=true;
		}else {
			logger.fatal("Unsupported language: "+lang.toString());
			validityFlag=false;
		}
	}
	
	public void run(File inputDir, File outDir) {
		try {
			logger.debug("Splitting directory: "+ inputDir.getCanonicalPath());
			if(isProcessedDir(inputDir, ".txt", outDir, ".txt")) {
				logger.debug("The output directory: "+outDir.getCanonicalPath()+" is not empty. It looks like it is already split...");
			}else {
				String cmd=null;
				cmd="sh"+" "+splitSentScript.getCanonicalPath()+" "+inputDir.getCanonicalPath()+" "+outDir.getCanonicalPath()+" "+lang.toString()+" "+version;
				logger.debug(cmd);
				Process process = Runtime.getRuntime().exec(cmd);
				exportStdOutputLog(process, logger);
				process.waitFor();	
				IoHdlr.getInstance().mergeFilesWithEmptyLinesInBetween(outDir, ".txt", ".rest.bst", ".all"); //create one file with all N best solutions
			}
			
		} catch (IOException | InterruptedException e) {
			logger.error(e.getMessage());
		}
	}
	
}
