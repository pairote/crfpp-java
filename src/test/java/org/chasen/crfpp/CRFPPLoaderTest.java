package org.chasen.crfpp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import org.codehaus.classworlds.ClassRealm;
import org.codehaus.classworlds.ClassWorld;
import org.junit.Test;

public class CRFPPLoaderTest {
  
  @Test
  public void loadCRFPPByDifferentClassLoadersInTheSameJVM() throws Exception {
    
    // Parent class loader cannot see CRFPP.class
    ClassLoader parent = this.getClass().getClassLoader().getParent();
    ClassWorld cw = new ClassWorld();
    ClassRealm P = cw.newRealm("P", parent);
    
    try {
      P.loadClass("org.chasen.crfpp.CRFPP");
      fail("org.chasen.crfpp.CRFPP is found in the parent");
    }
    catch (ClassNotFoundException e) {
      // OK
    }
    
    // Prepare the child class loaders which can load CRFPP.class
    URL classPath = new File("target/classes").toURI().toURL();
    ClassRealm L1 = cw.newRealm("l1", URLClassLoader.newInstance(new URL[] { classPath }, parent));
    ClassRealm L2 = cw.newRealm("l2", URLClassLoader.newInstance(new URL[] { classPath }, parent));
    
    // Actually load Tagger.class in a child class loader
    Class<?> S1 = L1.loadClass("org.chasen.crfpp.CRFPP");
    Method m = S1.getMethod("getVersion");
    String v = (String) m.invoke(null);
    
    // Load Tagger.class from another child class loader
    Class<?> S2 = L2.loadClass("org.chasen.crfpp.CRFPP");
    Method m2 = S2.getMethod("getVersion");
    String v2 = (String) m2.invoke(null);
    
    assertEquals(v, v2);
  }
  
  @Test
  public void load() {
    CRFPPLoader.load();
  }
  
  @Test
  public void autoload() {
    // This will load the library implicitly
    new Tagger("-m ./src/test/resources/test-model");
  }
  
  public static void main(String[] args) {
    // Test for loading native library specified in -Djava.library.path
    System.setProperty(CRFPPLoader.KEY_CRFPP_USE_SYSTEMLIB, "true");
    new Tagger("-m ./src/test/resources/test-model");
  }
  
}
