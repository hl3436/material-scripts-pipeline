# $1: input directory
# $2: output directory
# $3: language {swa, tgl}
# $4: version

docker run --rm  -v $1:/data/ -v $2:/output/  $4 $3 /data/ /output/
