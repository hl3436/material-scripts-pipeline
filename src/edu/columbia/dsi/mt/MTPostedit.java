/**
 * 
 */
package edu.columbia.dsi.mt;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import edu.columbia.dsi.containers.ProcessingUnit;

/**
 * @author badrashiny
 * Dec 26, 2018
 */
public class MTPostedit extends ProcessingUnit{
	private static Logger logger = Logger.getLogger(MTPostedit.class.getSimpleName());
	private File mtPosteditScript;
	/**
	 * 
	 */
	public MTPostedit() {
		logger.debug("Initializing CU-Postedit docker image...");		
		this.version=resourceFactory.getMtPosteditVersion();
		this.mtPosteditScript= resourceFactory.getMtPosteditScript();
		validityFlag=true;
	}
	
	
	public void run(File inputDir, File outDir) {
		try {
			logger.debug("Running M-Postedit on: "+ inputDir.getCanonicalPath());	
			if(isProcessedDir(inputDir, ".txt", outDir, ".txt")) {
				logger.debug("The output directory: "+outDir.getCanonicalPath()+" is not empty. It looks like it is already edited...");
			}else {
				String cmd="sh"+" "+mtPosteditScript.getCanonicalPath()+" "+inputDir.getCanonicalPath()+" "+outDir.getCanonicalPath()+" "+version;
				logger.debug(cmd);
				Process process = Runtime.getRuntime().exec(cmd);
				exportStdOutputLog(process, logger);
				process.waitFor();
				logger.debug(inputDir.getCanonicalPath()+ " is edited in: "+ outDir.getCanonicalPath());				
			}			
		} catch (IOException | InterruptedException e) {
			logger.error(e.getMessage());
		}		
	}
}
