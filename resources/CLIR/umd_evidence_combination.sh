# $1: folder name of the output of umd query matcher
# $2: cutoff
# $3: input format {tsv, trec}
# $4: output format {tsv, trec, tgz}
# $5: the parent direcotry of $1
# $6: the output directory
# $7: version

echo "EVC Command: docker run --rm -e \"INDIRS=$1\" -e \"CUTOFF=$2\" -e \"INFORMAT=$3\" -e \"OUTFORMAT=$4\" -v $5:/media/input_data -v $6:/media/output_data  $7"
docker run --rm -e "INDIRS=$1" -e "CUTOFF=$2" -e "INFORMAT=$3" -e "OUTFORMAT=$4" -v $5:/media/input_data -v $6:/media/output_data  $7
