/**
 * 
 */
package edu.columbia.dsi.asr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import edu.columbia.dsi.SCRIPTS.Language;
import edu.columbia.dsi.containers.ProcessingUnit;
import edu.columbia.dsi.utils.IoHdlr;

/**
 * @author badrashiny
 * Mar 9, 2018
 */
public class ASR extends ProcessingUnit{
	private static Logger logger = Logger.getLogger(ASR.class.getSimpleName());
	private int nbestSze=1;
	private File nbestGeneratorScript=null;
	private File transcribeScript=null;
	private String nbestGeneratorVersion=null;
	private int numberOfJobs=1;
	private int numberOfThreads=1;
	private File metaDataPath=null;

	/**
	 * 
	 */
	public ASR(Language lang, File metaDataPath, int numberOfJobs, int numberOfThreads) {
		this.version=resourceFactory.getASRVersion(lang);
		this.metaDataPath=metaDataPath;
		this.numberOfJobs=numberOfJobs;
		this.numberOfThreads=numberOfThreads;
		if(this.version!=null) {
			logger.debug("Loading ASR-Docker image for "+lang.toString());
			nbestGeneratorScript=resourceFactory.getASRSNbestGeneratorscript();
			transcribeScript=resourceFactory.getASRTranscribeScript();
			nbestSze=resourceFactory.getASRNbestSize();
			nbestGeneratorVersion=resourceFactory.getASRSNbestGeneratorVersion();
			validityFlag=true;
		}else {
			logger.fatal("Unsupported language: "+lang.toString());
			validityFlag=false;
		}
		
	}
	
	public void run(File inputDir,  File outDir) {				
		try {
			if(isProcessedDir(inputDir, ".wav",  outDir, ".bst")) {//i.e. finished the ASR and n-best generator
				logger.debug("The output directory: "+outDir.getCanonicalPath()+" is not empty. It looks like it is already transcribed...");
			}else if(isProcessedDir(inputDir, ".wav",  outDir, ".ctm")){//i.e. finished the ASR but not the n-best generator
				nbestGenerator(outDir);
				logger.debug("The input directory "+ inputDir.getCanonicalPath() +" is transcribed to :"+ outDir.getCanonicalPath());
			}else {//i.e. the ASR didn't finish
				transcribeDir(inputDir, outDir);
				nbestGenerator(outDir);
				logger.debug("The input directory "+ inputDir.getCanonicalPath() +" is transcribed to :"+ outDir.getCanonicalPath());
			}		
		} catch (IOException | InterruptedException e) {
			logger.error(e.getMessage());
		}	
	}
	
	private void transcribeDir(File inputDir, File outDir) throws IOException, InterruptedException {
		logger.debug("Running the ASR-Docker image on "+inputDir.getCanonicalPath());
		String cmd=null;
		cmd="sh"+" "+transcribeScript.getCanonicalPath()+" "+String.valueOf(numberOfJobs)+" "+String.valueOf(numberOfThreads)+" "+inputDir.getCanonicalPath()+" "+metaDataPath.getCanonicalPath();
		cmd+=" "+outDir.getCanonicalPath()+" "+version;				

		logger.debug(cmd);
		Process processASR = Runtime.getRuntime().exec(cmd);	
		exportStdOutputLog(processASR, logger);				
		processASR.waitFor();
		logger.debug("The input directory "+ inputDir.getCanonicalPath() +" is transcribed to :"+ outDir.getCanonicalPath());	
	}
	
	private void nbestGenerator(File dirPath) throws IOException, InterruptedException {
		String cmd="sh"+" "+nbestGeneratorScript.getCanonicalPath()+" "+dirPath.getCanonicalPath()+" "+nbestSze+" "+nbestGeneratorVersion;
		logger.debug(cmd);
		Process process = Runtime.getRuntime().exec(cmd);
		exportStdOutputLog(process, logger);
		process.waitFor();
		IoHdlr.getInstance().mergeFilesWithEmptyLinesInBetween(dirPath, ".txt", ".rest.bst", ".all"); //create one file with all N best solutions
		IoHdlr.getInstance().mergeFilesWithEmptyLinesInBetween(dirPath, ".conf", ".rest.bst.conf", ".all.conf"); //create one file with all N best solutions
	}
	
	protected boolean isProcessedDir(File inputDirPath, String inputExtintion,  File outputDirPath, String outputExtintion) throws IOException {
		if(inputDirPath==null || !inputDirPath.exists() || outputDirPath==null|| !outputDirPath.exists() || inputExtintion==null || outputExtintion==null || inputExtintion.isEmpty() || outputExtintion.isEmpty()) {
			return false;
		}
		ArrayList<File> srcFiles = IoHdlr.getInstance().getListOfFiles(inputDirPath, inputExtintion);
		ArrayList<File> tgtFiles= IoHdlr.getInstance().getListOfFiles(outputDirPath, outputExtintion);
		if(srcFiles.size()==tgtFiles.size()) {
			return true;
		}else {
			return false;
		}		
	}
	

}
