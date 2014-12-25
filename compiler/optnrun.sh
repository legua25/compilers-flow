#!/bin/bash

out="${1%.*}.opt.ll"

opt -S -std-compile-opts "$1" > $out &&
./run.sh "$out"
