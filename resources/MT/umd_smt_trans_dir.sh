# $1: input directory
# $2: output directory
# $3: version
# $4: source language {tl, sw, en}
# $5: target language {tl, sw, en}
# $6: number of threads

echo "UMDSMT Command: docker run --rm -v $1:/app/input -v $2:/app/output $3 $4 $5 $6"
docker run --rm -v $1:/app/input -v $2:/app/output $3 $4 $5 $6
