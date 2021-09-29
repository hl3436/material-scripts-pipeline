/**
 * 
 */
package edu.columbia.dsi.identification;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import edu.columbia.dsi.SCRIPTS.Language;
import edu.columbia.dsi.utils.IoHdlr;
import edu.columbia.dsi.utils.ResourceFactory;

/**
 * @author badrashiny
 * Jun 11, 2018
 */
public class LanguageIdentification {
	private static Logger logger = Logger.getLogger(LanguageIdentification.class.getSimpleName());
	private ResourceFactory resourceFactory = new ResourceFactory();
	private String langIdVersion;
	private File langIdScript;
	private String lang=null;
	private boolean validityFlag=false;
	public static final  ArrayList<Language> supportedLanguages = new ArrayList<Language>(){
		/**
		 * 
		 */
		private static final long serialVersionUID = 4687302179622150650L;

	{ add(Language.tl); add(Language.sw); add(Language.so);}};

	/**
	 * 
	 */
	public LanguageIdentification(Language src) {
		logger.debug("Initializing the language identification docker image...");			
		this.langIdVersion=resourceFactory.getLangIdVersion();
		this.langIdScript=resourceFactory.getLangIdScript();
		if(src==Language.sw) {
			this.lang="swa";
		}else if(src==Language.tl) {
			this.lang="tgl";
		}else if(src==Language.so) {
			this.lang="som";
		}else {
			logger.fatal("Unsupported language: "+src.toString());
			validityFlag=false;
		}
		validityFlag=true;
	}
	
	public boolean isValid() {
		return validityFlag;
	}
	
	public void identifyDir(File inputDirPath, File outputDirPath){
		try {
			logger.debug("Identifying  directory: "+ inputDirPath.getCanonicalPath());	
			if(isIdentifiedDir(inputDirPath, outputDirPath)) {
				logger.debug("The output directory: "+outputDirPath.getCanonicalPath()+" is not empty. It looks like it is already identified...");
			}else {
				String cmd="sh"+" "+langIdScript.getCanonicalPath()+" "+inputDirPath.getCanonicalPath()+" "+outputDirPath.getCanonicalPath()+" "+lang+" "+langIdVersion;
				Process process = Runtime.getRuntime().exec(cmd);
				BufferedReader reader =	new BufferedReader(new InputStreamReader(process.getInputStream()));
				while ((reader.readLine()) != null) {}
				process.waitFor();
				
				logger.debug("Validating identification output...");
				if(isIdentifiedDir(inputDirPath, outputDirPath)) {
					logger.debug(inputDirPath.getCanonicalPath()+ " is identified in: "+ outputDirPath.getCanonicalPath());
				}else {
					logger.error(inputDirPath.getCanonicalPath()+ " is not completely identified. Please check the error messages.");
				}	
			}		
		} catch (IOException | InterruptedException e) {
			logger.error(e.getMessage());
		}
		
	}
	private boolean isIdentifiedDir(File inputDirPath, File outputDirPath) {
		boolean identified=false;
		ArrayList<File> srcFiles = IoHdlr.getInstance().getListOfFiles(inputDirPath);
		ArrayList<File> tgtFilesTmp = IoHdlr.getInstance().getListOfFiles(outputDirPath);
		ArrayList<File> tgtFiles=new ArrayList<File> ();
		for(File file:tgtFilesTmp) {
			if(file.getName().endsWith(".txt")) {
				tgtFiles.add(file);
			}
		}
		if(srcFiles.size()==tgtFiles.size()) {
			int notEmptyFiles=0;
			for(int i=0;i<tgtFiles.size();i++) {
				if(isEmptyFile(tgtFiles.get(i))) {
					logger.error(tgtFiles.get(i).getAbsolutePath()+" is empty");
				}else {
					notEmptyFiles++;
				}
			}
			if(notEmptyFiles==tgtFiles.size()) {
				identified=true;
			}			
		}	
		return identified;
	}
	
	private boolean isEmptyFile(File filePath) {
		BufferedReader tmp=null;
		boolean empty=true;
		try {
			if(filePath.exists()) {
				tmp = new BufferedReader(new FileReader(filePath.getCanonicalPath())); 
				String str=tmp.readLine();
				tmp.close();
				if(str == null) {//i.e. the file is empty or crashed before identification
					empty=true;
				}else {
					empty=false;
				}
			}else {
				empty=true;
			}
		}catch (IOException e) {
			logger.error(e.getMessage());
			try {
				tmp.close();
			} catch (IOException e1) {
				logger.error(e1.getMessage());
			}
			 empty=true;
		}
		return empty;
	}
	
	public String getLanguageIdVersion() {
		return langIdVersion;
	}

}
