# $1: absolute path to the input directory containing all the files required to be translated
# $2: absolute path to the output directory
# $3: version (Docker version images to use. ex: umd-nmt:v2.2)
# $4: source language {sw, ps}
# $5: input type {text, audio}
# $6: number of processes (ex: 2. It takes ~5 GB per 1 process. So check the avilable Ram in the GPU first)
# $7: GPU index list (ex: 0,1)

gpu=$7
nBest=50
echo "NNLTM Command: docker run --gpus all --rm -v $1:/app/input -v $2:/app/output $3 $4 $nBest $5 $6 ${gpu}"
docker run --gpus all --rm -v $1:/app/input -v $2:/app/output $3 $4 $nBest $5 $6 ${gpu}
