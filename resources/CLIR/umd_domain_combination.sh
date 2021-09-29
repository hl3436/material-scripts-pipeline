# $1: input root directory (EX:  /storage/data/NIST-data)
# $2: all paths required to be merged under $1 to the domain-modeling-output.csv seprated by plus sign
# $3: output directory
# $4: language {sw, tl, so}
# $5: version


echo "Domain Comb Command: docker run  --rm -e \"RESULTS=$2\" -e \"LANGUAGE=$4\" -v $1/:/media/inputdir -v $3:/media/outputdir  $5"
docker run  --rm -e "RESULTS=$2" -e "LANGUAGE=$4" -v $1/:/media/inputdir -v $3:/media/outputdir  $5
