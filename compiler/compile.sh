#!/bin/bash

lib_c_path="./lib-c"
lib_src="$lib_c_path/base.c"
lib_bin="$lib_c_path/base.so"

if [ ! -f "$lib_bin" ] || [ "$lib_bin" -ot "$lib_src" ]; then
  gcc -shared -fPIC -std=c99 "$lib_src" -o "$lib_bin"
fi

tmp="${1%.*}.tmp.ll"

java -jar flowc.jar -o $tmp "$1" &&
opt -S -std-compile-opts "$1" > "$2" &&
rm $tmp
