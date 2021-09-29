/**
 * 
 */
package edu.columbia.dsi.morphology;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import edu.columbia.dsi.SCRIPTS.Language;
import edu.columbia.dsi.containers.ProcessingUnit;
import edu.columbia.dsi.utils.IoHdlr;

/**
 * @author badrashiny
 * Mar 27, 2018
 */
public class MorphologicalAnalyzer extends ProcessingUnit{
	private static Logger logger = Logger.getLogger(MorphologicalAnalyzer.class.getSimpleName());
	private File analyzeDirScript;
	private Language lang=null;
	
	/**
	 * 
	 */
	public MorphologicalAnalyzer(Language lang) {			
		ArrayList<String>versions=resourceFactory.getAllMorphologyVersions(lang);
		if(versions!=null && !versions.isEmpty()) {
			this.analyzeDirScript= resourceFactory.getMorphAnaslyzeDirScript();	
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
			logger.debug("Loading the morphological analyzer server for "+lang.toString());
			if(isProcessedDir(inputDir, ".txt", outDir, ".txt")) {
				logger.debug("The output directory: "+outDir.getCanonicalPath()+" is not empty. It looks like it is already analyzed...");
			}else {
				String cmd="sh"+" "+analyzeDirScript.getCanonicalPath()+" "+inputDir.getCanonicalPath()+" "+outDir.getCanonicalPath()+" "+lang.toString()+" "+version;
				logger.debug(cmd);
				Process process = Runtime.getRuntime().exec(cmd);
				exportStdOutputLog(process, logger);
				process.waitFor();				
				logger.debug("The input directory "+ inputDir.getCanonicalPath() +" is morphologically analyzed to :"+ outDir.getCanonicalPath());
			}									
		} catch (IOException | InterruptedException e) {
			logger.error(e.getMessage());
		}			
		
	}
	
	protected boolean isEmptyFile(File file){
		if(file==null || !file.exists()) {
			return true;
		}
		String emptyJsonFile="[[]]";
		boolean isEmpty=true;
		ArrayList<String> lines=IoHdlr.getInstance().readFileLines(file);
		if(lines==null || lines.isEmpty()) {
			return true;
		}
		for(String line:lines) {
			if(line!=null && !line.trim().isEmpty() && !line.trim().equals(emptyJsonFile)) {
				isEmpty=false;
				break;
			}
		}
		return isEmpty;		
	}

}
