/**
 * 
 */
package edu.columbia.dsi.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

import org.apache.log4j.Logger;

/**
 * @author badrashiny Mar 2, 2018
 */
public class WorkDirHdlr {
	private static Logger logger = Logger.getLogger(WorkDirHdlr.class.getSimpleName());
	private ArrayList<String> folders = new ArrayList<String>(); // subfolders inside the workDir
	private File workDir;
	private Hashtable<String, File> subDirPathMapper = new Hashtable<String, File>();

	/**
	 * This class handles any work under the workDir. It creates, deletes and empty
	 * folders or files under the workDir
	 */
	public WorkDirHdlr(ArrayList<String> folders) {
		this.workDir = new File(Resources.getInstance().getWorkDirCanonicalPath());
		this.folders.addAll(folders);
		try {
			for (int i = 0; i < folders.size(); i++) {
				File subFolder = new File(workDir.getCanonicalPath() + File.separator + folders.get(i));
				subDirPathMapper.put(folders.get(i), subFolder);
			}
		} catch (IOException e) {
			logger.error("Couldn't prepare the workDir");
		}
		prepareWorkDir();
	}

	public File getSubFolderHdlr(String subFolder) {
		return subDirPathMapper.get(subFolder);
	}

	public void prepareWorkDir() {
		try {
			for (int i = 0; i < folders.size(); i++) {
				File subFolder = getSubFolderHdlr(folders.get(i));
				if (!subFolder.exists()) {
					subFolder.mkdir();
				} else {
					empty(subFolder);
				}
			}
		} catch (IOException e) {
			logger.error("Couldn't prepare the workDir");
		}
	}

	/**
	 * If the input is a directory, it keeps it and deletes all of its subdirectories 
	 */
	public void empty(File directory) throws IOException {
		for (File childFile : directory.listFiles()) {
			if (childFile.isDirectory()) {
				delete(childFile);
			} else {
				if (!childFile.delete()) {
					throw new IOException();
				}
			}
		}
	}

	/**
	 * Delete the input file. If the input is a directory, it deletes it and all of its subdirectories 
	 */
	private void delete(File file) throws IOException {
		for (File childFile : file.listFiles()) {
			if (childFile.isDirectory()) {
				delete(childFile);
			} else {
				if (!childFile.delete()) {
					throw new IOException();
				}
			}
		}
		if (!file.delete()) {
			throw new IOException();
		}
	}

	/**
	 * delete all of the subdirectories created by this class 
	 */
	public void cleanup() {
		try {
			for (int i = 0; i < folders.size(); i++) {
				File subFolder = getSubFolderHdlr(folders.get(i));
				delete(subFolder);
			}
		} catch (IOException e) {
			logger.error("Couldn't cleanup the workDir");
		}
	}
}
