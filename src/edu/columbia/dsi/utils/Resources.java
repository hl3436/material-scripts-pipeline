package edu.columbia.dsi.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import org.apache.log4j.Logger;

/**
 * @author badrashiny
 * Mar 2, 2018
 */
public class Resources {
    private static Logger logger = Logger.getLogger( Resources.class.getSimpleName() );
    private File workDir;
    private File resourceDir=null;
    private File propFile=null;
    private Resources() {
    }

    private static final class Holder {
        private static final Resources instance = new Resources();
    }

    
    public static Resources getInstance() {
        return Holder.instance;
    }
    
    public void setWorkDir(File workDir) {
    	if(!workDir.exists()) {
    		workDir.mkdirs();
    	}
    	this.workDir=workDir;
    }
    
    public void setResourcekDir(File resDir) {
    	this.resourceDir=resDir;
    }
    
    public void setPropFile(File propFile) {
    	this.propFile=propFile;
    }

    /** Gets an Inputstream to a resource with the specified name. This resource must be in the classpath
     *
     * @param name file name.
     * @return An input stream to the resource, or null if resource is not found.
     */
    public InputStream getResourceAsStream(String name) {

        InputStream inputStream = null;

        ClassLoader loader = ClassLoader.getSystemClassLoader();
        if( (inputStream = loader.getResourceAsStream(name)) != null) {
            return inputStream;
        } else {
            logger.error("Unable to load resource from file "+name);
        }


        return inputStream;
    }

    /** Gets a BufferedReader to a resource with the specified name. This resource must be in the classpath
     *
     * @param name file name.
     * @return An input stream to the resource, or null if resource is not found.
     */
    public BufferedReader getResourceAsBufferedReader(String name) {

        InputStream is = getResourceAsStream(name);

        if(is != null ) {
            Reader reader = new InputStreamReader(is);
            return new BufferedReader(reader);
        } else {
            return null;
        }
    }
    
    /** 
     * Gets a canonical path of a resources directory
     */
    public String getResourcesDirCanonicalPath() {
    		try {
    			if(resourceDir==null) {
    				ClassLoader loader = ClassLoader.getSystemClassLoader();
        			URL resourceLink = loader.getResource("resourcesDirReadme.txt");
        			return new File(resourceLink.getPath()).getCanonicalPath().replace("/resourcesDirReadme.txt", "");     				
    			}else {
    				return resourceDir.getCanonicalPath();
    			}
    			 
    		}catch (Exception e) {
    			logger.error("Unable to find resources directory");
    			return null;
    		}
    }
    
    /** 
     * Gets a canonical path of a resources directory
     */
    public File getPropFilePath() {
    		try {
    			if(propFile==null) {
        			return null;     				
    			}else {
    				return propFile;
    			}
    			 
    		}catch (Exception e) {
    			logger.error("Unable to find resources directory");
    			return null;
    		}
    }
    
    /** 
     * Gets a canonical path of the work directory
     */
    public String getWorkDirCanonicalPath() {
//    		try {
//    			ClassLoader loader = ClassLoader.getSystemClassLoader();
//    			URL resourceLink = loader.getResource("workDirReadme.txt");
//    			return new File(resourceLink.getPath()).getCanonicalPath().replace("/workDirReadme.txt", "");  
//    		}catch (Exception e) {
//    			logger.error("Unable to find resources directory");
//    			return null;
//    		}
    		try {
    			return workDir.getCanonicalPath();  
    		}catch (Exception e) {
    			logger.error("Unable to find the work directory");
    			return null;
    		}    		
    }   
  
}
