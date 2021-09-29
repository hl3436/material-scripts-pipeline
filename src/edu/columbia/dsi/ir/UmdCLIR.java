/**
 * 
 */
package edu.columbia.dsi.ir;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import edu.columbia.dsi.SCRIPTS.Language;
import edu.columbia.dsi.containers.UmdMatcherConfig;
import edu.columbia.dsi.ir.UmdCLIR.UmdCLIRCombFormat;
import edu.columbia.dsi.mt.Translator.TranslatorEngine;
import edu.columbia.dsi.utils.IoHdlr;
import edu.columbia.dsi.utils.ResourceFactory;
import edu.columbia.dsi.utils.WorkDirHdlr;

/**
 * @author badrashiny
 * May 7, 2018
 */
public class UmdCLIR {
	private static Logger logger = Logger.getLogger(UmdCLIR.class.getSimpleName());
	private ResourceFactory resourceFactory = new ResourceFactory();
	private UmdQueryProcessor umdQueryProcessor=null;
	private UmdQueryMatcher umdQueryMatcher;
	private UmdEvidenceCombination umdEvidenceCombination;
	private UMDReranker umdReranker;
	
	private String umdQPVersion;
	private String umdQMVersion;
	private String umdECVersion;
	private String umdRerankerVersion;

	private WorkDirHdlr workDirHdlr=null;
	private static final String workQPDir="UMD-CLIR-workQPDir";
	private static final String workQMDir="UMD-CLIR-workQMDir";
	
	
	private File outputQPDir=null;
	private File outputECDir=null;
	ArrayList<String>queryMatchedDirecotriesList;
	
	private File mathcerCacheFolder = null;
	private File rankerCacheFolder = null;
	private File cacheFolder=null;
	private boolean cacheOverrideFlag=false;
	
	private boolean qpCopyflag = true; //by default, the QP output will be copied to the CLIR output directory
	
//	public enum UmdCLIRMatchingType {UMDPSQPhraseBasedGVCC, UMDPSQPhraseBasedGVCCStem, UMDPSQPhraseBasedGVCCCutoff097, UMDPSQPhraseBasedGVCCCutoff097Stem, wordsExpandedWord2Vec, wordsExpandedTFIDF, wordsStem, PSQStem,UMDPSQCutoff097, UMDPSQCutoff097Stem, UMDPSQPhraseBased, UMDPSQPhraseBasedStem, 
//									 UMDPSQPhraseBasedCutoff097, UMDPSQPhraseBasedCutoff097Stem, DBQT, DBQTStem, DBQTWiktionaryStemCheck, DBQTWiktionaryStemCheckStem, 
//									 DBQTWiktionaryMerged, DBQTWiktionaryMergedStem, DBQTWiktionaryMergedStemCheck,	DBQTWiktionaryMergedStemCheckStem, words, PSQ, wiktionary;}
	
	
	
	
	

	public enum UmdCLIRCombFormat {tsv, trec, tgz;}

	
	/**
	 * This constructor is to be used with multiple Matcher configurations
	 */
	public UmdCLIR() {
		logger.debug("Initializing the UMD-CLIR...");
		umdQueryProcessor = new UmdQueryProcessor();
		umdQueryMatcher = new UmdQueryMatcher();
		umdEvidenceCombination = new UmdEvidenceCombination();	
		umdReranker = new UMDReranker();
		
		this.umdQPVersion = resourceFactory.getUmdQueryProcessorVersion();
		this.umdQMVersion = resourceFactory.getUmdQueryMatcherVersion();
		this.umdECVersion = resourceFactory.getUmdEvidenceCombinationVersion();
		this.umdRerankerVersion = resourceFactory.getUmdRerankerVersion();
		logger.debug(" UMD-CLIR is ready.");
	}
	
	public UmdCLIR(boolean qpStatus) {
		logger.debug("Initializing the UMD-CLIR...");
		if(qpStatus) {
			umdQueryProcessor = new UmdQueryProcessor();
		}		
		umdQueryMatcher = new UmdQueryMatcher();
		umdEvidenceCombination = new UmdEvidenceCombination();
		umdReranker = new UMDReranker();
		
		this.umdQPVersion = resourceFactory.getUmdQueryProcessorVersion();
		this.umdQMVersion = resourceFactory.getUmdQueryMatcherVersion();
		this.umdECVersion = resourceFactory.getUmdEvidenceCombinationVersion();
		this.umdRerankerVersion = resourceFactory.getUmdRerankerVersion();
		logger.debug(" UMD-CLIR is ready.");
	}
	
	public void setCachingPath(File cacheFolder, boolean overrideFlag) {
		this.cacheOverrideFlag=overrideFlag;
		this.cacheFolder=cacheFolder;
		setMathcerCachingPath(this.cacheFolder);
	}
	
	private void setMathcerCachingPath(File cacheFolder) {
		if(this.cacheFolder!=null) {
			String matcherCachePath=cacheFolder.getAbsolutePath()+File.separator+"Matcher_Cache";
			String rankerCachePath=cacheFolder.getAbsolutePath()+File.separator+"Ranker_Cache";
			File tmp=new File(matcherCachePath);
			if(!tmp.exists()) {
				tmp.mkdirs();
				tmp.setExecutable(true, false);
				tmp.setReadable(true, false);
				tmp.setWritable(true, false);
			}
			
			matcherCachePath+=File.separator+this.umdQMVersion;			
			this.mathcerCacheFolder=new File(matcherCachePath);
			if(!this.mathcerCacheFolder.exists()) {
				this.mathcerCacheFolder.mkdirs();
				this.mathcerCacheFolder.setExecutable(true, false);
				this.mathcerCacheFolder.setReadable(true, false);
				this.mathcerCacheFolder.setWritable(true, false);
			}			
			
			
			tmp=new File(rankerCachePath);
			if(!tmp.exists()) {
				tmp.mkdirs();
				tmp.setExecutable(true, false);
				tmp.setReadable(true, false);
				tmp.setWritable(true, false);
			}
			
			rankerCachePath+=File.separator+this.umdRerankerVersion;
			this.rankerCacheFolder=new File(rankerCachePath);
			if(!this.rankerCacheFolder.exists()) {
				this.rankerCacheFolder.mkdir();
				this.rankerCacheFolder.setExecutable(true, false);
				this.rankerCacheFolder.setReadable(true, false);
				this.rankerCacheFolder.setWritable(true, false);
			}			
		}else {
			this.mathcerCacheFolder=null;
			this.rankerCacheFolder=null;
		}		
	}
	
	public void setQPCopyFlag(boolean qpCopyflag) {
		this.qpCopyflag=qpCopyflag;
	}
	
	public void setUmdQPVersion(String version) {
		this.umdQPVersion=version;
		if(umdQueryProcessor!=null) {
			this.umdQPVersion=version;
			umdQueryProcessor.setVersion(version);
		}
	}
	
	public void setUmdQMVersion(String version) {
		this.umdQMVersion=version;
		umdQueryMatcher.setVersion(version);
		setCachingPath(this.cacheFolder, this.cacheOverrideFlag);
	}
	
	public void setUmdEviCombVersion(String version) {
		this.umdECVersion=version;
		umdEvidenceCombination.setVersion(version);
	}
	
	public void setUmdRerankerVersion(String version) {
		this.umdRerankerVersion=version;
		umdReranker.setVersion(version);
	}
	
	public File getQPOutDir() {
		return this.outputQPDir;
	}
	
	public void setQPOutDir(File outputQPDir) {
		this.outputQPDir=outputQPDir;
	}
	
	public File getECOutDir() {
		return this.outputECDir;
	}
	
	public void setECOutDir(File outputECDir) {
		this.outputECDir=outputECDir;
	}
	
	private void setupWorkDir(ArrayList<UmdMatcherConfig> matcherConfigs) {
		if(workDirHdlr!=null) {
			workDirHdlr.cleanup();
		}	
		
		ArrayList<String>folders=new ArrayList<String>();
		queryMatchedDirecotriesList=new ArrayList<String>();
		folders.add(workQPDir);
		for(int i=0; i<matcherConfigs.size(); i++) {
			String qmDirName=workQMDir+"-"+matcherConfigs.get(i).getConfigName();
			folders.add(qmDirName);
			queryMatchedDirecotriesList.add(qmDirName);
		}		
		workDirHdlr = new WorkDirHdlr(folders);
	}
	/**
	 * Single query call. No multi-threading needed
	 * @param matcherConfigs
	 * @param queryId
	 * @param query
	 * @param domainId
	 * @param tgtLang
	 * @param eviComCutoff
	 * @param eviComFormat
	 * @param outDir
	 */
	public void searchQuery(File configFilePath, ArrayList<UmdMatcherConfig> matcherConfigs, String queryId, String query, String domainId, Language tgtLang, int eviComCutoff, UmdCLIRCombFormat eviComFormat, File outDir) {
		setupWorkDir(matcherConfigs);
		if(umdQueryProcessor.processQuery(queryId, query, domainId, tgtLang, workDirHdlr.getSubFolderHdlr(workQPDir))) {
			getRelevantDocs(tgtLang, matcherConfigs, eviComCutoff, eviComFormat, configFilePath, workDirHdlr.getSubFolderHdlr(workQPDir), outDir);
		}		
	}
	
	/**
	 * 
	 * @param inputQueries: is a tab separated file of all queries that are required to be processed. 
	 * 						Where the 1st column is the queryId, 2nd column is query, and the 3rd column is domainId 
	 * @param tgtLang
	 * @param outDir
	 */
	public void searchQuery(File configFilePath, ArrayList<UmdMatcherConfig> matcherConfigs, ArrayList<File> inputQueriesLists, Language tgtLang, int eviComCutoff, UmdCLIRCombFormat eviComFormat, File outDir) {
		try {
			setupWorkDir(matcherConfigs);
			boolean queriesReady=false;
			for(int i=0;i<inputQueriesLists.size();i++) {
				if(inputQueriesLists.get(i).isDirectory()) {//i.e. a directory that contains a preprocessed queries 
					ArrayList<File> qFiles = IoHdlr.getInstance().getListOfFiles(inputQueriesLists.get(i));
					for(File f:qFiles) {
						String oFile=workDirHdlr.getSubFolderHdlr(workQPDir).getCanonicalPath()+File.separator+f.getName();
						FileUtils.copyFile(f, new File(oFile));
					}
					queriesReady=true;
				}else {
					if(umdQueryProcessor!=null) {
						queriesReady=umdQueryProcessor.processQuery(inputQueriesLists.get(i), tgtLang, workDirHdlr.getSubFolderHdlr(workQPDir));
						if(queriesReady==false) {
							break;		
						}										
					}else {
						queriesReady=false;
						break;
					}
//					queriesReady=runMultiThreadQueryProcessor(inputQueriesLists.get(i), tgtLang);
//					if(queriesReady==false) {
//						break;
//					}
				}
			}
			
			
			if(queriesReady) {
				getRelevantDocs(tgtLang, matcherConfigs, eviComCutoff, eviComFormat, configFilePath, workDirHdlr.getSubFolderHdlr(workQPDir), outDir);
			}
		} catch (IOException e) {
			logger.fatal(e.getMessage());
		}		
	}
	
		
	private void getRelevantDocs(Language tgtLang, ArrayList<UmdMatcherConfig> matcherConfigs, int eviComCutoff, UmdCLIRCombFormat eviComFormat, File configFilePath, File qpOutput, File outDir) {		
		try {
			for(int i=0;i<matcherConfigs.size();i++) {
				ArrayList<String> indexes = new ArrayList<String>();
				indexes.addAll(matcherConfigs.get(i).getMatchingIndexes());
				String matchingType = matcherConfigs.get(i).getMatchingType();
				int matchingCutoff = matcherConfigs.get(i).getMatchingCutOff();
				String matcherName=matcherConfigs.get(i).getConfigName();
				File qmWorkingDir = workDirHdlr.getSubFolderHdlr(queryMatchedDirecotriesList.get(i));

				String mathcerUniqueID=matcherConfigs.get(i).getConfigMatcherUniqueIdentifier();
				File matcherCachePath=null;
				if(mathcerUniqueID!=null && mathcerCacheFolder!=null) {
					matcherCachePath=new File(mathcerCacheFolder.getAbsolutePath()+File.separator+mathcerUniqueID);
				}
				if(matcherCachePath==null || !matcherCachePath.exists() || this.cacheOverrideFlag) {//i.e. no caching or cache doesn't exist or we need to override the old cache 
					boolean matcherValidOutFlag=runMultiThreadQueryMatcher(configFilePath, tgtLang, qpOutput, qmWorkingDir, indexes, matchingType, matchingCutoff, matcherName);
					if(matcherValidOutFlag && matcherCachePath!=null) {//i.e. the cache doesn't exist and we need to copy the output to the cache
						if(this.cacheOverrideFlag && matcherCachePath.exists()) {// i.e override the old matcher
							IoHdlr.getInstance().deleteDir(matcherCachePath);
						}
						copyToCache(qmWorkingDir, matcherCachePath);
					}
				}else {//i.e. the cache exists and we will copy form the cache
					copyFromCache(matcherCachePath, qmWorkingDir);
				}
			}		
			UmdCLIRCombFormat matchingFormat = matcherConfigs.get(0).getMatchingFormat();//TODO: We assume all configs has the same format. If this is not the case, then there is a problem in this step
			File parentQMDir=workDirHdlr.getSubFolderHdlr(queryMatchedDirecotriesList.get(0)).getParentFile();
			
			this.outputECDir=new File(outDir.getAbsolutePath()+File.separator+"UMD-CLIR-workECDir");
			/*
			 * Rerank the matcher output 
			 */			
			if(rankerCacheFolder==null) {//i.e. don't use cache
				umdReranker.rerank(configFilePath, parentQMDir, queryMatchedDirecotriesList);
			}else {//i.e. use cache
				ArrayList<String> notCached = new ArrayList<String>();
				ArrayList<File> rankerCachePaths = new ArrayList<File>();
				for(int i=0;i<matcherConfigs.size();i++) {
					String rankerUniqueID=matcherConfigs.get(i).getConfigRankerUniqueIdentifier();							
					File rankerCachePath=new File(rankerCacheFolder.getAbsolutePath()+File.separator+rankerUniqueID);
					File qmWorkingDir = workDirHdlr.getSubFolderHdlr(queryMatchedDirecotriesList.get(i));
					if(rankerCachePath.exists()) {
						if(this.cacheOverrideFlag) {// i.e override the old cache
							IoHdlr.getInstance().deleteDir(rankerCachePath);							
							notCached.add(queryMatchedDirecotriesList.get(i));
							rankerCachePaths.add(rankerCachePath);
						}else { //i.e. get the cached version
							IoHdlr.getInstance().deleteDir(qmWorkingDir);	
							qmWorkingDir.mkdir();
							copyFromCache(rankerCachePath, qmWorkingDir);
						}						
					}else {
						notCached.add(queryMatchedDirecotriesList.get(i));
						rankerCachePaths.add(rankerCachePath);
					}							
				}
				if(notCached.size()>0) {
					umdReranker.rerank(configFilePath, parentQMDir, notCached);
					for(int i=0;i<notCached.size();i++) {
						File qmWorkingDir = workDirHdlr.getSubFolderHdlr(notCached.get(i));
						copyToCache(qmWorkingDir, rankerCachePaths.get(i));
					}					
				}				
			}
			
			/*
			 * Evidence Combination 
			 */
			runMultiThreadEvidenceCombination(configFilePath, qpOutput, parentQMDir, queryMatchedDirecotriesList, this.outputECDir, eviComCutoff, matchingFormat, eviComFormat);
			/*
			 * Rerank the Evidence Combination output 
			 */
			ArrayList<String>ecDirecotriesList=new ArrayList<String>();
			ecDirecotriesList.add(this.outputECDir.getName());
			umdReranker.rerank(configFilePath, this.outputECDir.getParentFile(), ecDirecotriesList);


			if(qpCopyflag) { //we will copy the QP output to the CLIR output directory only if the qpCopyflag is true 
				this.outputQPDir=new File(outDir.getCanonicalPath()+File.separator+workDirHdlr.getSubFolderHdlr(workQPDir).getName());
				FileUtils.copyDirectory(workDirHdlr.getSubFolderHdlr(workQPDir), this.outputQPDir);
			}

			for(int j=0;j<queryMatchedDirecotriesList.size();j++) {
				File src=workDirHdlr.getSubFolderHdlr(queryMatchedDirecotriesList.get(j));
				FileUtils.copyDirectory(src, new File(outDir.getCanonicalPath()+File.separator+src.getName()));
			}			

		}catch (IOException e) {
			logger.fatal(e.getMessage());
		}	
	}
	
	private void copyToCache(File dirToCache, File cachePath) throws IOException {
		cachePath.mkdirs();
		cachePath.setExecutable(true, false);
		cachePath.setReadable(true, false);
		cachePath.setWritable(true, false);
		ArrayList<File>fList=IoHdlr.getInstance().getListOfFiles(dirToCache);
		for(File f:fList) {
			File cacheFile=new File(cachePath.getCanonicalPath()+File.separator+f.getName());
			FileUtils.copyFile(f, cacheFile);	
			cacheFile.setExecutable(true, false);
			cacheFile.setReadable(true, false);
			cacheFile.setWritable(true, false);
		}
	}
	
	private void copyFromCache(File cachePath, File dirToCopyTo) throws IOException {
		ArrayList<File>cachedOutput=IoHdlr.getInstance().getListOfFiles(cachePath);
		for(File f:cachedOutput) {
			File cFile=new File(dirToCopyTo.getCanonicalPath()+File.separator+f.getName());
			FileUtils.copyFile(f, cFile);							
		}				
	}
	
	private void runMultiThreadEvidenceCombination(File configFilePath, File qpOutput, File parentQMDir, ArrayList<String> queryMatchedDirecotriesList, File outDir, int eviComCutoff, UmdCLIRCombFormat matchingFormat, UmdCLIRCombFormat eviComFormat){
		int firstRunIdx=0;
		int maxConcurrentRuns=resourceFactory.getUmdEvidenceCombinationMaxConcurrentTasks();
//		File queriesDir=new File(parentQMDir.getAbsolutePath()+File.separator+queryMatchedDirecotriesList.get(0));
//		ArrayList<File> queriesNames = IoHdlr.getInstance().getListOfFiles(queriesDir,".tsv");
		
		Hashtable<String, HashSet<File>> queriesNames = IoHdlr.getInstance().getListOfFilesGrouped(qpOutput);
		ExecutorService executorService = Executors.newFixedThreadPool(maxConcurrentRuns);
		ArrayList<Future<?>>openThreads=new ArrayList<Future<?>>();
		int currentRunIdx=firstRunIdx;
		Set<Entry<String, HashSet<File>>> queriesNamesSet = queriesNames.entrySet();
		for(Entry<String, HashSet<File>> qNameSet : queriesNamesSet) {
			if(currentRunIdx>maxConcurrentRuns) {//i.e. we used all of the runs. So, we have to wait until more runs become available and use them
				cleanupOpenThreads(openThreads);
				currentRunIdx=firstRunIdx;
			}				
			openThreads.add(startUmdECThread(executorService, this.umdECVersion, configFilePath, qpOutput, qNameSet.getKey(), parentQMDir, queryMatchedDirecotriesList, outDir, eviComCutoff, matchingFormat, eviComFormat));			
			currentRunIdx++;
		}
		cleanupOpenThreads(openThreads);//get the output from all of the running threads
		executorService.shutdown();	
//		File outLogFile=new File(outDir.getAbsolutePath()+File.separator+"output.log");
//		logger.debug(IoHdlr.getInstance().readFile(outLogFile));
//		outLogFile.delete();
	}
	
	private Future<?> startUmdECThread(ExecutorService executorService, String version, File configFilePath, File qpOutput,  String queryFileName, File parentQMDir, ArrayList<String> queryMatchedDirecotriesList, File outDir, int eviComCutoff, UmdCLIRCombFormat matchingFormat, UmdCLIRCombFormat eviComFormat){
		Future<?> Future = executorService.submit(new Runnable() {
		    public void run() {
		    	UmdEvidenceCombination umdEC= new UmdEvidenceCombination();
		    	umdEC.setVersion(version);
//		    	umdEC.evidenceCombine(queryFileName, parentQMDir, queryMatchedDirecotriesList, outDir, eviComCutoff, matchingFormat, eviComFormat);		
		    	umdEC.evidenceCombine(qpOutput, configFilePath, queryFileName, parentQMDir,queryMatchedDirecotriesList, outDir);	
		    	
		    }
		});	
		return Future;		
	}
	
//	private void runMultiThreadQueryMatcher(File qpOutput, File qmWorkingDir, ArrayList<String> indexes, UmdCLIRMatchingType matchingType, int matchingCutoff, UmdCLIRCombFormat matchingFormat) {
	private boolean runMultiThreadQueryMatcher(File configFilePath, Language tgtLang, File qpOutput, File qmWorkingDir, ArrayList<String> indexes, String matchingType, int matchingCutoff, String matcherName) {
		int firstRunIdx=0;
		int maxConcurrentRuns=resourceFactory.getUmdQueryMatcherMaxConcurrentTasks();
		Hashtable<String, HashSet<File>> queriesNames = IoHdlr.getInstance().getListOfFilesGrouped(qpOutput);
		ExecutorService executorService = Executors.newFixedThreadPool(maxConcurrentRuns);
		ArrayList<Future<?>>openThreads=new ArrayList<Future<?>>();
		int currentRunIdx=firstRunIdx;
		Set<Entry<String, HashSet<File>>> queriesNamesSet = queriesNames.entrySet();
		for(Entry<String, HashSet<File>> qNameSet : queriesNamesSet) {
			if(currentRunIdx>maxConcurrentRuns) {//i.e. we used all of the runs. So, we have to wait until more runs become available and use them
				cleanupOpenThreads(openThreads);
				currentRunIdx=firstRunIdx;
			}				
			openThreads.add(startUmdQMThread(executorService, this.umdQMVersion, configFilePath, tgtLang, qNameSet, qmWorkingDir, indexes, matchingType, matchingCutoff, matcherName));			
			currentRunIdx++;
		}
		cleanupOpenThreads(openThreads);//get the output from all of the running threads
		executorService.shutdown();	
		return isValidMatcherOutput(queriesNames.size(),qmWorkingDir);
		
	}
	
	private Future<?> startUmdQMThread(ExecutorService executorService, String version, File configFilePath,Language tgtLang, Entry<String, HashSet<File>> qNameSet, File outMatchedQueriesDir, ArrayList<String> indexes, String matchingType,  int matchingCutoff, String matcherName){
		Future<?> Future = executorService.submit(new Runnable() {
		    public void run() {
		    	UmdQueryMatcher umdQM= new UmdQueryMatcher();
		    	umdQM.setVersion(version);
		    	umdQM.matchQuery(configFilePath, tgtLang, qNameSet, outMatchedQueriesDir, indexes, matchingType, matchingCutoff, matcherName);
		    	umdQM.shutdown();
		    }
		});	
		return Future;		
	}
	
	private boolean isValidMatcherOutput(int queriesNum, File mathcerOutDir){
		boolean validOut=false;
		ArrayList<File>matcherOutput=IoHdlr.getInstance().getListOfFiles(mathcerOutDir, ".etsv");
		if(matcherOutput.size()==queriesNum) {
			validOut=true;
		}
		return validOut;		
	}
	
	
	private boolean runMultiThreadQueryProcessor(File inputQueries, Language tgtLang) {
		int firstRunIdx=0;
		int maxConcurrentRuns=resourceFactory.getUmdQueryProcessorMaxConcurrentTasks();	
		ArrayList<String>lines=IoHdlr.getInstance().readFileLines(inputQueries);		
		ExecutorService executorService = Executors.newFixedThreadPool(maxConcurrentRuns);
		ArrayList<Future<?>>openThreads=new ArrayList<Future<?>>();
		int currentRunIdx=firstRunIdx;
		
		ArrayList<File>outFiles=new ArrayList<File>();
		
		for(int i=0;i<lines.size();i++) {
			if(i==0 && lines.get(i).trim().replaceAll("\t", " ").equals("query_id query_string domain_id")){//check if it has a header and skip it
				continue;
			}
			String[] tmp=lines.get(i).trim().split("\t");
			if(tmp.length!=3){
				logger.error("Can not process this query line: "+lines.get(i).trim()+" It has incorrect number of columns");
			}else {
				String queryId=tmp[0];
				String query=tmp[1];
				String domainId=tmp[2];
				File expectedOutFile=new File(workDirHdlr.getSubFolderHdlr(workQPDir).getAbsolutePath()+File.separator+queryId);
				outFiles.add(expectedOutFile);
				
				if(currentRunIdx>maxConcurrentRuns) {//i.e. we used all of the runs. So, we have to wait until more runs become available and use them
					cleanupOpenThreads(openThreads);
					currentRunIdx=firstRunIdx;
				}				
				openThreads.add(startUmdQPThread(executorService, this.umdQPVersion, queryId, query, domainId, tgtLang));			
				currentRunIdx++;
			}
		}		
		cleanupOpenThreads(openThreads);//get the output from all of the running threads
		executorService.shutdown();
		
		for(int i=0;i<outFiles.size();i++) {
			if(!outFiles.get(i).exists()) {
				return false;
			}
		}
		return true;		
	}
	
	private Future<?> startUmdQPThread(ExecutorService executorService, String version, String queryId, String query, String domainId, Language tgtLang){
		Future<?> Future = executorService.submit(new Runnable() {
		    public void run() {
		    	UmdQueryProcessor umdQP= new UmdQueryProcessor();
		    	umdQP.setVersion(version);
		    	umdQP.processQuery(queryId, query, domainId, tgtLang, workDirHdlr.getSubFolderHdlr(workQPDir));
		    	umdQP.shutdown();
		    }
		});	
		return Future;
		
	}
	
	private void cleanupOpenThreads(ArrayList<Future<?>>openThreads) {
		for(int k=0;k<openThreads.size();k++) {
			try {
				openThreads.get(k).get();
			} catch (InterruptedException | ExecutionException e) {
				logger.fatal(e.getMessage());
			}					
		}		
	}

		
	
	public String getVersion() {
		return this.umdQPVersion+"_"+this.umdQMVersion+"_"+this.umdECVersion;		
	}
	
	public void shutdown() {
		if(umdQueryProcessor!=null) {
			umdQueryProcessor.shutdown();
		}		
		umdQueryMatcher.shutdown();
		
		if(workDirHdlr!=null) {
			workDirHdlr.cleanup();
		}
	}

}
