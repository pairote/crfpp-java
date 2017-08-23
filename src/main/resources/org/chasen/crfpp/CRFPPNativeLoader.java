package org.chasen.crfpp;

import java.util.HashMap;

public class CRFPPNativeLoader {
  
  private static HashMap<String, Boolean> loadedLibFiles = new HashMap<String, Boolean>();
  private static HashMap<String, Boolean> loadedLib = new HashMap<String, Boolean>();
  
  public static synchronized void load(String lib) {
    if (loadedLibFiles.containsKey(lib) && loadedLibFiles.get(lib) == true)
      return;
    
    try {
      System.load(lib);
      loadedLibFiles.put(lib, true);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  public static synchronized void loadLibrary(String libname) {
    if (loadedLib.containsKey(libname) && loadedLib.get(libname) == true)
      return;
    
    try {
      System.loadLibrary(libname);
      loadedLib.put(libname, true);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
