#!/bin/bash

out="${1%.*}.ll"

./compile.sh $1
./optnrun.sh $out
