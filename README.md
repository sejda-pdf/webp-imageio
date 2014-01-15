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
- Install CMake 3.8 or newer. CMake can be downloaded from www.cmake.org or installed using
  your systems package manager.
- Download [libwebp 0.4](https://webp.googlecode.com/files/libwebp-0.4.0.tar.gz) and extract it into the project directory
- Run 'cmake .' in the root of directory of the project to generate the build scripts for your system.
- Build the project using the generated build scripts.
- The build scripts will generate a number of binaries
    - java/webp-imageio.jar: JAR file containing the Image I/O reader and writer
    - c/libwebp-imageio.so: the JNI library that is required by webp-imageio.jar
