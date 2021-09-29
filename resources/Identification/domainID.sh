# $1: input directory
# $2: output directory
# $3: language {tl,sw,so}
# $4: version
#docker run --rm -e "INDIR=/media/inputdir" -v $1:/media/inputdir -v $2:/media/output-data  $3
docker run --rm -v $1:/media/inputdir -v $2:/media/output-data $4 $3 /bin/bash
