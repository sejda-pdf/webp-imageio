#!/usr/bin/env bash
mkdir -p build/$1/$2
cd build/$1/$2
cmake ../../..
cmake --build .

cd ../../..

LIB="libwebp-imageio.so"
if [ "$1" == "windows" ]; then
  LIB="webp-imageio.dll"
elif [ "$1" == "mac" ]; then
  LIB="libwebp-imageio.dylib"
fi

mkdir -p src/main/resources/native/$1/$2/
cp build/$1/$2/src/main/c/${LIB} src/main/resources/native/$1/$2/
