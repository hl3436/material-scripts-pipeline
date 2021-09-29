#!/bin/bash
# $1: ASR output directory absolute path
# $2: N-best size
# $3: version (ex: material/nbest:v2.0)

docker run --rm -v $1:/opt/app/asr-output -e "N=$2" $3
