/**
 * 
 */
package edu.columbia.dsi.morphology;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;

import org.apache.log4j.Logger;

import edu.columbia.dsi.containers.ProcessingUnit;
import edu.columbia.dsi.utils.IoHdlr;
import edu.columbia.dsi.utils.WorkDirHdlr;

/**
 * @author badrashiny
 * Sep 13, 2018
 */
public class Stemmer extends ProcessingUnit{
	private static Logger logger = Logger.getLogger(Stemmer.class.getSimpleName());
	private File stemDirScript;
	private String nGrams="char_3_gram char_4_gram char_5_gram";//Default value (the list of stemmers you wish to run on the documents separated by spaces)

	/**
	 * 
	 */
	public Stemmer() {
		logger.debug("Initializing the stemmer docker image...");		
		this.version=resourceFactory.getStemmerVersion();
		this.stemDirScript= resourceFactory.getStemDirScript();
		this.nGrams=resourceFactory.getStemmerNgrams();	
		validityFlag=true;
	}
	
	public void run(File inputDir, File outDir) {
		try {	
			String[] ngramsList=this.nGrams.split(";;");
			StringBuilder nGramsStr=new StringBuilder();
			for(String ngram:ngramsList) {
				if(!isProcessedDir(inputDir, ".txt", new File (outDir.getCanonicalPath()+File.separator+ngram), ".txt")) {
					nGramsStr.append(ngram+" ");
				}else {
					logger.debug("The output directory: "+outDir.getCanonicalPath()+File.separator+ngram+" is not empty. It looks like it is already stemmed...");
				}
			}
			if(nGramsStr.toString().trim().isEmpty()) {
				logger.debug("All of the input nGrams are already there. Nothing left for the stemmer to be processed.");
			}else {
				String workLogDir="Stemmer-WorkLogDir"+UUID.randomUUID().toString()+ (new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()));
				ArrayList<String>folders=new ArrayList<String>();
				folders.add(workLogDir);
				WorkDirHdlr workDirHdlr = new WorkDirHdlr(folders);
				
				String[] cmd = {"sh", stemDirScript.getCanonicalPath(), inputDir.getCanonicalPath(), 
                                    outDir.getCanonicalPath(), workDirHdlr.getSubFolderHdlr(workLogDir).getCanonicalPath(),
                                    nGramsStr.toString().trim(), this.version};
				
				Process processStemmer = Runtime.getRuntime().exec(cmd);
				
				exportStdOutputLog(processStemmer, logger);				
				processStemmer.waitFor();
				
				
				ArrayList<File> logs = IoHdlr.getInstance().getListOfFiles(workDirHdlr.getSubFolderHdlr(workLogDir));
				for(File log:logs) {
					logger.debug(IoHdlr.getInstance().readFile(log));
				}				
				workDirHdlr.cleanup();
				logger.debug("The input directory "+ inputDir.getCanonicalPath() +" is stemmed to :"+ outDir.getCanonicalPath());
			}			
						
		} catch (IOException | InterruptedException e) {
			logger.error(e.getMessage());
		}			
	}	
}
