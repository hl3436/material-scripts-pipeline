package edu.columbia.dsi;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
// import org.json.JSONArray;
import org.json.JSONObject;

import edu.columbia.dsi.SCRIPTS.Language;
import edu.columbia.dsi.containers.UmdCLIRConfig;
import edu.columbia.dsi.containers.UmdMatcherConfig;
import edu.columbia.dsi.ir.UmdCLIR.UmdCLIRCombFormat;
import edu.columbia.dsi.utils.IoHdlr;
import edu.columbia.dsi.utils.ResourceFactory;

public class StandaloneSummarizer {
	private static Logger logger = Logger.getLogger(CLIRExperimenter.class.getSimpleName());
	private ResourceFactory resourceFactory = new ResourceFactory();
	private File startServerScriptPath;

	public StandaloneSummarizer() {
		logger.debug("Initializing StandaloneSummarizer...");
		this.startServerScriptPath = resourceFactory.getCUSummarizerStartServerScript();
	}

	public boolean runSummarizer(File outputDir, File configFile) {
		logger.debug("Starting Summarizer...");

		boolean ok = true;
		try {

			SummarizerConfig config = parseConfig(configFile);

			File summarizerFolder = Paths.get(outputDir.getAbsolutePath(), "summarizer", config.querySearchDir).toFile();
			if (!summarizerFolder.exists()) {
				summarizerFolder.mkdirs();
			}

			// UmdCLIRConfig expConfig=parseconfig(expConfigFilePath);

			/*
			 * PIPELINE_DIR=/storage/proj/dw2735/experiments/docker_test/ps/text
			 * OUTPUT_DIR=/storage/proj/dw2735/summarizer_output/docker_test/ps/text
			 * 
			 * CLIR=$PIPELINE_DIR/UMD-CLIR-workECDir
			 * NIST_VOL="-v /storage/data/NIST-data:/NIST-data"
			 * EXPERIMENT_VOL="-v $PIPELINE_DIR:/experiment" CLIR_VOL="-v $CLIR:/clir"
			 * OUTPUT_VOL="-v $OUTPUT_DIR:/outputs"
			 * 
			 * VERBOSE="-e VERBOSE=true" run_name="CUSUM" work_dir=$OUTPUT_DIR num_procs=12
			 * gpu_id=0
			 * 
			 * /local/nlp/data/NIST-data $(pwd)/summ2
			 * $(pwd)/run14_multipleok/FA_Q2_EVAL_text_clir/query-analyzer-umd-v16.
			 * 0_matching-umd-v15.4_evidence-combination-v13.4/
			 * FA_QUERY2_EVAL_hmm_PSQ_neulex_words_indriinter_words_indri_p_D-
			 * tEDINMTbTN_indri_PSQ_indrijp_PSQ_Cutoff46 3
			 * 
			 * docker run -it -v /var/run/docker.sock:/var/run/docker.sock \ $NIST_VOL
			 * $OUTPUT_VOL $CLIR_VOL $EXPERIMENT_VOL -it --user "$(id -u):$(id -g)"
			 * --group-add $(stat -c '%g' /var/run/docker.sock) --rm $VERBOSE \ --name
			 * sumtest --ipc=host summarizer:v3.0 $run_name $work_dir $num_procs $gpu_id
			 */
			String nistDir = resourceFactory.getScriptsCorporaPath();
			String summarizerVersion = resourceFactory.getCUSummarizerVersion();
			File clirDir = Paths.get(outputDir.getAbsolutePath(), config.querySearchDir).toFile();
			File clirSubDir = getClirSubdirectory(clirDir, config.querySearch, 3);

			if (clirSubDir == null) {
				throw new IOException("CLIR directory not found in " + outputDir.getName());
			}
			logger.debug("starting " + this.startServerScriptPath);
			String cmd = String.format("sh %s %s %s %s %s %d %s %s", this.startServerScriptPath, nistDir,
					summarizerFolder.getAbsolutePath(), clirSubDir.getAbsolutePath(), config.runName, config.numProcs, // num processes
					config.gpuIds, // gpu index
					summarizerVersion
			);
			logger.debug("cmd: " + cmd);
			Process process = Runtime.getRuntime().exec(cmd);
			exportLog(process);
			process.waitFor();
			logger.debug("Summarizer process completed...");

		} catch (IOException e) {
			logger.fatal(e.getMessage());
			System.out.println(e.getMessage());
			ok = false;
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.fatal(e.getMessage());
			System.out.println(e.getMessage());
			ok = false;
		}

		return ok;
	}

	private void exportLog(Process process) {
		ExecutorService executorService = Executors.newFixedThreadPool(2);
		ArrayList<Future<?>> openThreads = new ArrayList<Future<?>>();
		Future<?> stdoutFuture = executorService.submit(new Runnable() {
			public void run() {
				BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
				String str = "";
				try {
					while ((str = stdout.readLine()) != null) {
						logger.debug(str);
					}
				} catch (IOException e) {
					logger.fatal(e.getMessage());
				}
			}
		});
		openThreads.add(stdoutFuture);

		Future<?> stderrFuture = executorService.submit(new Runnable() {
			public void run() {
				BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
				String strErr = "";
				try {
					while ((strErr = stderr.readLine()) != null) {
						logger.error(strErr);
					}
				} catch (IOException e) {
					logger.fatal(e.getMessage());
				}
			}
		});
		openThreads.add(stderrFuture);

		for (int k = 0; k < openThreads.size(); k++) {
			try {
				openThreads.get(k).get();
			} catch (InterruptedException | ExecutionException e) {
				logger.fatal(e.getMessage());
			}
		}
		executorService.shutdown();
	}

	// Recursively search for the CLIR output directory.
	private static File getClirSubdirectory(File root, String querysearchName, int maxDepth) {
		if (maxDepth <= 0)
			return null;

		boolean found = false;
		for (File f : IoHdlr.getInstance().getListOfFiles(root, ".json")) {
			if (querysearchName.equals(f.getName())) {
				found = true;
				break;
			}
		}

		ArrayList<File> subdirs = IoHdlr.getInstance().getListOfDirs(root);

		if (found) {
			for (File sub1 : subdirs) {
				if (sub1.getPath().endsWith("/UMD-CLIR-workECDir")) {
					return root;
				}
			}
		} else {
			// Search subdir if not found
			for (File sub1 : subdirs) {
				File r = getClirSubdirectory(sub1, querysearchName, maxDepth - 1);
				if (r != null) {
					return r;
				}
			}
		}
		return null;
	}

	private SummarizerConfig parseConfig(File configFile) {
		String jsonStr = IoHdlr.getInstance().readFile(configFile);
		JSONObject configObj = new JSONObject(jsonStr);

		SummarizerConfig config = new SummarizerConfig();

		String rawQuerySearch = configObj.getString("querysearch");
		if (rawQuerySearch.endsWith(".json")) {
			config.querySearch = rawQuerySearch;
			config.querySearchDir = rawQuerySearch.replace(".json", "");
		} else {
			config.querySearch = rawQuerySearch + ".json";
			config.querySearchDir = rawQuerySearch;
		}

		if (configObj.has("run_name")) {
			config.runName = configObj.getString("run_name");
		}

		if (configObj.has("num_procs")) {
			config.numProcs = configObj.getInt("num_procs");
		}

		if (configObj.has("gpu_id")) {
			config.gpuIds = configObj.getString("gpu_id");
		}

		return config;
	}

	private static class SummarizerConfig {
		String querySearch;
		String querySearchDir;
		String runName = "CUSUM";
		int numProcs = 12;
		String gpuIds = "0";

		SummarizerConfig() {
		}
	}
}
