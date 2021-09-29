package edu.columbia.dsi.utils;

import org.apache.log4j.Logger;

import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;


public class Properties {

    private Map<String, java.util.Properties> props;
    private static Logger logger = Logger.getLogger( Properties.class.getSimpleName() );

    private Properties() {
        props = new HashMap<String, java.util.Properties>();
    }

    private static final class Holder {
        private static final Properties instance = new Properties();
    }

    /** Get an instance of this class.
     *
     * @return returns an instance of this singleton class
     */
    public static Properties getInstance() {
        return Holder.instance;
    }

    /**  Gets a set of properties
     *
     * @param propertiesName name of properties file
     * @return a properties object containing properties from the file with the specified file name.
     * The returned object will be empty if the property file contains no properties, or if there
     * was a problem reading the properties file.
     */
    public java.util.Properties getProperties(String propertiesName) {

        if( props.containsKey(propertiesName))
            return props.get(propertiesName);

        java.util.Properties pp = load(propertiesName);

        if(pp.size() > 0)
            props.put(propertiesName, pp);

        logger.debug("retrieved property "+pp);

        return pp;
    }
    
    /**  Gets a set of properties
    *
    * @param propertiesName name of properties file
    * @return a properties object containing properties from the file with the specified file name.
    * The returned object will be empty if the property file contains no properties, or if there
    * was a problem reading the properties file.
    */
   public java.util.Properties getProperties(File propertiesName) {

       if( props.containsKey(propertiesName.getName()))
           return props.get(propertiesName.getName());

       java.util.Properties pp = load(propertiesName);

       if(pp.size() > 0)
           props.put(propertiesName.getName(), pp);

       logger.debug("retrieved property "+pp);

       return pp;
   }

    /** Searches for the property with the specified key in the set of properties belonging to the
     * specified file. The method returns null if the property is not found.
     *
     * @param propertiesName name of a properties file
     * @param key key in the specified properties file
     * @return value of the key in the specified properties file, or null if the specified key is not
     * found or its value is unspecified in the properties file.
     */
    public String getValue(String propertiesName, String key) {

        return getProperties( propertiesName ).getProperty( key);

    }
    
    /** Searches for the property with the specified key in the set of properties belonging to the
     * specified file. The method returns null if the property is not found.
     *
     * @param propertiesName name of a properties file
     * @param key key in the specified properties file
     * @return value of the key in the specified properties file, or null if the specified key is not
     * found or its value is unspecified in the properties file.
     */
    public String getValue(File propertiesName, String key) {

        return getProperties( propertiesName ).getProperty(key);

    }

    /** Sets a property value. If a number of property values are being set and all of them need to
     * be made persistent, it is more efficient to set the persistent parameter to false in this
     * method call and then make a final call to the setPersistent method once all the values have
     * been set.
     *
     * @param propertiesName name of a properties file
     * @param key key in the specified properties file
     * @param value of the key in the specified properties file
     * @param persistent true if the value should be made persistent, else false.
     *
     */
    public synchronized void setValue(String propertiesName, String key, String value, boolean persistent) {

        getProperties( propertiesName ).setProperty(key, value);
        logger.debug("stored property "+key+"="+value+" for property set "+propertiesName);
        if(persistent)
            setPersistent(propertiesName);

    }

    /** Store the properties in the file with the specified file name (persistent memory).
     *
     * @param propertiesName Properties file name
     */
    public synchronized void setPersistent(String propertiesName) {
        store(propertiesName);
    }

    /** Stores values in the properties file with the specified name.
     *
     * @param name String name of properties file
     */
    private void store(String name) {
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        URL url = loader.getResource(name);
        String fileName = url.getFile();

        OutputStream outputStream = null;

        try {
            outputStream = new FileOutputStream(fileName);
        } catch(FileNotFoundException fnfe) {
            logger.error("Unable to locate properties file "+ name+
                    ". check if name is correct and if file is present at the specified location.");
        }

        java.util.Properties properties = props.get(name);

        try {
            properties.store(outputStream, "");
        } catch(IOException ioe) {
            logger.error("Unable to load properties file "+ name+
                    ", possibly due to failed or interrupted I/O operation.");
        }
        logger.debug("stored properties into file "+name);
    }

    /** Loads a set of properties from a file with the specified ame.
     *
     * @param name file name.
     * @return A Properties object containing a set of properties, or an empty object if no
     * properties were found, or there was a problem reading the properties from the properties file.
     */
    private java.util.Properties load(String name) {

        java.util.Properties properties = new java.util.Properties();
        InputStream inputStream;

        ClassLoader loader = ClassLoader.getSystemClassLoader();
        inputStream = loader.getResourceAsStream(name);

        try {
            if(inputStream != null) {
                properties.load(inputStream);
            } else {
                logger.error("Unable to load properties from file "+name);
            }
        } catch(IOException ioe) {
            logger.error("Unable to load properties file "+ name+
                    ", possibly due to failed or interrupted I/O operation.");
        }

        return properties;
    }
   
    /** Loads a set of properties from a file with the specified ame.
    *
    * @param name file name.
    * @return A Properties object containing a set of properties, or an empty object if no
    * properties were found, or there was a problem reading the properties from the properties file.
    */
   private java.util.Properties load(File name) {
	   java.util.Properties properties = new java.util.Properties();
       try {    	   
           InputStream inputStream=new FileInputStream(name);
           properties.load(inputStream);
       } catch(IOException ioe) {
           logger.error("Unable to load properties file "+ name.getAbsolutePath()+
                   ", possibly due to failed or interrupted I/O operation.");
       }
       return properties;
   }
}
