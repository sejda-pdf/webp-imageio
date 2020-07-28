#!/usr/bin/env bash
echo Building for OS: $1, Arch: $2, Toolchain: $3
mkdir -p build/$1/$2
cd build/$1/$2

if [ -z "$3" ]; then
  cmake ../../..
else
  cmake ../../.. -DCMAKE_TOOLCHAIN_FILE=$3
fi

cmake --build . --config Release

cd ../../..

LIB="libwebp-imageio.so"
if [ "$1" == "Windows" ]; then
  LIB="webp-imageio.dll"
elif [ "$1" == "Mac" ]; then
  LIB="libwebp-imageio.dylib"
fi

mkdir -p src/main/resources/native/$1/$2/
cp build/$1/$2/src/main/c/${LIB} src/main/resources/native/$1/$2/
