#!/bin/bash

lib_c_path="./lib-c"
lib_bin="$lib_c_path/base.so"

lli -load="$lib_bin" $1
