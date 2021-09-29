/**
 * 
 */
package edu.columbia.dsi;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import edu.columbia.dsi.containers.DocInfo.Type;
import edu.columbia.dsi.utils.IoHdlr;
import edu.columbia.dsi.utils.ResourceFactory;
import edu.columbia.dsi.utils.WorkDirHdlr;

/**
 * @author badrashiny Jun 10, 2020
 */
public class ElasticSearchDemo {
	private static Logger logger = Logger.getLogger(ElasticSearchDemo.class.getSimpleName());
	private File startServerScriptPath = null;
	private File shutdownServerScriptPath = null;
	private File searchQueriesScriptPath = null;
	private File deployFilePath = null;
	private String summarizerVersionNumber = null;
	private String maxNumDocs;
	private File clirOutDir = null;
	private File summarizerOutDir = null;
	private int maxthread;
	private String instanceName = UUID.randomUUID().toString(); // Generate a random docker instance name
	private ResourceFactory resourceFactory = new ResourceFactory();
	private WorkDirHdlr workDirHdlr;
	private String workDir = "Demo-WorkDir" + UUID.randomUUID().toString()
			+ (new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()));

	/**
	 * 
	 */
	public ElasticSearchDemo(JSONObject expConfigJson, File outRootDir, String nistDataDir) {
		try {
			this.startServerScriptPath = resourceFactory.getDemoStartServerScript();
			this.shutdownServerScriptPath = resourceFactory.getDemoShutdownServerScript();
			this.searchQueriesScriptPath = resourceFactory.getDemoSearchQueryScript();
			this.deployFilePath = resourceFactory.getDemoDeployFilePath();
			this.maxthread = resourceFactory.getCUSummarizerMaxConcurrentTasks();
			this.summarizerVersionNumber = resourceFactory.getCUSummarizerVersion();
			this.maxNumDocs = resourceFactory.getDemoMaxNumOfDocumnets();
			this.summarizerOutDir = new File(outRootDir.getCanonicalPath() + File.separator + "CU-summaryDir");

			ArrayList<String> folders = new ArrayList<String>();
			folders.add(workDir);
			workDirHdlr = new WorkDirHdlr(folders);

			String expPathTmp = workDirHdlr.getSubFolderHdlr(this.workDir).getCanonicalPath() + File.separator
					+ "exp-config.json";
			BufferedWriter writer = new BufferedWriter(new FileWriter(expPathTmp));
			writer.write(expConfigJson.toString(4));
			writer.close();

			if (!this.summarizerOutDir.exists()) {
				this.summarizerOutDir.mkdir();
			} else {
				IoHdlr.getInstance().emptyDir(this.summarizerOutDir);
			}

			this.clirOutDir = new File(summarizerOutDir.getCanonicalPath() + File.separator + "clir_output");
			if (!this.clirOutDir.exists()) {
				this.clirOutDir.mkdir();
			} else {
				IoHdlr.getInstance().emptyDir(this.clirOutDir);
			}

			startServer(nistDataDir);

		} catch (IOException e) {
			logger.fatal(e.getMessage());
		}
	}

	private void startServer(String nistDataDir) {
		try {
			String cmd = "sh" + " " + this.startServerScriptPath.getCanonicalPath() + " "
					+ deployFilePath.getCanonicalPath() + " " + nistDataDir;
			cmd += " " + workDirHdlr.getSubFolderHdlr(this.workDir).getCanonicalPath() + " "
					+ this.summarizerOutDir.getCanonicalPath();
			cmd += " " + this.instanceName + " " + this.summarizerVersionNumber;
			logger.debug(cmd);
			Process process = Runtime.getRuntime().exec(cmd);
			BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = "";
			while ((line = input.readLine()) != null) {
				if (line.contains("Summarization server is ready")) {
					logger.debug(line);
					TimeUnit.SECONDS.sleep(30);
					break;
				} else {
					logger.debug(line);
				}
			}
		} catch (IOException | InterruptedException e) {
			logger.fatal(e);
		}

	}

	public void run(String query, String outJsonName, Type type) {
		// String baseUrl = "localhost:5000/search?q=";
		// String size ="\\&size=10";
		// String encodedQuery = null;
		try {
			if (!this.summarizerOutDir.exists()) {
				this.summarizerOutDir.mkdir();
			}
			if (!this.clirOutDir.exists()) {
				this.clirOutDir.mkdir();
			}

			// encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
			// String URLedQuery = baseUrl + encodedQuery+size;
			// String cmd="sh"+" "+this.searchQueriesScriptPath.getCanonicalPath()+"
			// "+URLedQuery+" "+outJsonName+" "+this.clirOutDir+ " "+this.instanceName+"
			// "+String.valueOf(this.maxthread);
			// # $1: query string
			// # $2: source index to use {audio, text}
			// # $3: maximum number of retrieved documents
			// # $4: json file name
			// # $5: absolute path to put $4 in
			// # $6: summarization instance name
			// # $7: number of jobs

			File tmpQueryFile = new File(workDirHdlr.getSubFolderHdlr(workDir).getCanonicalPath() + File.separator
					+ outJsonName.replace(".json", ".qry"));
			PrintWriter writer = new PrintWriter(tmpQueryFile.getCanonicalPath());
			writer.println(query);
			writer.close();

			String cmd = "sh" + " " + this.searchQueriesScriptPath.getCanonicalPath() + " "
					+ tmpQueryFile.getCanonicalPath() + " " + type.toString() + " " + this.maxNumDocs + " "
					+ outJsonName + " " + this.clirOutDir;
			cmd += " " + this.instanceName + " " + String.valueOf(this.maxthread);

			logger.debug(cmd);

			Process process = Runtime.getRuntime().exec(cmd);
			exportLog(process);
			process.waitFor();
		} catch (IOException | InterruptedException e) {
			logger.fatal(e);
		}

	}

	public void shutdown() {
		try {
			String cmd = "sh" + " " + this.shutdownServerScriptPath.getCanonicalPath() + " "
					+ deployFilePath.getCanonicalPath() + " " + this.instanceName;
			logger.debug(cmd);

			Process process = Runtime.getRuntime().exec(cmd);
			exportLog(process);
			process.waitFor();
		} catch (IOException | InterruptedException e) {
			logger.fatal(e);
		}

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

}
