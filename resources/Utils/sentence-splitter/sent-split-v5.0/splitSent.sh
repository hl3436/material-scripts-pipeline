#!/bin/bash
# $1: path of the directory containing resplit.sh
# $2: input file
# $3: output file
# $4: language {fa, bg, sw, ...}
bash $1/resplit.sh $4 <$2 >$3
