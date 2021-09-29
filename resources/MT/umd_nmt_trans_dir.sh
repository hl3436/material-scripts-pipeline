##########################################Version4.1 and up#################################################
# $1: absolute path to the input directory containing all the files required to be translated
# $2: absolute path to the output directory
# $3: version (Docker version images to use. ex: umd-nmt:v2.2)
# $4: source language {tl, sw, en}
# $5: target language {tl, sw, en}
# $6: input type {text, audio}
# $7: GPU index (ex: 0)
# $8: number of threads (ex: 2. It takes 11 GB per 1 thread. So check the avilable Ram in the GPU first)


gpu=$7
nBest=5
#nvidia-docker run --rm -v $1:/app/input -v $2:/app/output $3 $4 $5 $6 $nBest $7 $8
#docker run --gpus all --rm -v $1:/app/input -v $2:/app/output $3 $4 $5 $6 $nBest $7 $8
echo "UMDNMT Command: docker run --gpus all --rm -v $1:/app/input -v $2:/app/output $3 translate $4 $5 $6 $nBest ${gpu} $8"
docker run --gpus all --rm -v $1:/app/input -v $2:/app/output $3 translate $4 $5 $6 $nBest ${gpu} $8


###########################################Version3 and up#################################################
## $1: absolute path to the input directory containing all the files required to be translated
## $2: absolute path to the output directory
## $3: version (Docker version images to use. ex: umd-nmt:v2.2)
## $4: source language {tl, sw, en}
## $5: target language {tl, sw, en}
## $6: GPU index (ex: 0)
## $7: number of threads (ex: 2. It takes 2 GB per 1 thread. So check the avilable Ram in the GPU first)
##    Note: the latest version (umd-nmt:v2.2) needs more ram. So, we just can run 1 thread on the current gpu (M60). More powerful GPUs are needed
#
#nBest=5
#nvidia-docker run --rm -v $1:/app/input -v $2:/app/output $3 $4 $5 $nBest $6 $7








###########################################Before version3#################################################
## $1: absolute path to the input directory containing all the files required to be translated
## $2: absolute path to the output directory
## $3: version (Docker version images to use. ex: umd-nmt:v2.2)
## $4: source language {tl, sw, en}
## $5: target language {tl, sw, en}
## $6: GPU index (ex: 0)
## $7: number of threads (ex: 2. It takes 2 GB per 1 thread. So check the avilable Ram in the GPU first)
##    Note: the latest version (umd-nmt:v2.2) needs more ram. So, we just can run 1 thread on the current gpu (M60). More powerful GPUs are needed
#
#nvidia-docker run --rm -v $1:/app/input -v $2:/app/output $3 $4 $5 $6 $7
