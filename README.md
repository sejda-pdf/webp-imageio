# Description
[Java Image I/O](http://docs.oracle.com/javase/7/docs/api/javax/imageio/package-summary.html) reader and writer for the
[Google WebP](https://developers.google.com/speed/webp/) image format.

# License
webp-imageio is distributed under the [Apache Software License](https://www.apache.org/licenses/LICENSE-2.0) version 2.0.

# Usage
- Add webp-imageio.jar to the classpath of your application
- Ensure libwebp-jni.so or webp-jni.dll is accessible on the Java native library path (java.library.path system property)
- The WebP reader and writer can be used like any other Image I/O reader and writer.

# Compiling

## Compiling the native library
- Install CMake 2.8 or newer. CMake can be downloaded from www.cmake.org or installed using
  your systems package manager.
- Create a directory called 'build' in the root of the project
- Open a terminal and navigate to the newly created 'build' directory
- Run 'cmake ..' in the 'build' directory to generate the build scripts for your system.
- 'cmake --build .' to compile the library
- The compiled library can be found under the directory 'build/src/main/c'

## Compiling the Java library

### Using Buildr
- Install [Buildr](http://buildr.apache.org)
- Run 'buildr package test=false' in the root of the project
- The compiled Java library can be found under the 'target' directory

### Using Maven
- Install [Maven](http://maven.apache.org)
- Run 'mvn -Dmaven.test.skip=true package' in the root of the project
- The compiled Java library can be found under the 'target' directory