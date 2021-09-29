#!/bin/bash
set -eu
# $1: absolute path to the input directory containing all the files required to be translated
# $2: absolute path to the output directory
# $3: version (Docker version images to use. ex: scriptsmt/systems:v21.2.2)
# $4: source language {tl, sw, en,...}
# $5: target language {tl, sw, en,...}
# $6: input type {text, audio}
# $7: mode {fast, accurate}
# $8: GPU index (ex: 0,1,2,3)
# $9: split script absolute path

# The output from Edi-NMT has root ownership. We want to fix this problem in this section
# uN=${USER}
# uID="$(id -u $uN)"
# gID="$(id -g $uN)"
gpu=$8



# nbest is zero based. So if =4, it will return 5 soultions
if [ $7 = "fast" ]; then
    echo "EDINMT Command: docker run --gpus all  --rm -v $1:/mt/input_dir -v $2:/mt/output_dir -e NBEST_WORDS=1 -e TYPE=$6 -e MODE=fast -e DEVICES=0  $3 translate $4 $5 /mt/input_dir /mt/output_dir --mini-batch-words 200"
    docker run --gpus all  --rm -v $1:/mt/input_dir -v $2:/mt/output_dir -e NBEST_WORDS=1 -e TYPE=$6 -e MODE=$7 -e DEVICES=0 $3 translate $4 $5 /mt/input_dir /mt/output_dir --mini-batch-words 200
    # docker run --rm  -v $2:/project busybox chown -R ${uID}.${gID} /project
    docker run --rm  -v $2:/project busybox chmod -R 777 /project
    echo "EDINMT Command: python3 $9 $2 $nBest"
    python3 $9 $2 0
else
    nBest=4
    echo "EDINMT Command: docker run --gpus all  --rm -v $1:/mt/input_dir -v $2:/mt/output_dir -e NBEST_WORDS=1 -e NBEST=$nBest -e TYPE=$6 -e MODE=fast -e DEVICES=0 $3 translate $4 $5 /mt/input_dir /mt/output_dir --mini-batch-words 200"
    docker run --gpus all  --rm -v $1:/mt/input_dir -v $2:/mt/output_dir -e NBEST_WORDS=1 -e NBEST=$nBest -e TYPE=$6 -e MODE=$7 -e DEVICES=0 $3 translate $4 $5 /mt/input_dir /mt/output_dir --mini-batch-words 200
    # docker run --rm  -v $2:/project busybox chown -R ${uID}.${gID} /project
    docker run --rm  -v $2:/project busybox chmod -R 777 /project
    echo "EDINMT Command: python3 $9 $2 $nBest"
    python3 $9 $2 $nBest
fi

# docker run --rm  -v $2:/project busybox chown -R ${uID}.${gID} /project
docker run --rm  -v $2:/project busybox chmod -R 777 /project

