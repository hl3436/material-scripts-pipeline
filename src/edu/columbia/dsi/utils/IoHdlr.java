/**
 * 
 */
package edu.columbia.dsi.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

/**
 * @author badrashiny
 * Mar 3, 2018
 */
public class IoHdlr {

	/**
	 * 
	 */
	public IoHdlr() {
		
	}
	private static final class Holder {
        private static final IoHdlr instance = new IoHdlr();
    }
    
    public static IoHdlr getInstance() {
        return Holder.instance;
    }
    
    /**
     * Read a file in one String
     * @param file
     * @return
     */
    public String readFile(File file) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader (file.getCanonicalPath()));
			String line = null;
	        StringBuilder stringBuilder = new StringBuilder();
	        while((line = reader.readLine()) != null) {
	            stringBuilder.append(line+"\n");
	        }
	        reader.close();
	        return stringBuilder.toString().replaceAll("\n$", "");
		} catch (IOException e) {
			return null;
		}
    }
    
    /**
     * Read a file in a List of lines
     * @param file
     * @return
     */
    public ArrayList<String> readFileLines(File file) {
		return readFileLines(file, true);
    }

	public ArrayList<String> readFileLines(File file, boolean trim) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader (file.getCanonicalPath()));
			String line = null;
	        ArrayList<String> lines = new ArrayList<String>();
	        while((line = reader.readLine()) != null) {
				if (trim) {
					lines.add(line.trim());
				} else {
					lines.add(line);
				}
	        }
	        reader.close();
	        return lines;
		} catch (IOException e) {
			System.out.println(e);
			e.printStackTrace();
			return null;
		}
    }
    
    public List<String> readFileLinesUTF8(File file) {
    	 try {
        	Charset charset = StandardCharsets.UTF_8;
			return  Files.readAllLines(Paths.get(file.getAbsolutePath()), charset);
		} catch (IOException e) {
			return null;
		}
    }
    
    /**
     * Read a file in a List of lines skipping the first line
     * @param file
     * @return
     */
    public ArrayList<String> readFileLinesWithoutHeader(File file) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader (file.getCanonicalPath()));
			String line = null;
	        ArrayList<String> lines = new ArrayList<String>();
	        String header=reader.readLine();//skip the first line
	        while((line = reader.readLine()) != null) {
	        		lines.add(line.trim());
	        }
	        reader.close();
	        return lines;
		} catch (IOException e) {
			return null;
		}
    }
    
    public boolean exportFile(ArrayList<String>lines, File outFilePath) {
    	return exportFile(lines, outFilePath, true);
    }

	public boolean exportFile(ArrayList<String>lines, File outFilePath, boolean trim) {
    	try {
			PrintWriter writer = new PrintWriter(outFilePath.getCanonicalPath());
			for(int i=0;i<lines.size();i++) {
				if (trim) {
					writer.println(lines.get(i).trim());
				} else {
					writer.println(lines.get(i));
				}
			}				
			writer.close();
		} catch (IOException e) {
			return false;
		}
    	return true;		
    }
    
    public ArrayList<File> getListOfFiles(File directoryPath){
    	ArrayList<File> files = new ArrayList<File> ();   
	    // get all the files in the directory
	    File[] fList = directoryPath.listFiles();
	    for (File file : fList) {
	        if (file.isFile()) {
	        		files.add(file);	
	        }
	    }
		files.sort(null);
		return files;	    
	}
    
    /**
     * it returns a hashtable where the key is the file name without extension and the value is all the files that have the same name of the key with any extension
     * @param directoryPath
     * @return
     */
    public Hashtable<String, HashSet<File>> getListOfFilesGrouped(File directoryPath){    	
    	Hashtable<String, HashSet<File>> groupedFiles= new Hashtable<String, HashSet<File>>();
    	// get all the files in the directory
	    File[] fList = directoryPath.listFiles();
	    for (File file : fList) {
	        if (file.isFile()) {
	        	String fileNameWithOutExt = FilenameUtils.removeExtension(file.getName());
	        	HashSet<File> fGroup=null;
	        	if(groupedFiles.containsKey(fileNameWithOutExt)) {
        			fGroup=groupedFiles.get(fileNameWithOutExt);
        		}else {
        			fGroup=new HashSet<File>();
        		}
	        	fGroup.add(file);
	        	groupedFiles.put(fileNameWithOutExt, fGroup);
	        }
	    }
		return groupedFiles;	    
	}
    
    public ArrayList<String> getListOfFilesNames(File directoryPath){
    	ArrayList<String> files=new  ArrayList<String> ();   
	    // get all the files in the directory
	    File[] fList = directoryPath.listFiles();
	    for (File file : fList) {
	        if (file.isFile()) {
	        		files.add(file.getName());	
	        }
	    }
		return files;	    
	}

	public ArrayList<String> getListOfDirsNames(File directoryPath){
		ArrayList<String> dirs=new  ArrayList<String> ();   
		// get all the files in the directory
		File[] dList = directoryPath.listFiles();
		for (File dir : dList) {
			if (dir.isDirectory()) {
				dirs.add(dir.getName());	
			}
		}
		return dirs;
	}



    public ArrayList<File> getListOfFiles(File directoryPath, String extension){
    	if(extension==null) {
    		return getListOfFiles(directoryPath);
    	}else {
    		ArrayList<File> files=new  ArrayList<File> ();   
    	    // get all the files in the directory
    	    File[] fList = directoryPath.listFiles();
    	    for (File file : fList) {
    	        if (file.isFile() && file.getName().endsWith(extension)) {
    	        		files.add(file);	
    	        }
    	    }
    		return files;	
    	}
    	    
	}
    /**
     * Sometimes some files ends with the multiple extension suffix. Ex: .txt and .bst.txt
     * If you want to get only .txt for example, put the .bst.txt in the avoidExtension variable
     * @param directoryPath
     * @param extension
     * @param avoidExtension
     * @return
     */
    public ArrayList<File> getListOfFiles(File directoryPath, String extension, String avoidExtension){
    	if(extension==null) {
    		return getListOfFiles(directoryPath);
    	}else {
    		ArrayList<File> files=new  ArrayList<File> ();   
    	    // get all the files in the directory
    	    File[] fList = directoryPath.listFiles();
    	    for (File file : fList) {
    	        if (file.isFile() && file.getName().endsWith(extension) && !file.getName().endsWith(avoidExtension)) {
    	        		files.add(file);	
    	        }
    	    }
    		return files;	
    	}
    	    
	}
    
    public ArrayList<File> getListOfDirs(File directoryPath){
    	ArrayList<File> dirs=new  ArrayList<File> ();   
	    // get all the directories in the directory
	    File[] fList = directoryPath.listFiles();
	    for (File file : fList) {
	        if (file.isDirectory()) {
	        	dirs.add(file);	
	        }
	    }
		return dirs;
	}
    
    /**
	 * If the input is a directory, it keeps it and deletes all of its subdirectories 
	 */
	public void emptyDir(File directory) throws IOException {
		for (File childFile : directory.listFiles()) {
			if (childFile.isDirectory()) {
				deleteDir(childFile);
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
	public void deleteDir(File file) throws IOException {
		for (File childFile : file.listFiles()) {
            if (childFile.isDirectory()) {
            	deleteDir(childFile);
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
	 * Merge 2 files of same names with different extensions into a third extension with an empty line in between
	 * @param dirPath
	 * @param file1Ext
	 * @param file2Ext
	 * @param outExt
	 */
	public void mergeFilesWithEmptyLinesInBetween(File dirPath, String file1Ext, String file2Ext, String outExt) {
		try {
			ArrayList<File> firstFiles = getListOfFiles(dirPath, file1Ext, file2Ext);// get the files that have file1Ext and NOT file2Ext
			for(File f1: firstFiles) {
				String key=f1.getName().replace(file1Ext, "");
				File f2= new File (dirPath.getAbsolutePath()+File.separator+key+file2Ext);
				if(f2.exists()) {
					File outputFile = new File (dirPath.getAbsolutePath()+File.separator+key+outExt);
					

					// Read the file like string
					String f1Str = FileUtils.readFileToString(f1);
					f1Str+="\n";//add empty line
					String f2Str = FileUtils.readFileToString(f2);

					// Write the file
					FileUtils.write(outputFile, f1Str);					
					FileUtils.write(outputFile, f2Str, true); // true for append
				}
			}		
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Merge 2 files of same names with different extensions into a third extension
	 * @param dirPath
	 * @param file1Ext
	 * @param file2Ext
	 * @param outExt
	 */
	public void mergeFiles(File dirPath, String file1Ext, String file2Ext, String outExt) {
		try {
			
			ArrayList<File> firstFiles = getListOfFiles(dirPath, file1Ext, file2Ext);// get the files that have file1Ext and NOT file2Ext
			for(File f1: firstFiles) {
				String key=f1.getName().replace(file1Ext, "");
				File f2= new File (dirPath.getAbsolutePath()+File.separator+key+file2Ext);
				if(f2.exists()) {
					File outputFile = new File (dirPath.getAbsolutePath()+File.separator+key+outExt);
					// Read the file like string
					String f1Str = FileUtils.readFileToString(f1);
					String f2Str = FileUtils.readFileToString(f2);

					// Write the file
					FileUtils.write(outputFile, f1Str);					
					FileUtils.write(outputFile, f2Str, true); // true for append
				}
			}	
		} catch (IOException e) {
			e.printStackTrace();
		}
			
	}

}
