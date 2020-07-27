#!/usr/bin/env bash
./dockcross/dockcross-linux-armv5 bash -c './compile.sh Linux arm'
./dockcross/dockcross-linux-armv7 bash -c './compile.sh Linux armv7'
./dockcross/dockcross-linux-arm64 bash -c './compile.sh Linux aarch64'
./dockcross/dockcross-linux-x86 bash -c './compile.sh Linux x86'
./dockcross/dockcross-linux-x64 bash -c './compile.sh Linux x86_64'
./dockcross/dockcross-linux-ppc64le bash -c './compile.sh Linux ppc64'

./dockcross/dockcross-windows-shared-x86 bash -c './compile.sh Windows x86'
./dockcross/dockcross-windows-shared-x64 bash -c './compile.sh Windows x86_64'