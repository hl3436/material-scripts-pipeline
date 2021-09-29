# $1: folder name of the output of umd query matcher (you can add many folders seprated by plus sign)
# $2: query Id
# $3: experiment configuration file name
# $4: the parent direcotry of $1
# $5: query analyzer output directory
# $6: path to the directory containing $3
# $7: path to NIST-data directory
# $8: path to the output directory
# $9: version

echo "SA EVC Command: docker run --rm -e \"INDIRS=$1\" -e \"QUERY=$2\" -e \"CONFIG=$3\" -v $4:/media/input/experiments -v $5:/media/input/queries -v $6:/media/input/configs  -v $7:/media/input/data  -v $8:/media/output   $9"
docker run --rm -e "INDIRS=$1" -e "QUERY=$2" -e "CONFIG=$3" -v $4:/media/input/experiments -v $5:/media/input/queries -v $6:/media/input/configs  -v $7:/media/input/data  -v $8:/media/output   $9



#Versions 1 and 2
# $1: folder name of the output of umd query matcher (you can add many folders seprated by plus sign)
# $2: cutoff
# $3: input format {tsv, trec}
# $4: output format {tsv, trec}
# $5: The file name to be merged
# $6: the parent direcotry of $1
# $7: the output directory
# $8: version
#
#docker run --rm -e "INDIRS=$1" -e "CUTOFF=$2" -e "INFORMAT=$3" -e "OUTFORMAT=$4" -e "QUERY=$5" -v $6:/media/input_data -v $7:/media/output_data  $8
