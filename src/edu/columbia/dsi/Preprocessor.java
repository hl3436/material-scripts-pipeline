/**
 * 
 */
package edu.columbia.dsi;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import edu.columbia.dsi.SCRIPTS.Language;
import edu.columbia.dsi.asr.ASR;
import edu.columbia.dsi.asr.KWS;
import edu.columbia.dsi.containers.Corpus;
import edu.columbia.dsi.containers.Query;
import edu.columbia.dsi.containers.Component;
import edu.columbia.dsi.containers.Component.CompNames;
import edu.columbia.dsi.containers.DocInfo.Type;
import edu.columbia.dsi.containers.ProcessingUnit;
import edu.columbia.dsi.identification.AudioLanguageIdentification;
import edu.columbia.dsi.identification.DomainIdentification;
import edu.columbia.dsi.identification.LanguageIdentification;
import edu.columbia.dsi.ir.UmdQueryProcessor;
import edu.columbia.dsi.morphology.MorphologicalAnalyzer;
import edu.columbia.dsi.morphology.Stemmer;
import edu.columbia.dsi.mt.MTPostedit;
import edu.columbia.dsi.mt.Translator;
import edu.columbia.dsi.mt.Translator.TranslatorEngine;
import edu.columbia.dsi.mt.Translator.TranslatorMode;
import edu.columbia.dsi.utils.AudioSentenceSplitter;
import edu.columbia.dsi.utils.IoHdlr;
import edu.columbia.dsi.utils.ResourceFactory;
import edu.columbia.dsi.utils.SentenceSplitter;

/**
 * @author badrashiny Sep 17, 2019
 */
public class Preprocessor {
	private static Logger logger = Logger.getLogger(Preprocessor.class.getSimpleName());

	private ResourceFactory resourceFactory = new ResourceFactory();
	private int availableGPUs;
	private int cpuRam;
	private Hashtable<Language, ArrayList<Corpus>> textCorpora = new Hashtable<Language, ArrayList<Corpus>>();
	private Hashtable<Language, ArrayList<Corpus>> audioCorpora = new Hashtable<Language, ArrayList<Corpus>>();
	private Hashtable<Language, ArrayList<Corpus>> mtMissedCorpora = new Hashtable<Language, ArrayList<Corpus>>();
	private ArrayList<Query> queries = new ArrayList<Query>();
	Hashtable<Language, File> langToQPOutMap = new Hashtable<Language, File>();
	private boolean validityFlag = false;

	/**
	 * 
	 */
	public Preprocessor(ArrayList<Corpus> corpora, ArrayList<Query> queries) {
		this.queries.addAll(queries);
		this.availableGPUs = resourceFactory.getAvailableGpuNumber();
		this.cpuRam = resourceFactory.getRamPerCPU();
		if (this.cpuRam < 100) {
			logger.fatal("Insuffecint memory. They system minimum requiremnts is 100 GB of RAM. Only "
					+ String.valueOf(this.cpuRam) + " are detected.");
			System.out.println("ERROR: insuffecint memory. The system minimum requiremnts is 100 GB of RAM. Only "
					+ String.valueOf(this.cpuRam) + " are detected.");
			System.exit(0);
		}

		for (Query q : this.queries) {
			langToQPOutMap.put(q.getQueriesLang(), q.getQPVersionDir());
		}

		if (this.availableGPUs > 0) {
			logger.debug("Initalizing the preprocessor ...");

			for (int j = 0; j < corpora.size(); j++) {
				Corpus corpus = corpora.get(j);
				Language lang = corpus.getLanguage();
				if (corpus.getType() == Type.audio) {
					ArrayList<Corpus> corpTmp;
					if (audioCorpora.containsKey(lang)) {
						corpTmp = audioCorpora.get(lang);
					} else {
						corpTmp = new ArrayList<Corpus>();
					}
					corpTmp.add(corpus);
					audioCorpora.put(lang, corpTmp);
				} else if (corpus.getType() == Type.text) {
					ArrayList<Corpus> corpTmp;
					if (textCorpora.containsKey(lang)) {
						corpTmp = textCorpora.get(lang);
					} else {
						corpTmp = new ArrayList<Corpus>();
					}
					corpTmp.add(corpus);
					textCorpora.put(lang, corpTmp);

				} else {
					logger.error("Unsupported type: " + corpus.getType().toString() + " for this corpus: "
							+ corpus.getSourceLocation().getAbsolutePath());
				}
			}
			validityFlag = true;
		} else {
			System.out.println("The system needs at least one GPU. The provided number is insufficient:  "
					+ String.valueOf(this.availableGPUs));
			logger.fatal("The system needs at least one GPU. The provided number is insufficient:  "
					+ String.valueOf(this.availableGPUs));
			validityFlag = false;
		}
	}

	public boolean isValid() {
		return validityFlag;
	}

	public void process() {
		logger.debug("Processing the input corpora...");
		runQueryProcessor(queries);

		runSentSplitter(textCorpora);// sentence splitter is only for the text input. It doesn't work with the ASR
										// output
		runLangId(textCorpora);// Language Identification is only for the text input. It doesn't work with the
								// ASR output

		int maxThreads = Runtime.getRuntime().availableProcessors();
		ExecutorService executorService = Executors.newFixedThreadPool(maxThreads);

		ArrayList<Future<?>> openThreads = new ArrayList<Future<?>>();

		int asrRamGB = this.cpuRam;
		int asrCPUs = maxThreads;
		if (textCorpora.size() > 0) {
			asrRamGB = asrRamGB - 50;// 50 GB are kept to run the MT on the text corpora while running the ASR on the
										// audio corpora
		}
		if (audioCorpora.size() > 0) {
			// We assume the RAM and CPU must be greater than the number of the languages
			// that are required to be processed
			if (asrCPUs >= audioCorpora.size() && asrRamGB >= audioCorpora.size()) {
				asrRamGB = asrRamGB / audioCorpora.size();// we split the available RAM between all the languages we
															// have
				asrCPUs = asrCPUs / audioCorpora.size();// we split the available CPUs between all the languages we have
			} else {
				logger.fatal(
						"ERROR: Insuffecint memory or CPUs. The RAM size and the number of CPUs must be greater than the number of corpora that are required to be processed");
				System.out.println(
						"ERROR: Insuffecint memory or CPUs. The RAM size and the number of CPUs must be greater than the number of corpora that are required to be processed");
				System.exit(0);
			}
		}

		int asrNumberOfThreads = Math.min(4, asrCPUs);
		int asrNumberOfJobs = asrCPUs / asrNumberOfThreads;

		if ((asrCPUs * 15) > asrRamGB) {// there are 15 GB per one job
			// We assume that numProcessors == 2^n
			// We floor maxNumberOfJobs to the closest power of 2 and scale asrThreads
			// accordingly
			asrNumberOfJobs = 1 << (int) (Math.log(asrRamGB / 15) / Math.log(2));
			asrNumberOfThreads = maxThreads / asrNumberOfJobs;
		}

		openThreads.addAll(runMultiThreadTask(executorService, audioCorpora, CompNames.asr, asrNumberOfJobs,
				asrNumberOfThreads, false));// it will run the ASR and the ASR document expansion, ASR LangID
		openThreads.addAll(runMultiThreadTask(executorService, textCorpora, CompNames.mt, -1, -1, true)); // gpu mt
		openThreads.addAll(runMultiThreadTask(executorService, textCorpora, CompNames.mt, -1, -1, false)); // cpu mt

		cleanupOpenThreads(openThreads);// get the output from all of the running threads

		/**
		 * Validating the MT output
		 */
		// logger.debug("validating edi-NMT output...");
		// int[]gpuIdxs=new int[availableGPUs];
		// for(int h=0;h<availableGPUs;h++) {
		// gpuIdxs[h]=h;
		// }
		Hashtable<Language, ArrayList<Corpus>> notFullyTranslatedCorpora = new Hashtable<Language, ArrayList<Corpus>>();
		// Hashtable<Language,ArrayList<Corpus>> tmp=new
		// Hashtable<Language,ArrayList<Corpus>>();
		// tmp.putAll(mtMissedCorpora);
		// mtMissedCorpora=new Hashtable<Language,ArrayList<Corpus>>();
		// for(Entry<Language, ArrayList<Corpus>> entry : tmp.entrySet()) {// We will
		// try one more time in case it was a memory issue
		// ArrayList<Corpus> corpTmp=entry.getValue();
		// runEngineTranslation(TranslatorEngine.EDINMT, corpTmp, gpuIdxs);
		// }
		notFullyTranslatedCorpora.putAll(mtMissedCorpora);
		mtMissedCorpora = new Hashtable<Language, ArrayList<Corpus>>();
		logger.debug("Prallel ASR and MT threads are done...");

		openThreads = new ArrayList<Future<?>>();
		openThreads.addAll(runMultiThreadTask(executorService, audioCorpora, CompNames.audSentSp, -1, -1, false)); // Run the audio sentence splitter on the ASR output
		openThreads.addAll(runMultiThreadTask(executorService, audioCorpora, CompNames.kws, -1, -1, false)); // Run the KWS on the ASR output
		cleanupOpenThreads(openThreads);// get the output from all of the running threads

		openThreads = new ArrayList<Future<?>>();
		openThreads.addAll(runMultiThreadTask(executorService, audioCorpora, CompNames.audLangId, -1, -1, false)); // Run the audio languageID on the ASR output
		cleanupOpenThreads(openThreads);// get the output from all of the running threads

		logger.debug("translating and morphologically analyzing ASR output if there is any...");
		openThreads = new ArrayList<Future<?>>();
		openThreads.addAll(runMultiThreadTask(executorService, audioCorpora, CompNames.mt, -1, -1, true)); // gpu mt
		openThreads.addAll(runMultiThreadTask(executorService, audioCorpora, CompNames.mt, -1, -1, false)); // cpu mt

		cleanupOpenThreads(openThreads);// get the output from all of the running threads

		/**
		 * Validating the MT output
		 */
		// logger.debug("validating marianNMT output...");
		// tmp=new Hashtable<Language,ArrayList<Corpus>>();
		// tmp.putAll(mtMissedCorpora);
		// mtMissedCorpora=new Hashtable<Language,ArrayList<Corpus>>();
		// for(Entry<Language, ArrayList<Corpus>> entry : tmp.entrySet()) {// We will
		// try one more time in case it was a memory issue
		// ArrayList<Corpus> corpTmp=entry.getValue();
		// runEngineTranslation(TranslatorEngine.EDINMT, corpTmp, gpuIdxs);
		// }

		for (Entry<Language, ArrayList<Corpus>> entry : mtMissedCorpora.entrySet()) {// in case there are some are still
																						// not translated
			ArrayList<Corpus> corpTmp;
			if (notFullyTranslatedCorpora.containsKey(entry.getKey())) {
				corpTmp = notFullyTranslatedCorpora.get(entry.getKey());
			} else {
				corpTmp = new ArrayList<Corpus>();
			}
			corpTmp.addAll(entry.getValue());
			notFullyTranslatedCorpora.put(entry.getKey(), corpTmp);
		}

		openThreads = new ArrayList<Future<?>>();
		openThreads.addAll(runMultiThreadTask(executorService, textCorpora, CompNames.morph, -1, -1, false)); // Run the morphological analyzer on the MT output and the sentence splitter output of the text corpora
		openThreads.addAll(runMultiThreadTask(executorService, audioCorpora, CompNames.morph, -1, -1, false));// Run the morphological analyzer on the MT output and the asr output of the audio corpora
		cleanupOpenThreads(openThreads);// get the output from all of the running threads

		openThreads = new ArrayList<Future<?>>();
		openThreads.addAll(runMultiThreadTask(executorService, textCorpora, CompNames.postEd, -1, -1, false));// Run the MT-Postedit script on the MT output of the text corpora
		cleanupOpenThreads(openThreads);// get the output from all of the running threads
		openThreads = new ArrayList<Future<?>>();
		openThreads.addAll(runMultiThreadTask(executorService, audioCorpora, CompNames.postEd, -1, -1, false));// Run the MT-Postedit script on the MT output of the audio corpora
		cleanupOpenThreads(openThreads);// get the output from all of the running threads

		openThreads = new ArrayList<Future<?>>();
		openThreads.addAll(runMultiThreadTask(executorService, textCorpora, CompNames.stmr, -1, -1, false));// stemming the MT output of the text corpora aftet postEdit
		openThreads.addAll(runMultiThreadTask(executorService, audioCorpora, CompNames.stmr, -1, -1, false));// stemming the MT output of the audio corpora aftet postEdit
		cleanupOpenThreads(openThreads);// get the output from all of the running threads

		openThreads = new ArrayList<Future<?>>();
		openThreads.addAll(runMultiThreadTask(executorService, textCorpora, CompNames.domId, -1, -1, false));// domain identification for the MT output of the text corpora
		openThreads.addAll(runMultiThreadTask(executorService, audioCorpora, CompNames.domId, -1, -1, false));// domain identification for the MT output of the audio corpora

		cleanupOpenThreads(openThreads);// get the output from all of the running threads
		executorService.shutdown();

		logger.debug("All supported ASR and Text inputs have been translated and morphologically analyzed...");

		if (!notFullyTranslatedCorpora.isEmpty()) {
			logger.error(
					"Except the follwing corpora have some files could not be translated by one or more MT systems. If the corpus name is duplicated, this means one from text and one from audio collections");
			for (Entry<Language, ArrayList<Corpus>> entry : notFullyTranslatedCorpora.entrySet()) {
				Language lang = entry.getKey();
				ArrayList<Corpus> corpTmp = entry.getValue();
				for (Corpus c : corpTmp) {
					logger.error(lang.toString() + ": " + c.getCorpusName());
				}
			}
		}
	}

	private void cleanupOpenThreads(ArrayList<Future<?>> openThreads) {
		for (int k = 0; k < openThreads.size(); k++) {
			try {
				openThreads.get(k).get();
			} catch (InterruptedException | ExecutionException e) {
				logger.fatal(e.getMessage());
			}
		}
	}

	/**
	 * 
	 * @param executorService
	 * @param corpora
	 * @param task
	 * @param maxNumOfJobs:   some tasks requires a predefined number of jobs like
	 *                        the ASR. This value should be -1 for the rest of tasks
	 * @param useGPU
	 * @return
	 */
	private ArrayList<Future<?>> runMultiThreadTask(ExecutorService executorService,
			Hashtable<Language, ArrayList<Corpus>> corpora, CompNames task, int maxNumOfJobs, int maxNumOfThreads,
			boolean useGPU) {
		ArrayList<Future<?>> openThreads = new ArrayList<Future<?>>();
		int availableGPUsTmp = this.availableGPUs;
		int gpuPerLang = 1;
		if (corpora.size() > 0) {
			gpuPerLang = this.availableGPUs / corpora.size();
		}
		for (Entry<Language, ArrayList<Corpus>> entry : corpora.entrySet()) {
			ArrayList<Corpus> corpTmp = entry.getValue();
			if (!useGPU) {// i.e the task is asr, audio sentence splitter, morph, postedit, stemmer, or
							// cpu MT
				openThreads.add(startThread(executorService, corpTmp, task, maxNumOfJobs, maxNumOfThreads, null));
			} else if (task == CompNames.mt && useGPU) { // i.e the task is gpu MT
				if (availableGPUsTmp >= gpuPerLang) {
					int[] gpuIdxs = new int[gpuPerLang];
					for (int m = 0; m < gpuIdxs.length; m++) {
						int gpuIdx = availableGPUsTmp - 1; // -1 because the gpu index is zero based
						availableGPUsTmp--;
						gpuIdxs[m] = gpuIdx;
					}
					openThreads
							.add(startThread(executorService, corpTmp, task, maxNumOfJobs, maxNumOfThreads, gpuIdxs));
				} else {// i.e. the number of languages > the number of GPUs. So, we have to wait until
						// the GPUs become available and use them
					cleanupOpenThreads(openThreads);
					availableGPUsTmp = this.availableGPUs;
					openThreads = new ArrayList<Future<?>>();
					int[] gpuIdxs = new int[gpuPerLang];
					for (int m = 0; m < gpuIdxs.length; m++) {
						int gpuIdx = availableGPUsTmp - 1; // -1 because the gpu index is zero based
						availableGPUsTmp--;
						gpuIdxs[m] = gpuIdx;
					}
					openThreads
							.add(startThread(executorService, corpTmp, task, maxNumOfJobs, maxNumOfThreads, gpuIdxs));
				}
			}
		}
		return openThreads;
	}

	private Future<?> startThread(ExecutorService executorService, ArrayList<Corpus> corp, CompNames task,
			int maxNumOfJobs, int maxNumOfThreads, int[] gpuIdx) {
		ArrayList<Corpus> corpTmp = corp;
		CompNames taskTmp = task;
		int maxNumOfJobsTmp = maxNumOfJobs;
		int maxNumOfThreadsTmp = maxNumOfThreads;
		int[] gpuIdxTmp = null;
		boolean cpuMTTmp = true;// i.e. use cpu mt in mt task
		if (gpuIdx != null) {// i.e. use gpu for mt task
			gpuIdxTmp = Arrays.copyOf(gpuIdx, gpuIdx.length);
			cpuMTTmp = false; // i.e. use gpu for mt task
		} else {
			gpuIdxTmp = new int[1];// just a place holder
			gpuIdxTmp[0] = 1;
		}
		int[] gpuIdxTh = Arrays.copyOf(gpuIdxTmp, gpuIdxTmp.length);
		boolean cpuMTTh = cpuMTTmp;
		Future<?> future = executorService.submit(new Runnable() {
			public void run() {
				if (taskTmp == CompNames.mt) {
					if (cpuMTTh) {// i.e. use cpu mt
						runEngineTranslation(TranslatorEngine.UMDSMT, corpTmp, gpuIdxTh);
					} else {// i.e. use gpu mt
						runEngineTranslation(TranslatorEngine.EDINMT, corpTmp, gpuIdxTh);
						runEngineTranslation(TranslatorEngine.UMDNMT, corpTmp, gpuIdxTh);
						runEngineTranslation(TranslatorEngine.UMDNNLTM, corpTmp, gpuIdxTh);
						runEngineTranslation(TranslatorEngine.UMDNMTPSQ, corpTmp, gpuIdxTh);
					}
				} else {// all other tasks
					runProcessingUnit(corpTmp, taskTmp, maxNumOfJobsTmp, maxNumOfThreadsTmp);
				}
			}
		});
		return future;
	}

	private void runProcessingUnit(ArrayList<Corpus> corp, CompNames comp, int maxNumOfJobsTmp,
			int maxNumOfThreadsTmp) {
		for (int j = 0; j < corp.size(); j++) {
			Component pipelineComp = corp.get(j).getComponent(comp);
			Hashtable<String, String> verToShortLocationMap = new Hashtable<String, String>();
			if (pipelineComp != null) {
				verToShortLocationMap = pipelineComp.getVerToShortLocationMap();
			}
			for (Entry<String, String> e : verToShortLocationMap.entrySet()) {
				File outDir = pipelineComp.getOutDirectoryPath(e.getKey());
				File sourceDir = pipelineComp.getSourceDirectoryPath(e.getKey());
				ProcessingUnit unit = null;
				Language lang = null;
				if (comp == CompNames.audSentSp) { // if the current corpus is text, pipelineComp=null for
													// CompNames.audSentSp and the verToShortLocationMap will be empty.
													// So, we don't need to check if the corpus is audio or not
					lang = corp.get(j).getLanguage();
					unit = new AudioSentenceSplitter(lang);
				} else if (comp == CompNames.morph) {
					lang = pipelineComp.getSourceDirectoryLanguage(e.getKey());
					unit = new MorphologicalAnalyzer(lang);
				} else if (comp == CompNames.stmr) {
					unit = new Stemmer();
				} else if (comp == CompNames.postEd) {
					// lang=corp.get(j).getLanguage();
					// unit=new MTPostedit(lang);
					unit = new MTPostedit();
				} else if (comp == CompNames.domId) {
					lang = corp.get(j).getLanguage();
					unit = new DomainIdentification(lang);
				} else if (comp == CompNames.asr) {
					lang = corp.get(j).getLanguage();
					File metaDataPath = corp.get(j).getMetaDataPath();
					unit = new ASR(lang, metaDataPath, maxNumOfJobsTmp, maxNumOfThreadsTmp);
				} else if (comp == CompNames.kws) {
					lang = corp.get(j).getLanguage();
					unit = new KWS(lang);
				} else if (comp == CompNames.audLangId) {
					lang = corp.get(j).getLanguage();
					File metaDataPath = corp.get(j).getMetaDataPath();
					unit = new AudioLanguageIdentification(lang, metaDataPath);
				}
				if (unit != null && unit.isValid() && sourceDir != null && outDir != null) {
					unit.run(sourceDir, outDir);
				} else {
					logger.error(
							"Source direcotry or the output direcotry is missing or language is not supported for this component: Source= "
									+ sourceDir + " Output= " + outDir + " Language= " + lang.toString()
									+ " Component= " + comp.toString());
				}
				unit.shutdown();
			}
		}
	}

	private void runEngineTranslation(TranslatorEngine engine, ArrayList<Corpus> corp, int[] gpuIdx) {
		for (int i = 0; i < corp.size(); i++) {
			Type type = corp.get(i).getType();
			Language lang = corp.get(i).getLanguage();
			String mtVersion = null;
			if (engine == TranslatorEngine.EDINMT) {
				mtVersion = resourceFactory.getEdiNMTVersion(lang, type);
			} else if (engine == TranslatorEngine.UMDNMT) {
				mtVersion = resourceFactory.getUmdNMTVersion(lang, type);
			} else if (engine == TranslatorEngine.UMDSMT) {
				mtVersion = resourceFactory.getUmdSMTVersion(lang, type);
			} else if (engine == TranslatorEngine.UMDNNLTM) {
				mtVersion = resourceFactory.getUmdnnltmVersion(lang, type);
			} else if (engine == TranslatorEngine.UMDNMTPSQ) {
				mtVersion = resourceFactory.getUmdNMTPsqVersion(lang, type);
			}

			Component pipelineComp = corp.get(i).getComponent(CompNames.mt);
			Hashtable<String, String> verToShortLocationMap = new Hashtable<String, String>();
			if (pipelineComp != null) {
				verToShortLocationMap = pipelineComp.getVerToShortLocationMap(mtVersion);
			}

			for (Entry<String, String> e : verToShortLocationMap.entrySet()) {
				File mtOutDir = pipelineComp.getOutDirectoryPath(e.getKey());
				File mtSourceDir = pipelineComp.getSourceDirectoryPath(e.getKey());

				ProcessingUnit translatorTmp = new Translator(engine, lang, Language.en, type, TranslatorMode.accurate,
						".txt", gpuIdx);
				boolean noErrors = true;
				if (translatorTmp.isValid() && mtSourceDir != null && mtOutDir != null) {
					translatorTmp.run(mtSourceDir, mtOutDir);
					boolean noErrorAccurateMode = translatorTmp.isCorrectOutput();
					if (!noErrorAccurateMode) {
						noErrors = false;
					}
				} else {
					logger.error("Source direcotry or the output direcotry is missing: Source= " + mtSourceDir
							+ " Output= " + mtOutDir);
				}
				translatorTmp.shutdown();

				/**
				 * The CLIR team don't want to translate the .rest.bst files anymore
				 */
				// CompNames parentName=pipelineComp.getParentName(e.getKey());
				// if(parentName==CompNames.audSentSp && engine==TranslatorEngine.EDINMT) {
				// translatorTmp= new Translator(engine, lang, Language.en, type,
				// TranslatorMode.fast, ".rest.bst", gpuIdx);
				// if(translatorTmp.isValid() && mtSourceDir!=null && mtOutDir!=null) {
				// translatorTmp.run(mtSourceDir, mtOutDir);
				// boolean noErrorFastMode=translatorTmp.isCorrectOutput();
				// if(!noErrorFastMode) {
				// noErrors=false;
				// }
				// }else {
				// logger.error("Source directory or the output directory is missing: Source=
				// "+mtSourceDir+" Output= "+mtOutDir);
				// }
				// translatorTmp.shutdown();
				// }
				//
				//
				// if(engine==TranslatorEngine.EDINMT) {// this happens only with Edi-NMT
				// if(noErrors) {
				// IoHdlr.getInstance().mergeFilesWithEmptyLinesInBetween(mtOutDir, ".txt",
				// ".rest.bst", ".all");// this will merge the .txt and .rest.bst files only if
				// the .rest.bst exist. Otherwise, nothing will happen
				// }
				// }
				//
				// if(engine==TranslatorEngine.EDINMT) {// this happens only with Edi-NMT
				// if(!noErrors) {
				// ArrayList<Corpus> corpTmp;
				// if(mtMissedCorpora.containsKey(lang)) {
				// corpTmp=mtMissedCorpora.get(lang);
				// }else {
				// corpTmp=new ArrayList<Corpus>();
				// }
				// corpTmp.add(corp.get(i));
				// mtMissedCorpora.put(lang, corpTmp);
				// }
				// }

				if (!noErrors) {
					ArrayList<Corpus> corpTmp;
					if (mtMissedCorpora.containsKey(lang)) {
						corpTmp = mtMissedCorpora.get(lang);
					} else {
						corpTmp = new ArrayList<Corpus>();
					}
					corpTmp.add(corp.get(i));
					mtMissedCorpora.put(lang, corpTmp);
				}
			}
		}
	}

	private void runQueryProcessor(ArrayList<Query> queriesLists) {
		for (Query queryList : queriesLists) {
			File inputList = queryList.getInputQueriesList();
			Language lang = queryList.getQueriesLang();
			File outDir = queryList.getOutQueryProcessingDir();
			if (areQueriesProcessed(inputList, outDir)) {
				logger.debug(inputList.getAbsolutePath() + " is already processed.");
			} else {
				logger.debug("Running the queryProcessor on: " + inputList.getAbsolutePath());
				UmdQueryProcessor umdQP = new UmdQueryProcessor();
				umdQP.processQuery(inputList, lang, outDir);
				umdQP.shutdown();
			}
		}
	}

	private boolean areQueriesProcessed(File inputQueries, File outDir) {
		boolean processed = true;
		ArrayList<File> processedFiles = IoHdlr.getInstance().getListOfFiles(outDir);
		ArrayList<String> queries = IoHdlr.getInstance().readFileLines(inputQueries);
		int numQueries = queries.size();
		if (queries.get(0).trim().replaceAll("\t", " ").equals("query_id query_string domain_id")
				|| queries.get(0).trim().replaceAll("\t", " ").equals("query_id query_string")) {// check if it has a header
			numQueries--;
		}
		// if(numQueries==processedFiles.size()) {
		if (processedFiles.size() >= numQueries && processedFiles.size() % numQueries == 0) { // the QA output can be one or more files per each input query
			// for(File f:processedFiles) {
			// if(IoHdlr.getInstance().readFile(f).trim().isEmpty()) {
			// processed=false;
			// break;
			// }
			// }
			processed = true;
		} else {
			processed = false;
		}

		return processed;
	}

	private void runSentSplitter(Hashtable<Language, ArrayList<Corpus>> corp) {
		for (Entry<Language, ArrayList<Corpus>> entry : corp.entrySet()) {
			ArrayList<Corpus> corpTmp = entry.getValue();
			SentenceSplitter sentenceSplitter = new SentenceSplitter();
			for (int j = 0; j < corpTmp.size(); j++) {
				if (corpTmp.get(j).getType() == Type.text) {
					Language lang = corpTmp.get(j).getLanguage();
					Component sentSpPipelineComp = corpTmp.get(j).getComponent(CompNames.sentSp);
					File sentSplitterOutDir = null;
					File sentSplitterSourceDir = null;
					if (sentSpPipelineComp != null) {
						sentSplitterOutDir = sentSpPipelineComp
								.getOutDirectoryPath(sentenceSplitter.getSentenceSplitterVersion());
						sentSplitterSourceDir = sentSpPipelineComp
								.getSourceDirectoryPath(sentenceSplitter.getSentenceSplitterVersion());
					}
					if (sentSplitterOutDir != null && sentSplitterSourceDir != null) {
						sentenceSplitter.splitDirectory(sentSplitterSourceDir, sentSplitterOutDir, lang);
					} else {
						logger.error("Source direcotry or the output direcotry is missing: Source= "
								+ sentSplitterSourceDir + " Output= " + sentSplitterOutDir);
					}
				}
			}
		}
	}

	private void runLangId(Hashtable<Language, ArrayList<Corpus>> textCorpora) {
		for (Entry<Language, ArrayList<Corpus>> entry : textCorpora.entrySet()) {
			ArrayList<Corpus> corpTmp = entry.getValue();
			for (int j = 0; j < corpTmp.size(); j++) {
				if (corpTmp.get(j).getType() == Type.text) {
					Component langIdPipelineComp = corpTmp.get(j).getComponent(CompNames.langId);
					Hashtable<String, String> langIdVerToShortLocationMap = new Hashtable<String, String>();
					if (langIdPipelineComp != null) {
						langIdVerToShortLocationMap = langIdPipelineComp.getVerToShortLocationMap();
					}
					for (Entry<String, String> e : langIdVerToShortLocationMap.entrySet()) {
						File langIdOutDir = langIdPipelineComp.getOutDirectoryPath(e.getKey());
						File langIdSourceDir = langIdPipelineComp.getSourceDirectoryPath(e.getKey());
						if (langIdSourceDir != null && langIdOutDir != null) {
							LanguageIdentification languageIdentification = new LanguageIdentification(
									corpTmp.get(j).getLanguage());
							languageIdentification.identifyDir(langIdSourceDir, langIdOutDir);
						} else {
							logger.error("Source direcotry or the output direcotry is missing: Source= "
									+ langIdSourceDir + " Output= " + langIdOutDir);
						}
					}
				}
			}
		}
	}

}
