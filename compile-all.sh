#!/usr/bin/env bash
./dockcross/dockcross-linux-armv5 bash -c './compile.sh Linux arm'
./dockcross/dockcross-linux-armv7 bash -c './compile.sh Linux armv7'
./dockcross/dockcross-linux-arm64 bash -c './compile.sh Linux aarch64'
./dockcross/dockcross-linux-x86 bash -c './compile.sh Linux x86'
./dockcross/dockcross-linux-x64 bash -c './compile.sh Linux x86_64'
./dockcross/dockcross-linux-ppc64le bash -c './compile.sh Linux ppc64'

./dockcross/dockcross-windows-static-x86 bash -c './compile.sh Windows x86'
./dockcross/dockcross-windows-static-x64 bash -c './compile.sh Windows x86_64'

docker run --rm -v $(pwd):/workdir -e CROSS_TRIPLE=x86_64-apple-darwin multiarch/crossbuild ./compile.sh Mac x86_64 /workdir/multiarch-darwin.cmake
