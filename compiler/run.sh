#!/bin/bash

# lib_c_path="./lib-c"
# lib_files=""

# for lib_src in "$lib_c_path"/*.c; do
#   lib_bin="$lib_c_path/library.so"

#   if [ ! -f "$lib_bin" ] || [ "$lib_bin" -ot "$lib_src" ]; then
#     gcc -shared -fPIC -std=c99 "$lib_src" -o "$lib_bin"
#   fi

#   lib_files="-load=$lib_bin $lib_files"
# done

# echo $lib_files
# lli "$lib_files" $1

lib_c_path="./lib-c"
lib_src="$lib_c_path/base.c"
lib_bin="$lib_c_path/base.so"

if [ ! -f "$lib_bin" ] || [ "$lib_bin" -ot "$lib_src" ]; then
  gcc -shared -fPIC -std=c99 "$lib_src" -o "$lib_bin"
fi

lli -load="$lib_bin" $1
