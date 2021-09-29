package edu.columbia.dsi.mt;

import java.io.File;
import java.util.Arrays;

import org.apache.log4j.Logger;

import edu.columbia.dsi.SCRIPTS.Language;
import edu.columbia.dsi.containers.DocInfo.Type;
import edu.columbia.dsi.mt.Translator.TranslatorMode;
import edu.columbia.dsi.containers.ProcessingUnit;
import edu.columbia.dsi.utils.ResourceFactory;

/**
 * @author badrashiny
 * Mar 8, 2018
 */
public class Translator extends ProcessingUnit{
	private static Logger logger = Logger.getLogger(Translator.class.getSimpleName());
	public enum TranslatorEngine {OLDEDINMT, EDINMT, UMDNMT, UMDSMT, UMDNNLTM, UMDNMTPSQ;}
	public enum TranslatorMode {fast, accurate;}
	private EdiNMTDeprecated oldEdiNMT=null;
	private EdiNMT ediNMT=null;
	private UmdNMT umdNMT=null;
	private UmdSMT umdSMT=null;
	private UmdNNltm umdNNltm=null;
	private UmdNMTPsq umdNMTPsq=null;
	private TranslatorEngine engine = TranslatorEngine.EDINMT;
	private String sourceFileExtension=".txt";
	private int[] gpuIdx;
	private ResourceFactory resourceFactory = new ResourceFactory();	
	
	public Translator(TranslatorEngine engine, Language src, Language tgt, Type type, TranslatorMode mode, String sourceFileExtension, int[] gpuIdx) {
		logger.debug("Loading translator engine ...");
		this.engine=engine;
		this.sourceFileExtension=sourceFileExtension;
		this.gpuIdx=Arrays.copyOf(gpuIdx, gpuIdx.length);
		shutdown();//to shutdown any running version
		version=null;
		if(engine==TranslatorEngine.OLDEDINMT) {			
			oldEdiNMT=new EdiNMTDeprecated(src, tgt, type, mode, this.gpuIdx);
			if(oldEdiNMT.isValid()) {
				version=oldEdiNMT.getVersion();
			}
		}else if(engine==TranslatorEngine.EDINMT) {
			ediNMT=new EdiNMT(src, tgt, type, mode, this.gpuIdx); 
			version=ediNMT.getVersion();
		}else if(engine==TranslatorEngine.UMDNMT) {
			int threads=Math.max(1, resourceFactory.getRamPerGPU()/11);//the UMD NMT needs 11 GB for each thread
			umdNMT=new UmdNMT(src, tgt, type, this.gpuIdx, threads); 
			version=umdNMT.getVersion();
		}else if(engine==TranslatorEngine.UMDSMT) {
			int threads=Math.min(24, Runtime.getRuntime().availableProcessors());// No improvement in the UMD-SMT system beyond 24 threads
			umdSMT=new UmdSMT(src, tgt, type, threads); 
			version=umdSMT.getVersion();
		}else if(engine==TranslatorEngine.UMDNNLTM) {
			int threads=Math.max(1, resourceFactory.getRamPerGPU()/5);//the UMD NNltm needs 5 GB for each thread
			umdNNltm=new UmdNNltm(src, type, this.gpuIdx, threads); 
			version=umdNNltm.getVersion();
		}else if(engine==TranslatorEngine.UMDNMTPSQ) {
			int threads=Math.max(1, resourceFactory.getRamPerGPU()/8);//the UMD NMT PSQ needs 8 GB for each thread
			umdNMTPsq=new UmdNMTPsq(src, tgt, type, this.gpuIdx, threads); 
			version=umdNMTPsq.getVersion();
		}else {
			logger.error("Unsupported translation engine "+engine.toString());
			validityFlag=false;
		}
		
		if(version!=null) {
			validityFlag=true;
			logger.debug("Translator engine "+engine.toString()+" is loaded successfully...");
		}else {
			logger.error("Couldn't initlaize the translation engine "+engine.toString());
			validityFlag=false;
		}
	}
	
	public void run(File sourceDirPath, File targetDirPath){	
		boolean isNoError=true;
		if(engine==TranslatorEngine.OLDEDINMT) {
//			isNoError=oldEdiNMT.translateDir(sourceDirPath, targetDirPath, sourceFileExtension);
			isNoError=oldEdiNMT.translateDirMultiThred(sourceDirPath, targetDirPath, sourceFileExtension);
		}else if(engine==TranslatorEngine.EDINMT) {
			isNoError=ediNMT.translateDir(sourceDirPath, targetDirPath);
		}else if(engine==TranslatorEngine.UMDNMT) {
			isNoError=umdNMT.translateDir(sourceDirPath, targetDirPath);
		}else if(engine==TranslatorEngine.UMDSMT) {
			isNoError=umdSMT.translateDir(sourceDirPath, targetDirPath);
		}else if(engine==TranslatorEngine.UMDNNLTM) {
			isNoError=umdNNltm.translateDir(sourceDirPath, targetDirPath);
		}else if(engine==TranslatorEngine.UMDNMTPSQ) {
			isNoError=umdNMTPsq.translateDir(sourceDirPath, targetDirPath);
		}
		outputValidityFlag=isNoError;
	}
		
	public void shutdown() {
		if(oldEdiNMT!=null) {
			oldEdiNMT.shutdown();
		} 
	}	
}
