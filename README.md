crfpp-java
===

A Java JNI wrapper for [CRF++](https://github.com/taku910/crfpp) which is an open source C++ implementation of Conditional Random Fields (CRF) a machine learning algorithm for segmenting/labeling sequential data.

`crfpp-java` is a Java library that can be used across operating systems because it contains pre-compiled native libraries of CRF++ for Windows/Mac/Linux (For both 32-bit and 64-bit). At runtime, It will auto-detects your machine environment and loads the native library accordingly.

How to build
---

To build `crfpp-java`, you need to install JDK (1.6 or higher) and Maven.

For example, in Ubuntu 16.04, you might need to install JDK and Maven like this:
```
sudo apt-get install openjdk-8-jdk
sudo apt-get install maven
```

Then run the following command:

```
mvn package
```

The file `target/crfpp-java-$(version).jar` will be created. It is a non-executable JAR file that you can add into your classpath when compiling your Java code.

Alternatively, if you use Maven to manage dependencies, you can build and install `crfpp-java` into your Maven's local repository with the following command:

```
mvn install
```

Using with Maven
----------------

- `crfpp-java` is not available in Maven's central repository, but you can run `mvn install` to build and install it into your Maven's local repository.

Add the following dependency to your pom.xml and specify the version number.

```xml
<dependency>
  <groupId>org.chasen.crfpp</groupId>
  <artifactId>crfpp-java<artifactId>
  <version>0.57</version>
</dependency>
```

Usage
-----

First, import `org.chasen.crfpp.Tagger` in your code:

`import org.chasen.crfpp.Tagger;`

Then create a new Tagger and use `Tagger#add(String)` and `Tagger#parse()` to add and parse context respectively.

```java
Tagger tagger = new Tagger("-m modelfile");
tagger.add("Confidence NN");
tagger.add("in IN");
tagger.add("the DT");
...
tagger.parse();
```

Finally, you can get tagging result by using `Tagger#size()`, `Tagger.xsize()`, `Tagger#yname(int)`, `Tagger#prob(int, int)`, etc.

```java
System.out.println("conditional prob=" + tagger.prob() + " log(Z)=" + tagger.Z());
for (int i = 0; i < tagger.size(); ++i) {
  for (int j = 0; j < tagger.xsize(); ++j) {
    System.out.print(tagger.x(i, j) + "\t");
  }
  System.out.print(tagger.y2(i) + "\t");
  System.out.print("\n");

  System.out.print("Details");
  for (int j = 0; j < tagger.ysize(); ++j) {
    System.out.print("\t" + tagger.yname(j) + "/prob=" + tagger.prob(i,j)
     + "/alpha=" + tagger.alpha(i, j) + "/beta=" + tagger.beta(i, j));
  }
  System.out.print("\n");
}

// when -n20 is specified, you can access nbest outputs
System.out.println("nbest outputs:");
for (int n = 0; n < 10; ++n) {
  if (! tagger.next())
    break;
  System.out.println("nbest n=" + n + "\tconditional prob=" + tagger.prob());
  // you can access any information using tagger.y()...
}
System.out.println("Done");
```

Using with your own compiled library file
---

The `crfpp-java` searches for native libraries (e.g `CRFPP.dll` on Windows, `libCRFPP.so` on Linux, etc.) according to the user platform (`os.name` and `os.arch`).

Even though, the natively compiled libraries are bundled into `crfpp-java`, you can still use your own compiled library file.

`crfpp-java` searches for native libraries in the following order:

1. If the system property `org.chasen.crfpp.use.systemlib` is set to `true`, it will lookup folders specified by `java.lib.path` system property (This is the default path that JVM searches for native libraries).

2. (System property: `com.chasen.crfpp.lib.path`)/(System property: `com.chasen.crfpp.lib.name`).

3. One of the bundled libraries in the JAR file extracted into the folder specified by `java.io.tmpdir`. If the system property `org.chasen.crfpp.tempdir` is set, use this folder instead of `java.io.tmpdir`.
