package org.chasen.crfpp;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.UUID;

/**
 * The CRFPPLoader searches for native libraries (CRFPP.dll, libCRFPP.so,
 * libCRFPP.dylib, etc.) according to the user platform (<i>os.name</i> and
 * <i>os.arch</i>). Tha natively compiled libraries bundled to crfpp-java
 * contain the codes of the original crfpp and JNI programs to access CRFPP.
 * 
 * The CRFPPLoader searches for native libraries in the following order:
 * <ol>
 * <li>
 * If the system property <i>org.chasen.crfpp.use.systemlib</i> is set to
 * true, it will lookup folders specified by <i>java.lib.path</i> system
 * property (This is the default path that JVM searches for native libraries).
 * </li>
 * <li>
 * (System property: <i>com.chasen.crfpp.lib.path</i>)/(System property:
 * <i>com.chasen.crfpp.lib.name</i>)
 * </li>
 * <li>
 * One of the bundled libraries in the JAR file extracted into the folder
 * specified by <i>java.io.tmpdir</i>. If the system property
 * <i>org.chasen.crfpp.tempdir</i> is set, use this folder instead of
 * <i>java.io.tmpdir</i>.
 * </li>
 * </ol>
 * 
 */
public class CRFPPLoader {
  
  public static final String CRFPP_SYSTEM_PROPERTIES_FILE = "org.chasen.crfpp.properties";
  public static final String KEY_CRFPP_LIB_PATH           = "org.chasen.crfpp.lib.path";
  public static final String KEY_CRFPP_LIB_NAME           = "org.chasen.crfpp.lib.name";
  public static final String KEY_CRFPP_TEMPDIR            = "org.chasen.crfpp.tempdir";
  public static final String KEY_CRFPP_USE_SYSTEMLIB      = "org.chasen.crfpp.use.systemlib";
  
  private static volatile boolean isLoaded = false;
  
  private static File nativeLibFile = null;
  
  static void cleanUpExtractedNativeLib() {
    if (nativeLibFile != null && nativeLibFile.exists())
      nativeLibFile.delete();
  }
  
  /**
   * Load the system properties when the configuration file of the name {@link #CRFPP_SYSTEM_PROPERTIES_FILE} is found.
   */
  private static void loadCRFPPSystemProperties() {
    try {
      InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(CRFPP_SYSTEM_PROPERTIES_FILE);
      
      if (is == null)
        return; // no configuration file is found
        
      // load property file
      Properties props = new Properties();
      props.load(is);
      is.close();
      Enumeration<?> names = props.propertyNames();
      while (names.hasMoreElements()) {
        String name = (String) names.nextElement();
        if (name.startsWith("org.chasen.crfpp.")) {
          if (System.getProperty(name) == null) {
            System.setProperty(name, props.getProperty(name));
          }
        }
      }
    }
    catch (Throwable ex) {
      System.err.println("Cound not load '" + CRFPP_SYSTEM_PROPERTIES_FILE + "' from classpath: " + ex.toString());
    }
  }
  
  static {
    loadCRFPPSystemProperties();
  }
  
  static synchronized void load() {
    if (isLoaded)
      return;
    
    try {
      loadNativeLibrary();
      
      isLoaded = true;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new CRFPPError(CRFPPErrorCode.FAILED_TO_LOAD_NATIVE_LIBRARY, e.getMessage());
    }
  }
  
  /**
   * Load the native library.
   */
  private static void loadNativeLibrary() {
    
    nativeLibFile = findNativeLibrary();
    
    if (nativeLibFile != null) {
      // Load extracted or specified crfpp native library.
      System.load(nativeLibFile.getAbsolutePath());
    }
    else {
      // Load preinstalled crfpp (in the path -Djava.library.path)
      System.loadLibrary("crfpp");
    }
  }
  
  /**
   * Compares the contents of the specified input streams.
   * 
   * @param in1
   * @param in2
   * @return
   * @throws IOException
   */
  private static boolean contentsEquals(InputStream in1, InputStream in2) throws IOException {
    if (!(in1 instanceof BufferedInputStream)) {
      in1 = new BufferedInputStream(in1);
    }
    if (!(in2 instanceof BufferedInputStream)) {
      in2 = new BufferedInputStream(in2);
    }
    
    int ch = in1.read();
    while (ch != -1) {
      int ch2 = in2.read();
      if (ch != ch2) return false;
      ch = in1.read();
    }
    int ch2 = in2.read();
    return ch2 == -1;
  }
  
  /**
   * Extract the specified library file to the target folder
   * 
   * @param libFolderForCurrentOS
   * @param libraryFileName
   * @param targetFolder
   * @return
   */
  private static File extractLibraryFile(String libFolderForCurrentOS, String libraryFileName, String targetFolder) {
    String nativeLibraryFilePath = libFolderForCurrentOS + "/" + libraryFileName;
    
    // Attach UUID to the native library file to ensure multiple class loaders can read the library multiple times.
    String uuid = UUID.randomUUID().toString();
    String extractedLibFileName = String.format("crfpp-java-%s-%s-%s", getVersion(), uuid, libraryFileName);
    File extractedLibFile = new File(targetFolder, extractedLibFileName);
    
    try {
      // Extract a native library file into the target directory
      InputStream reader = CRFPPLoader.class.getResourceAsStream(nativeLibraryFilePath);
      FileOutputStream writer = new FileOutputStream(extractedLibFile);
      try {
        byte[] buffer = new byte[8192];
        int bytesRead = 0;
        while ((bytesRead = reader.read(buffer)) != -1) {
          writer.write(buffer, 0, bytesRead);
        }
      } finally {
        // Delete the extracted lib file on JVM exit.
        extractedLibFile.deleteOnExit();
        
        if (writer != null) writer.close();
        if (reader != null) reader.close();
      }
      
      // Set executable (x) flag to enable Java to load the native library
      extractedLibFile.setReadable(true);
      extractedLibFile.setWritable(true, true);
      extractedLibFile.setExecutable(true);
      
      // Check whether the contents are properly copied from the resource folder
      {
        InputStream nativeIn = CRFPPLoader.class.getResourceAsStream(nativeLibraryFilePath);
        InputStream extractedLibIn = new FileInputStream(extractedLibFile);
        try {
          if (!contentsEquals(nativeIn, extractedLibIn)) throw new CRFPPError(
              CRFPPErrorCode.FAILED_TO_LOAD_NATIVE_LIBRARY, String.format("Failed to write a native library file at %s", extractedLibFile));
        } finally {
          if (nativeIn != null)
            nativeIn.close();
          if (extractedLibIn != null)
            extractedLibIn.close();
        }
      }
      
      return new File(targetFolder, extractedLibFileName);
    }
    catch (IOException e) {
      e.printStackTrace(System.err);
      return null;
    }
  }
  
  static File findNativeLibrary() {
    
    boolean useSystemLib = Boolean.parseBoolean(System.getProperty(KEY_CRFPP_USE_SYSTEMLIB, "false"));
    if (useSystemLib)
      return null; // use a pre-installed library
    
    // Try to load the library in org.chasen.crfpp.lib.path
    String crfppNativeLibraryPath = System.getProperty(KEY_CRFPP_LIB_PATH);
    String crfppNativeLibraryName = System.getProperty(KEY_CRFPP_LIB_NAME);
    
    // Resolve the library file name with a suffix (e.g., dll, .so, etc.)
    if (crfppNativeLibraryName == null)
      crfppNativeLibraryName = System.mapLibraryName("CRFPP");
    
    if (crfppNativeLibraryPath != null) {
      File nativeLib = new File(crfppNativeLibraryPath, crfppNativeLibraryName);
      if (nativeLib.exists())
        return nativeLib;
    }
    
    // Load an OS-dependent native library inside a jar file
    crfppNativeLibraryPath = "/org/chasen/crfpp/native/" + OSInfo.getNativeLibFolderPathForCurrentOS();
    boolean hasNativeLib = hasResource(crfppNativeLibraryPath + "/" + crfppNativeLibraryName);
    
    // Fix for OpenJDK7 for Mac
    if (!hasNativeLib && OSInfo.getOSName().equals("Mac")) {
      String altName = "libCRFPP.jnilib";
      if (hasResource(crfppNativeLibraryPath + "/" + altName)) {
        crfppNativeLibraryName = altName;
        hasNativeLib = true;
      }
    }
    
    if (!hasNativeLib) {
      String errorMessage = String.format("no native library is found for os.name=%s and os.arch=%s", OSInfo.getOSName(), OSInfo.getArchName());
      throw new CRFPPError(CRFPPErrorCode.FAILED_TO_LOAD_NATIVE_LIBRARY, errorMessage);
    }
    
    // Temporary library folder. Use the value of org.chasen.crfpp.tempdir or java.io.tmpdir
    String tempFolder = new File(System.getProperty(KEY_CRFPP_TEMPDIR, 
        System.getProperty("java.io.tmpdir"))).getAbsolutePath();
    
    // Extract and load a native library inside the jar file
    return extractLibraryFile(crfppNativeLibraryPath, crfppNativeLibraryName, tempFolder);
  }
  
  /**
   * Tests whether a resource with a given path is exists.
   * 
   * @param path - The path or name of a resource.
   * @return true if resource with the given name is found, false otherwise.
   */
  private static boolean hasResource(String path) {
    return CRFPPLoader.class.getResource(path) != null;
  }
  
  /**
   * Get the crfpp-java version by reading pom.properties embedded in jar.
   * This version data is used as a suffix of a dll file extracted from the jar.
   * 
   * @return the version string
   */
  public static String getVersion() {
    
    URL versionFile = CRFPPLoader.class.getResource("/META-INF/maven/org.chasen.crfpp/crfpp-java/pom.properties");
    if (versionFile == null)
      versionFile = CRFPPLoader.class.getResource("/org/chasen/crfpp/VERSION");
    
    String version = "unknown";
    try {
      if (versionFile != null) {
        Properties versionData = new Properties();
        versionData.load(versionFile.openStream());
        version = versionData.getProperty("version", version);
        if (version.equals("unknown"))
          version = versionData.getProperty("VERSION", version);
        version = version.trim().replaceAll("[^0-9\\.]", "");
      }
    }
    catch (IOException e) {
      System.err.println(e);
    }
    return version;
  }
}
