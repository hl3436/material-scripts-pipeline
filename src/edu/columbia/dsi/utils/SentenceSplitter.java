/**
 * 
 */
package edu.columbia.dsi.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import edu.columbia.dsi.SCRIPTS.Language;

/**
 * @author badrashiny
 * Apr 9, 2018
 */
public class SentenceSplitter {
	private static Logger logger = Logger.getLogger(SentenceSplitter.class.getSimpleName());
	private ResourceFactory resourceFactory = new ResourceFactory();
	private File splitSentScriptPath=null;
	private File splitSentScriptDir=null;

	/**
	 * 
	 */
	public SentenceSplitter() {
		splitSentScriptPath=resourceFactory.getSplitSentScript();
		splitSentScriptDir=resourceFactory.getSplitSentScriptDir();
	}
	
	
	public void splitDirectory(File inputDir, File outDir, Language lang) {
		try {
			logger.debug("Splitting directory: "+ inputDir.getCanonicalPath());
			ArrayList<File> srcFiles = IoHdlr.getInstance().getListOfFiles(inputDir);
			for(int i=0;i<srcFiles.size();i++) {
				File inputFile=srcFiles.get(i);
				File outputFile = new File (outDir.getCanonicalPath()+File.separator+inputFile.getName());
				if(inputFile.getCanonicalPath().endsWith(".txt")) {
					splitFile(inputFile, outputFile, lang);
				}				
			}
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
	}
	
	public void splitFile(File inputFile, File outputFile, Language lang) {
		try {
			logger.debug("Splitting "+inputFile.getCanonicalPath());		
			if(outputFile.exists() && !isEmptyFile(outputFile)) {
				logger.debug(inputFile.getCanonicalPath()+" is already split");
			}else {
//				ProcessBuilder processBuilder = new ProcessBuilder(splitSentScriptPath.getCanonicalPath(), inputFile.getCanonicalPath(), outputFile.getCanonicalPath());
//				Process process = processBuilder.start();
//				process.waitFor();
				
				String cmd="bash"+" "+splitSentScriptPath.getCanonicalPath()+" "+splitSentScriptDir.getCanonicalPath()+" "+inputFile.getCanonicalPath()+" "+outputFile.getCanonicalPath()+" "+lang.toString();
				logger.debug(cmd);
				Process process = Runtime.getRuntime().exec(cmd);
				BufferedReader reader =	new BufferedReader(new InputStreamReader(process.getInputStream()));
				while ((reader.readLine()) != null) {}
				process.waitFor();
				
			    logger.debug(inputFile.getCanonicalPath()+ " is split to: "+ outputFile.getCanonicalPath());
			}
	
		} catch (IOException | InterruptedException e) {
			logger.error(e.getMessage());
		}			
		
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
	
	
	
	
	public String getSentenceSplitterVersion() {
		return resourceFactory.getSentenceSplitterVersion();		
	}

}
