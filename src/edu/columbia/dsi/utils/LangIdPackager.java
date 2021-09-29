package edu.columbia.dsi.utils;

import java.io.File;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import edu.columbia.dsi.SCRIPTS.Language;

/**
 * @author badrashiny
 * Jul 25, 2018
 */
public class LangIdPackager {
	private static Logger logger = Logger.getLogger(LangIdPackager.class.getSimpleName());
	private ResourceFactory resourceFactory = new ResourceFactory();

	public LangIdPackager() {
		logger.debug("Initializing the LanguageID packer...");
	}
	
	public void createPackage(ArrayList<File>langIds, Language lang, String outDirectory) {
//		String outFileName=outDirectory+File.separator+"l-";
		String outFileName=outDirectory+File.separator;
//		String header=null;
		if(lang==Language.sw) {
			outFileName+="1A.tsv";
//			header="1A";
		}else if(lang==Language.tl) {
			outFileName+="1B.tsv";
//			header="1B";
		}else if(lang==Language.so) {
			outFileName+="1S.tsv";
//			header="1S";
		}else {
			logger.error("Unsupported Language: "+lang.toString());
			return;
		}
		ArrayList<String>mergedFiles= new ArrayList<String>(20000);//initial size (EVAL1+EVAL2+EVAL3)
//		mergedFiles.add(header);
		
		for(File f:langIds) {
//			mergedFiles.addAll(IoHdlr.getInstance().readFileLinesWithoutHeader(f));	
			mergedFiles.addAll(IoHdlr.getInstance().readFileLines(f));	
		}
		if(IoHdlr.getInstance().exportFile(mergedFiles, new File(outFileName))) {
			logger.debug("The output LangugeID package has been created here: "+outFileName);
		}else {
			logger.error("Can not create a LangugeID package for the input list of files");
		}	
	}
}
