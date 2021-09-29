/**
 * 
 */
package edu.columbia.dsi.asr;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import edu.columbia.dsi.SCRIPTS.Language;
import edu.columbia.dsi.containers.ProcessingUnit;

/**
 * @author badrashiny
 * Oct 9, 2019
 */
public class KWS extends ProcessingUnit{
	private static Logger logger = Logger.getLogger(KWS.class.getSimpleName());
	private File kwsScript=null;
	private Language lang=null;
	
	/**
	 * 
	 */
	public KWS(Language lang) {
		this.lang=lang;
		this.version=resourceFactory.getKWSVersion(lang);
		if(this.version!=null) {
			logger.debug("Loading kWS-Docker image for "+lang.toString());
			kwsScript=resourceFactory.getKWSScript();
			validityFlag=true;
		}else {
			logger.fatal("Unsupported language: "+lang.toString());
			validityFlag=false;
		}
	}
	
	public void run(File inputDir,  File outDir) {				
		try {
			if(isProcessed(outDir)) {
				logger.debug("The output directory: "+outDir.getCanonicalPath()+" is not empty. It looks like it is already processed...");
			}else {
				kwsDir(inputDir, outDir);
				if(isProcessed(outDir)) {
					logger.debug("The KWS output of the input directory "+ inputDir.getCanonicalPath() +" can be found in: "+ outDir.getCanonicalPath());
				}else {
					logger.debug("Can not run the KWS on input directory "+ inputDir.getCanonicalPath() +". Please check the log file");
				}
				
			}
		} catch (IOException | InterruptedException e) {
			logger.error(e.getMessage());
		}	
	}
	
	private boolean isProcessed(File outDir) {
		File outFile=new File(outDir.getAbsolutePath()+File.separator+"results"+File.separator+"kws.hdf5");
		if(outFile.exists()) {
			return true;
		}else {
			return false;
		}		
	}
	
	private void kwsDir(File inputDir, File outDir) throws IOException, InterruptedException {
		logger.debug("Running the KWS-Docker image on "+inputDir.getCanonicalPath());
		String cmd=null;
		cmd="sh"+" "+kwsScript.getCanonicalPath()+" "+inputDir.getCanonicalPath()+" "+outDir.getCanonicalPath()+" "+lang.toString()+" "+version;
		logger.debug(cmd);
		Process process = Runtime.getRuntime().exec(cmd);	
		exportStdOutputLog(process, logger);				
		process.waitFor();
		logger.debug("The input directory "+ inputDir.getCanonicalPath() +" is KWS'd in :"+ outDir.getCanonicalPath());	
	}

}
