/**
 * 
 */
package edu.columbia.dsi.identification;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import edu.columbia.dsi.SCRIPTS.Language;
import edu.columbia.dsi.containers.ProcessingUnit;
import edu.columbia.dsi.utils.IoHdlr;
import edu.columbia.dsi.utils.WorkDirHdlr;

/**
 * @author badrashiny
 * Apr 23, 2018
 */
public class DomainIdentification  extends ProcessingUnit {
	private static Logger logger = Logger.getLogger(DomainIdentification.class.getSimpleName());
	private File domainIdScript;
	private Language lang=null;

	/**
	 * 
	 */
	public DomainIdentification(Language lang) {
		ArrayList<String>versions=resourceFactory.getAllDomainIdVersions(lang);
		if(versions!=null && !versions.isEmpty()) {
			this.domainIdScript=resourceFactory.getDomainIdScript();	
			this.lang=lang;
			this.version=versions.get(0);
			validityFlag=true;
		}else {
			logger.fatal("Unsupported language: "+lang.toString());
			validityFlag=false;
		}
	}
	
	
	public void run(File inputDir, File outputDirPath){
		ArrayList<String>folders=new ArrayList<String>();
		String wrkDirName="domIDwork"+UUID.randomUUID().toString()+ (new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()));
		folders.add(wrkDirName);				
		WorkDirHdlr workDirHdlr = new WorkDirHdlr(folders);
		try {
			logger.debug("Identifying  directory: "+ inputDir.getCanonicalPath());	
			if(isProcessedDir(inputDir, ".txt", outputDirPath, ".csv")) {
				logger.debug("The output directory: "+outputDirPath.getCanonicalPath()+" is not empty. It looks like it is already identified...");
			}else {
				
				File tmpInputDirPath=workDirHdlr.getSubFolderHdlr(wrkDirName);
				ArrayList<File> srcFiles = IoHdlr.getInstance().getListOfFiles(inputDir);
				for(File f:srcFiles) {
					if(f.getCanonicalPath().endsWith(".txt")) {
						String tgtFilePath=tmpInputDirPath.getCanonicalPath()+File.separator+f.getName();
						FileUtils.copyFile(f, new File(tgtFilePath));
					}
				}			
				
				String cmd="sh"+" "+domainIdScript.getCanonicalPath()+" "+tmpInputDirPath.getCanonicalPath()+" "+outputDirPath.getCanonicalPath()+" "+lang.toString()+" "+version;
				logger.debug(cmd);
				Process process = Runtime.getRuntime().exec(cmd);
				exportStdOutputLog(process, logger);
				process.waitFor();
				logger.debug("Validating identification output...");
				File outTempDir=new File(outputDirPath.getCanonicalPath()+File.separator+"Temp");
				File outLogFile=new File(outputDirPath.getCanonicalPath()+File.separator+"output.log");	
				File outLogWekaFile=new File(outputDirPath.getCanonicalPath()+File.separator+"output-weka.log");	
				File outLogNNFile=new File(outputDirPath.getCanonicalPath()+File.separator+"output-nn.log");	
				if(outTempDir.exists()) {
					IoHdlr.getInstance().deleteDir(outTempDir);
				}
				if(outLogFile.exists()) {
					logger.debug(IoHdlr.getInstance().readFile(outLogFile));
					outLogFile.delete();
				}				
				if(outLogWekaFile.exists()) {
					logger.debug(IoHdlr.getInstance().readFile(outLogWekaFile));
					outLogWekaFile.delete();
				}
				if(outLogNNFile.exists()) {
					logger.debug(IoHdlr.getInstance().readFile(outLogNNFile));
					outLogNNFile.delete();
				}
				if(isProcessedDir(inputDir, ".txt", outputDirPath, ".csv")) {
					logger.debug(inputDir.getCanonicalPath()+ " is identified in: "+ outputDirPath.getCanonicalPath());
				}else {
					logger.error(inputDir.getCanonicalPath()+ " is not completely identified. Please check the error messages.");
				}	
			}		
		} catch (IOException | InterruptedException e) {
			logger.error(e.getMessage());
		}
		workDirHdlr.cleanup();
	}
	
	
	protected boolean isProcessedDir(File inputDirPath, String inputExtintion,  File outputDirPath, String outputExtintion) throws IOException {
		ArrayList<File> srcFiles = IoHdlr.getInstance().getListOfFiles(inputDirPath, inputExtintion);
		File classifications=new File(outputDirPath.getCanonicalPath()+File.separator+"domain-modeling-output"+outputExtintion);
		if(classifications.exists()) {
			ArrayList<String>outFiles=IoHdlr.getInstance().readFileLinesWithoutHeader(classifications);
			if(outFiles.size()==srcFiles.size()) {
				return true;
			}else {
				return false;
			}			
		}else {
			return false;
		}		
	}
}
