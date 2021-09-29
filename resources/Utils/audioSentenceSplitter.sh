# $1: absolute path of the input directory that contains the ASR output
# $2: absolute path of the output directory
# $3: language {tl,sw,so,lt,bg}
# $4: version (ex: audio-sentence-splitter:v1.0)
#docker run --rm -v $1:/input -v $2:/output $4 $3
docker run --gpus all --rm -v $1:/input -v $2:/output $4 $3
