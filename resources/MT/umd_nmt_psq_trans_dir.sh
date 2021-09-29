# $1: absolute path to the input directory containing all the files required to be translated
# $2: absolute path to the output directory
# $3: version (Docker version images to use. ex: umd-nmt-psq:v1.0)
# $4: source language {ps}
# $5: target language {en}
# $6: input type {text, audio}
# $7: GPU index (ex: 0)
# $8: number of threads (ex: 2. It takes 2 GB per 1 thread. So check the avilable Ram in the GPU first)

nBest=50
docker run --gpus all --rm -v $1:/app/input -v $2:/app/output $3 $4 $5 $6 $nBest $7 $8
