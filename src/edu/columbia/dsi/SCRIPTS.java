package edu.columbia.dsi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import edu.columbia.dsi.containers.Corpora;
import edu.columbia.dsi.containers.DataStoreMgr;
import edu.columbia.dsi.containers.DocInfo.Type;
import edu.columbia.dsi.utils.IoHdlr;
import edu.columbia.dsi.utils.MatcherTuner;
import edu.columbia.dsi.utils.ResourceFactory;
import edu.columbia.dsi.utils.Resources;

/**
 * @author badrashiny Feb 28, 2018
 */
public class SCRIPTS {
	private static Logger logger = Logger.getLogger(SCRIPTS.class.getSimpleName());

	public enum Language {
		tl, sw, en, so, sp, bg, lt, ps, fa, kk, ka;
	}// sp is also so but used with MarianNMT to translate ASR output of so

	DemoMode scriptsDemo = null; // For the demo using the internal CLIR system
	ElasticSearchDemo elasticsearchDemo = null; // For the demo using the elastic search CLIR system

	/**
	 * For the demo using the internal CLIR system
	 */
	public SCRIPTS(String resourceDir, String propFile, JSONObject expConfig, String outRootDir, String wrkdir,
			boolean stream) throws IOException {
		logger.debug("Initalizing SCRIPTS ...");

		Resources.getInstance().setWorkDir(new File(wrkdir));
		Resources.getInstance().setResourcekDir(new File(resourceDir));
		Resources.getInstance().setPropFile(new File(propFile));

		File workDir = new File(Resources.getInstance().getWorkDirCanonicalPath());
		ArrayList<File> oldFiles = IoHdlr.getInstance().getListOfFiles(workDir);
		ArrayList<File> oldDirs = IoHdlr.getInstance().getListOfDirs(workDir);
		for (File f : oldFiles) {
			if (f.getName().equals("workDirReadme.txt")) {
				continue;
			} else {
				f.delete();
			}
		}

		for (File d : oldDirs) {
			IoHdlr.getInstance().deleteDir(d);
		}
		ResourceFactory resourceFactory = new ResourceFactory();
		String nistDataDir = resourceFactory.getScriptsCorporaPath();
		scriptsDemo = new DemoMode(expConfig, new File(outRootDir), nistDataDir, stream);
	}

	public void shutdown() {
		if (scriptsDemo != null) {
			scriptsDemo.shutdown();
		}
	}

	public void search(Entry<String, HashSet<File>> processedQuery) {
		if (scriptsDemo != null) {
			scriptsDemo.run(processedQuery);
		}
	}

	public void search(String inputQueryString) {
		if (scriptsDemo != null) {
			scriptsDemo.run(inputQueryString);
		}
	}

	// ========================================================================================================================================================
	/**
	 * the elastic search CLIR system
	 */
	public SCRIPTS(String resourceDir, String propFile, JSONObject expConfig, String outRootDir, String wrkdir)
			throws IOException {
		logger.debug("Initalizing SCRIPTS ...");

		Resources.getInstance().setWorkDir(new File(wrkdir));
		Resources.getInstance().setResourcekDir(new File(resourceDir));
		Resources.getInstance().setPropFile(new File(propFile));

		File workDir = new File(Resources.getInstance().getWorkDirCanonicalPath());
		ArrayList<File> oldFiles = IoHdlr.getInstance().getListOfFiles(workDir);
		ArrayList<File> oldDirs = IoHdlr.getInstance().getListOfDirs(workDir);
		for (File f : oldFiles) {
			if (f.getName().equals("workDirReadme.txt")) {
				continue;
			} else {
				f.delete();
			}
		}

		for (File d : oldDirs) {
			IoHdlr.getInstance().deleteDir(d);
		}
		ResourceFactory resourceFactory = new ResourceFactory();
		String nistDataDir = resourceFactory.getScriptsCorporaPath();
		elasticsearchDemo = new ElasticSearchDemo(expConfig, new File(outRootDir), nistDataDir);
	}

	public void elasticSearch(String inputQueryString, String outputJsonFileName, Type type) {
		if (elasticsearchDemo != null) {
			elasticsearchDemo.run(inputQueryString, outputJsonFileName, type);
		}
	}

	public void shutdownElasticSearch() {
		if (elasticsearchDemo != null) {
			elasticsearchDemo.shutdown();
		}
	}
	// private boolean validityFlag=false;
	// private CLIR clir=null;
	// private Summarizer summarizer=null;
	//
	// public SCRIPTS(Language lang){
	// logger.debug("Initalizing SCRIPTS ...");
	// clir=new CLIR(lang);
	// summarizer=new Summarizer();
	// if(clir.isValid() && summarizer.isValid()) {
	// validityFlag=true;
	// }else {
	// shutdown();
	// validityFlag=false;
	// }
	// }
	// public boolean isValid() {
	// return validityFlag;
	// }
	// public void shutdown() {
	// if(clir!=null) {
	// clir.shutdown();
	// }
	// if(summarizer!=null) {
	// summarizer.shutdown();
	// }
	// }
	//
	// public QueryRes search(String query, SummarizerEngine summEngine, int
	// maxResSize) {
	// QueryRes results=clir.runQuery(query, maxResSize);
	// summarizer.summarize(results,summEngine);
	// return results;
	// }

	private static ConfigMode getMode(File configFile) {
		String jsonStr = IoHdlr.getInstance().readFile(configFile);
		JSONObject config = new JSONObject(jsonStr);

		if (config.has("mode")) {
			String value = config.getString("mode").trim().toUpperCase();
			String filename = "";
			for (ConfigMode mode : ConfigMode.values()) {
				if (mode.getName().equals(value)) {
					return mode;
				}
			}
			throw new IllegalArgumentException(String.format(
					"Unknown mode: \"%s\". Must be one of \"PREPROCESS\", \"INDEX\", \"QUERYSEARCH\", or \"SUMMARIZATION\"",
					value));
		} else {
			return null;
		}
	}

	private static ArrayList<File> sortConfFiles(ArrayList<File> configFiles) {
		if (configFiles.size() <= 1) {
			return configFiles;
		}

		HashMap<ConfigMode, ArrayList<File>> filemap = new HashMap<>();

		for (File configFile : configFiles) {
			ConfigMode mode = getMode(configFile);
			if (mode == null) {
				throw new IllegalArgumentException(
						String.format("Missing \"mode\" attribute in \"%s\"", configFile.getName()));
			}

			ArrayList<File> files = filemap.getOrDefault(mode, new ArrayList<>());
			files.add(configFile);
			filemap.put(mode, files);
		}

		configFiles.clear();
		for (ConfigMode mode : ConfigMode.values()) {
			configFiles.addAll(filemap.getOrDefault(mode, new ArrayList<>()));
		}
		return configFiles;
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		// String respurces="/storage/proj/badrashiny/Demo/demo2/resources";
		// String properties="/storage/proj/badrashiny/Demo/demo2/scripts.properties";
		// String jsonStrTmp=IoHdlr.getInstance().readFile(new
		// File("/storage/proj/badrashiny/Demo/demo2/PS_Q2_EVAL.json"));
		// String outRootDir="/storage/proj/badrashiny/Demo/demo2/outDir";
		// String wrkDir="/storage/proj/badrashiny/Demo/demo2/workDir";
		// JSONObject expConfig = new JSONObject(jsonStrTmp);
		// // SCRIPTS test =new SCRIPTS(respurces, properties, expConfig, outRootDir,
		// wrkDir);
		// SCRIPTS test= new SCRIPTS(respurces, properties, expConfig, outRootDir,
		// wrkDir);
		// long startTimeTmp = System.currentTimeMillis();
		// test.elasticSearch("drug addiction", "Q1.json", Type.text);
		// //// int relvFiles=IoHdlr.getInstance().getListOfFiles(new
		// File("/storage3/proj/badrashiny/SCRIPTS/sample_input_config/outDir/demo-out/CU-summaryDir/query5397"),
		// "json").size();
		// System.out.println("Search
		// time:"+String.valueOf((double)(System.currentTimeMillis()-startTimeTmp))+"msec");
		//
		// startTimeTmp = System.currentTimeMillis();
		// test.elasticSearch("super bus", "Q2.json", Type.text);
		// System.out.println("Search
		// time:"+String.valueOf((double)(System.currentTimeMillis()-startTimeTmp))+"msec");
		//
		// startTimeTmp = System.currentTimeMillis();
		// test.elasticSearch("\"reason for insomnia\",\"sleep improvement\"+",
		// "Q3.json", Type.text);
		// System.out.println("Search
		// time:"+String.valueOf((double)(System.currentTimeMillis()-startTimeTmp))+"msec");
		//
		// startTimeTmp = System.currentTimeMillis();
		// test.elasticSearch("orphanage,diplomacy+", "Q4.json", Type.text);
		// System.out.println("Search
		// time:"+String.valueOf((double)(System.currentTimeMillis()-startTimeTmp))+"msec");
		//
		// startTimeTmp = System.currentTimeMillis();
		// test.elasticSearch("equal <rights>", "Q5.json", Type.text);
		// System.out.println("Search
		// time:"+String.valueOf((double)(System.currentTimeMillis()-startTimeTmp))+"msec");
		//
		// startTimeTmp = System.currentTimeMillis();
		// test.elasticSearch("cauliflower,blood[syn:a fluid]", "Q6.json", Type.text);
		// System.out.println("Search
		// time:"+String.valueOf((double)(System.currentTimeMillis()-startTimeTmp))+"msec");
		//
		// startTimeTmp = System.currentTimeMillis();
		// test.elasticSearch("\"bad cholesterol\",EXAMPLE_OF(seasoning)[syn:a spice]",
		// "Q7.json", Type.text);
		// System.out.println("Search
		// time:"+String.valueOf((double)(System.currentTimeMillis()-startTimeTmp))+"msec");
		//
		// startTimeTmp = System.currentTimeMillis();
		// test.elasticSearch("\"equal <rights>\"", "Q8.json", Type.text);
		// System.out.println("Search
		// time:"+String.valueOf((double)(System.currentTimeMillis()-startTimeTmp))+"msec");
		//
		// startTimeTmp = System.currentTimeMillis();
		// test.elasticSearch("\"equal <rights>\"", "Q9.json", Type.audio);
		// System.out.println("Search
		// time:"+String.valueOf((double)(System.currentTimeMillis()-startTimeTmp))+"msec");
		//
		// ////
		// // startTimeTmp = System.currentTimeMillis();
		// // test.search("ladybug");
		// //// ArrayList<File>qTmp=IoHdlr.getInstance().getListOfDirs(new
		// File("/storage3/proj/badrashiny/SCRIPTS/sample_input_config/outDir/demo-out/CU-summaryDir"));
		// //// relvFiles=IoHdlr.getInstance().getListOfFiles(qTmp.get(0),
		// "json").size();
		// // System.out.println("Search
		// time:"+String.valueOf((double)(System.currentTimeMillis()-startTimeTmp)/(double)60000)+"min");
		// //
		// //
		// //
		// //
		// // startTime = System.currentTimeMillis();
		// // test.search(new
		// File("/storage3/data/NIST-data/1S/IARPA_MATERIAL_BASE-1S/query_store/query-analyzer-umd-v10.3/QUERY1/query5287"));
		// // relvFiles=IoHdlr.getInstance().getListOfFiles(new
		// File("/storage3/proj/badrashiny/SCRIPTS/sample_input_config/outDir/demo-out/CU-summaryDir/query5287"),
		// "json").size();
		// // System.out.println("Relv. #: "+relvFiles+"\tSearch
		// time:"+String.valueOf((double)(System.currentTimeMillis()-startTime)/(double)60000));
		// //
		// test.shutdownElasticSearch();
		// System.exit(0);

		long startTime = System.currentTimeMillis();
		logger.debug("Start time: " + String.valueOf(startTime));
		ResourceFactory resourceFactory = new ResourceFactory();


		final CommandLineParser cmdLineGnuParser = new GnuParser();
		CommandLine commandLine;
		final Options gnuOptions = CLI.constructGnuOptions();
		try {
			commandLine = cmdLineGnuParser.parse(gnuOptions, args);
			ArrayList<File> inputJsonFiles = new ArrayList<File>();

			if (commandLine.hasOption("i")) {
				File inputTmp = new File(commandLine.getOptionValue("i"));
				if (inputTmp.isFile()) {
					inputJsonFiles.add(new File(commandLine.getOptionValue("i")));
				} else {
					inputJsonFiles.addAll(IoHdlr.getInstance().getListOfFiles(inputTmp));
				}
			} else if (commandLine.hasOption("t")) {// testing mode
				inputJsonFiles.addAll(resourceFactory.getTestConfigFile());
			} else {
				System.out.println("Error: missing the input configuration file. System exiting...");
				logger.fatal("Error: missing the input configuration file. System exiting...");
				CLI.printUsage();
				System.exit(0);
			}

			sortConfFiles(inputJsonFiles);

			File workDir = null;
			if (!commandLine.hasOption("w")) {
				System.out.println("Error: missing the working directory. System exiting...");
				logger.fatal("Error: missing the working directory. System exiting...");
				CLI.printUsage();
				System.exit(0);
			}
			// else {
			// /**
			// * Cleanup the workDir
			// */
			// Resources.getInstance().setWorkDir(new
			// File(commandLine.getOptionValue("w")));
			// workDir=new File(Resources.getInstance().getWorkDirCanonicalPath());
			// ArrayList<File> oldFiles=IoHdlr.getInstance().getListOfFiles(workDir);
			// ArrayList<File> oldDirs=IoHdlr.getInstance().getListOfDirs(workDir);
			// for(File f:oldFiles) {
			// if(f.getName().equals("workDirReadme.txt")) {
			// continue;
			// }else {
			// f.delete();
			// }
			// }
			// for(File d:oldDirs) {
			// IoHdlr.getInstance().deleteDir(d);
			// }
			// }

			File outputFolder = null;
			if (commandLine.hasOption("o")) {
				outputFolder = new File(commandLine.getOptionValue("o"));
				if (!outputFolder.exists()) {
					outputFolder.mkdirs();

				}
			}

			File mathcerStoreFolder = null;
			if (commandLine.hasOption("c")) {
				mathcerStoreFolder = new File(commandLine.getOptionValue("c"));
				if (!mathcerStoreFolder.exists()) {
					mathcerStoreFolder.mkdirs();
				}
			}

			File mathcersList = null;
			if (commandLine.hasOption("m")) {
				mathcersList = new File(commandLine.getOptionValue("m"));
			}

			int maxMatchersLimit = Integer.MIN_VALUE;
			if (commandLine.hasOption("l")) {
				maxMatchersLimit = Integer.valueOf(commandLine.getOptionValue("l"));
			}

			String generatedIndexPath = null;

			for (File inputJsonFile : inputJsonFiles) {
				/**
				 * Cleanup the workDir
				 */
				ConfigMode mode = SCRIPTS.getMode(inputJsonFile);

				Resources.getInstance().setWorkDir(new File(commandLine.getOptionValue("w")));
				workDir = new File(Resources.getInstance().getWorkDirCanonicalPath());
				ArrayList<File> oldFiles = IoHdlr.getInstance().getListOfFiles(workDir);
				ArrayList<File> oldDirs = IoHdlr.getInstance().getListOfDirs(workDir);
				for (File f : oldFiles) {
					if (f.getName().equals("workDirReadme.txt")) {
						continue;
					} else {
						f.delete();
					}
				}
				for (File d : oldDirs) {
					IoHdlr.getInstance().deleteDir(d);
				}
				if (commandLine.hasOption("t")) {// testing mode
					System.out.print("Testing " + inputJsonFile.getName().replaceFirst("[.][^.]+$", "") + "...");
					logger.debug("Testing " + inputJsonFile.getName().replaceFirst("[.][^.]+$", "") + "...");
				} else {
					System.out.print("Processing " + inputJsonFile.getName().replaceFirst("[.][^.]+$", "") + " ...");
					logger.debug("Processing " + inputJsonFile.getName().replaceFirst("[.][^.]+$", "") + " ...");
				}

				// if (inputJsonFile.getName().contains("prep")) {
				if (ConfigMode.PREPROCESS.equals(mode) || ConfigMode.INDEX.equals(mode)
						|| (outputFolder == null && mathcersList == null && maxMatchersLimit == Integer.MIN_VALUE)) {// i.e. Preprocessing or Indexing pipeline
					String jsonStr = IoHdlr.getInstance().readFile(inputJsonFile);
					JSONObject jsonConfig = new JSONObject(jsonStr);
					boolean hasCorpora = jsonConfig.has("corpora");
					boolean hasIndexer = jsonConfig.has("indexer");
					Corpora corpora = null;
					if (hasCorpora) {
						String absPath = "";
						String relativePath = "";
						if (commandLine.hasOption("t")) {// testing mode
							absPath = resourceFactory.getTestDataRootAbsPath().trim();
							relativePath = resourceFactory.getTestDataRelativePath().trim();
						} else {// normal mode
							absPath = jsonConfig.getJSONObject("corpora").getString("root_absolute_path").trim();
							relativePath = jsonConfig.getJSONObject("corpora").getString("relative_path").trim();
						}

						corpora = new Corpora(absPath, relativePath);
						JSONArray collections = jsonConfig.getJSONObject("corpora").getJSONArray("collections");
						for (int i = 0; i < collections.length(); i++) {
							String corpusName = collections.getJSONObject(i).getString("corpus_name").trim();
							String sourceLocation = collections.getJSONObject(i).getString("source_location").trim();
							String manualTranslationLocation = null;
							if (collections.getJSONObject(i).has("manual_translation")) {
								manualTranslationLocation = collections.getJSONObject(i).getString("manual_translation")
										.trim();
							}
							String manualTranscriptionLocation = null;
							if (collections.getJSONObject(i).has("manual_transcription")) {
								manualTranscriptionLocation = collections.getJSONObject(i)
										.getString("manual_transcription").trim();
							}
							String metaDataLocation = absPath + File.separator + relativePath + File.separator
									+ corpusName + File.separator
									+ collections.getJSONObject(i).getString("meta_data_location").trim();
							Type type = Type.valueOf(collections.getJSONObject(i).getString("type").trim());
							Language lang = Language.valueOf(collections.getJSONObject(i).getString("language").trim());
							corpora.addCorpus(corpusName, sourceLocation, type, lang, metaDataLocation,
									manualTranslationLocation, manualTranscriptionLocation);
						}

						if (jsonConfig.getJSONObject("corpora").has("queries")) {
							JSONArray queries = jsonConfig.getJSONObject("corpora").getJSONArray("queries");
							for (int i = 0; i < queries.length(); i++) {
								String queryName = queries.getJSONObject(i).getString("query_name").trim();
								Language lang = Language.valueOf(queries.getJSONObject(i).getString("language").trim());
								corpora.addQuery(queryName, lang);
							}
						}
					}
					if (corpora == null || corpora.getAllCorpora().isEmpty()) {
						System.out.println("Error: The corpora section in the input configuration file is missing or empty. System exiting...");
						System.exit(0);
					}

					if (hasIndexer) {// i.e. Indexing pipeline
						System.out.println("The input file is an indexing configuration file. SCRIPTS is going to index the input corpora. Check the log file for the system progress");
						JSONArray indriParam = jsonConfig.getJSONObject("indexer").getJSONArray("indri_parameters");
						ArrayList<File> indriParamFiles = new ArrayList<File>();
						
						String indexerResourceDir = resourceFactory.getIndexerResourceDir();
						String umdIndexerParameters = resourceFactory.getUmdIndexerParameters();
						for (int i = 0; i < indriParam.length(); i++) {
							String indriFilename = indriParam.getString(i).trim();
							File paramSourceFile = Paths.get(indexerResourceDir, umdIndexerParameters, indriFilename).toFile();
							File paramTargetFile = Paths.get(Resources.getInstance().getWorkDirCanonicalPath(), umdIndexerParameters, indriFilename).toFile();
							FileUtils.copyFile(paramSourceFile, paramTargetFile);
							indriParamFiles.add(paramTargetFile);
						}
						if (indriParamFiles.size() == 0) {
							System.out.println("Error: missing the indri parameters files. Please add at least one indri prameter file to the input configuration file");
							System.exit(0);
						} else {
							Indexer indexer = new Indexer(indriParamFiles);
							generatedIndexPath = indexer.index(corpora);
						}
					} else {// i.e. Preprocessing pipeline
						if (!commandLine.hasOption("t")) {// not testing mode
							System.out.println("The input file is a preprocessing configuration file. SCRIPTS is going to preprocess the input corpora. Check the log file for the system progress");
						}
						DataStoreMgr dataStoreMgr = new DataStoreMgr(corpora);
						Preprocessor preprocessor = new Preprocessor(corpora.getAllCorpora(), corpora.getAllQueries());
						if (preprocessor.isValid()) {
							preprocessor.process();
						}
						dataStoreMgr = new DataStoreMgr(corpora);// update the data store file after the actual directories from the pipeline
					}
				} else if (ConfigMode.SUMMARIZATION.equals(mode)) {
					System.out.println("SCRIPTS is running in the summarization mode. Please check the log file for the system progress");
					StandaloneSummarizer summarizer = new StandaloneSummarizer();
					// workDir=new File(Resources.getInstance().getWorkDirCanonicalPath());
					// File clirFolder = Paths.get(commandLine.getOptionValue("o"),
					// "clir").toFile();
					// File summarizerFolder = Paths.get(commandLine.getOptionValue("o"),
					// "summarizer").toFile();
					// if(!summarizerFolder.exists()) {
					// summarizerFolder.mkdirs();
					// }
					summarizer.runSummarizer(outputFolder, inputJsonFile);

				} else if (ConfigMode.QUERYSEARCH.equals(mode) // i.e. Query search pipeline;
						|| (outputFolder != null && mathcersList == null && maxMatchersLimit == Integer.MIN_VALUE)) {
						System.out.println(
							"SCRIPTS is running in the query search mode. Please check the log file for the system progress");
					CLIRExperimenter experimenter = new CLIRExperimenter();
					if (mathcerStoreFolder != null) {
						boolean override = false;
						if (commandLine.hasOption("r")) {
							override = true;
						}
						experimenter.setCachingPath(mathcerStoreFolder, override);
					}

					String subdir = FilenameUtils.removeExtension(inputJsonFile.getName());

					File clirFolder = Paths.get(commandLine.getOptionValue("o"), subdir).toFile();
					if (!clirFolder.exists()) {
						clirFolder.mkdirs();
					}

					experimenter.runExperiment(inputJsonFile, clirFolder, generatedIndexPath);
					experimenter.shutdown();

				} else if (outputFolder != null && mathcersList != null && maxMatchersLimit != Integer.MIN_VALUE) {// i.e. Tuning pipeline
					System.out.println("SCRIPTS is running in the tuning mode. Please check the log file for the system progress");
					MatcherTuner mTuner = new MatcherTuner(inputJsonFile, mathcersList);
					mTuner.tune(outputFolder, maxMatchersLimit);
				} else {
					System.out.println("Wrong input options. Please check the user manual...");
					CLI.printUsage();
				}

				if (commandLine.hasOption("t")) {// testing mode
					System.out.println("[finished]");
				}
			}
			if (commandLine.hasOption("t")) {// testing mode
				System.out.println("Please check the output log4j.log file. Also please check the output stores under resources/TestData: EX: resources/NIST-data/1A/IARPA_MATERIAL_BASE-1A/DEV/{text, audio}/X_store; where X is your component {asr, mt, morphology, kws} ");
			}
		} catch (ParseException parseException) { // checked exception
			String str;
			str = "Encountered a problem while parsing arguments:\n" + parseException.getMessage();
			System.out.println(str);
			logger.error(str);
			CLI.printUsage();
		}

		// /**
		// * Cleanup the workDir
		// */
		// String wrkdir=null;
		// if(args.length==2) {
		// wrkdir=args[1];
		// }else if(args.length==3) {
		// wrkdir=args[2];
		// }else if(args.length==5) {
		// wrkdir=args[4];
		// }else {
		// System.out.println("Error: Incorrect number of input parameters...");
		// System.exit(0);
		// }
		// Resources.getInstance().setWorkDir(new File(wrkdir));
		//
		// File workDir=new File(Resources.getInstance().getWorkDirCanonicalPath());
		// ArrayList<File> oldFiles=IoHdlr.getInstance().getListOfFiles(workDir);
		// ArrayList<File> oldDirs=IoHdlr.getInstance().getListOfDirs(workDir);
		// for(File f:oldFiles) {
		// if(f.getName().equals("workDirReadme.txt")) {
		// continue;
		// }else {
		// f.delete();
		// }
		// }
		//
		// for(File d:oldDirs) {
		// IoHdlr.getInstance().deleteDir(d);
		// }
		//
		// if(args.length==2) {
		// File inputJsonFile=new File(args[0]);
		// String jsonStr=IoHdlr.getInstance().readFile(inputJsonFile);
		// JSONObject jsonConfig = new JSONObject(jsonStr);
		// boolean hasCorpora=jsonConfig.has("corpora");
		// boolean hasIndexer=jsonConfig.has("indexer");
		// Corpora corpora=null;
		//
		// if(hasCorpora) {
		// String
		// absPath=jsonConfig.getJSONObject("corpora").getString("root_absolute_path").trim();
		// String
		// relativePath=jsonConfig.getJSONObject("corpora").getString("relative_path").trim();
		// corpora=new Corpora(absPath, relativePath);
		// JSONArray collections =
		// jsonConfig.getJSONObject("corpora").getJSONArray("collections");
		// for (int i = 0; i < collections.length(); i++){
		// String
		// corpusName=collections.getJSONObject(i).getString("corpus_name").trim();
		// String
		// sourceLocation=collections.getJSONObject(i).getString("source_location").trim();
		// String
		// metaDataLocation=absPath+File.separator+relativePath+File.separator+corpusName+File.separator+collections.getJSONObject(i).getString("meta_data_location").trim();
		// Type
		// type=Type.valueOf(collections.getJSONObject(i).getString("type").trim());
		// Language
		// lang=Language.valueOf(collections.getJSONObject(i).getString("language").trim());
		// corpora.addCorpus(corpusName, sourceLocation, type, lang, metaDataLocation);
		// }
		//
		// if(jsonConfig.getJSONObject("corpora").has("queries")) {
		// JSONArray queries =
		// jsonConfig.getJSONObject("corpora").getJSONArray("queries");
		// for (int i = 0; i < queries.length(); i++){
		// String queryName=queries.getJSONObject(i).getString("query_name").trim();
		// Language
		// lang=Language.valueOf(queries.getJSONObject(i).getString("language").trim());
		// corpora.addQuery(queryName, lang);
		// }
		// }
		// }
		//
		// if(corpora!=null && !hasIndexer) {//i.e. preprocessing pipeline
		// System.out.println("The input file is a preprocessing configuration file.
		// SCRIPTS is going to preprocess the input corpora. Check the log file for the
		// system progress");
		// DataStoreMgr dataStoreMgr=new DataStoreMgr(corpora);
		// Preprocessor preprocessor = new Preprocessor(corpora.getAllCorpora(),
		// corpora.getAllQueries());
		// if(preprocessor.isValid()) {
		// preprocessor.process();
		// }
		// }else if(corpora!=null && hasIndexer) {//i.e. indexing pipeline
		// System.out.println("The input file is an indexing configuration file. SCRIPTS
		// is going to index the input corpora. Check the log file for the system
		// progress");
		//
		// JSONArray indriParam =
		// jsonConfig.getJSONObject("indexer").getJSONArray("indri_parameters");
		// ArrayList<File>indriParamFiles=new ArrayList<File>();
		// for (int i = 0; i < indriParam.length(); i++){
		// indriParamFiles.add(new File(indriParam.getString(i).trim()));
		// }
		// if(indriParamFiles.size()==0) {
		// System.out.println("Error: missing the indri parameters files. Please add at
		// least one indri prameter file to the input configuration file");
		// System.exit(0);
		// }else {
		// Indexer indexer=new Indexer(indriParamFiles);
		// indexer.index(corpora);
		// }
		// }else {
		// System.out.println("Error: can not detect the type of the input configuration
		// file...");
		// System.exit(0);
		// }
		// }else if(args.length==3){ //i.e. query search pipeline
		// File inputJsonFile=new File(args[0]);
		// File outputFolder=new File(args[1]);
		// if(!outputFolder.exists()) {
		// outputFolder.mkdirs();
		// }
		// CLIRExperimenter experimenter= new CLIRExperimenter();
		// experimenter.runExperiment(inputJsonFile, outputFolder);
		// experimenter.shutdown();
		// }else if(args.length==5){ //i.e. Tuning pipeline
		// File configTemplate=new File(args[0]);
		// File mathcersList=new File(args[1]);
		// int maxMatchersLimit=Integer.valueOf(args[2]);
		// File outputFolder=new File(args[3]);
		// if(!outputFolder.exists()) {
		// outputFolder.mkdirs();
		// }
		//
		// MatcherTuner mTuner=new MatcherTuner(configTemplate, mathcersList);
		// mTuner.tune(outputFolder, maxMatchersLimit);
		//
		// }else {
		// System.out.println("Error: Incorrect number of input parameters...");
		// System.exit(0);
		// }

		logger.debug("Processing time:" + String.valueOf(System.currentTimeMillis() - startTime));
		System.exit(0);
	}

	public static enum ConfigMode {
		/* ORDER MATTERS!! */
		PREPROCESS("PREPROCESS"), INDEX("INDEX"), QUERYSEARCH("QUERYSEARCH"), SUMMARIZATION("SUMMARIZATION");

		private final String name;

		/**
		 * @param name
		 */
		private ConfigMode(final String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}
}
