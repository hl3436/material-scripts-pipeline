#!/bin/bash
# $1: ASR output directory absolute path
# $2: KWS output directory absolute path
# $3: language {tl, sw, en, so, bg, lt}
# $4: version

docker run --rm -e "NUMBER_OF_JOBS=32" -e "NUMBER_OF_THREADS=8" -e "DOC_LANG=$3" -v $1:/opt/app/asr-output -v $2:/opt/app/kws-output $4

