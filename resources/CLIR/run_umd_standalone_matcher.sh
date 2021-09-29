####################################Version 11.1 and up############################################################################################################
# $1: root to the index directory {EX: /storage/data/NIST-data}
# $2: path to the direcotry containing the output from the query-processor
# $3: path to a work directory. It'll also contain the output.
# $4: instance name
# $5: version
# $6: input file name {located under $2}
# $7: index location {you can add many indexes seprated by plus sign}. Located under $1
# $8: type {PSQ, words, ...}. Check with the CLIR team for all supported types
# $9: cutoff {-1 means don't do cutoff}
# $10: target language {tl, sw, so}
# $11: MAtcher name (ex: PSQ)
# $12: config file name
# $13: path to the directory containing $12

echo "SA Matcher Command: docker run --rm -e \"QUERY=$6\" -e \"TYPE=$8\" -e \"INDEX=$7\" -e \"CUTOFF=$9\" -e \"LANG=${10}\" -e \"CONFIG=${12}\" -e \"NAME=${11}\"  -v $1:/media/index -v $2:/media/queries -v $3:/media/log_dir -v ${13}:/media/input/configs --name $4 $5"
docker run --rm -e "QUERY=$6" -e "TYPE=$8" -e "INDEX=$7" -e "CUTOFF=$9" -e "LANG=${10}" -e "CONFIG=${12}" -e "NAME=${11}"  -v $1:/media/index -v $2:/media/queries -v $3:/media/log_dir -v ${13}:/media/input/configs --name $4 $5

#####################################Version 11.0 ############################################################################################################
## $1: root to the index directory {EX: /storage/data/NIST-data}
## $2: path to the direcotry containing the output from the query-processor
## $3: path to a work directory. It'll also contain the output.
## $4: instance name
## $5: version
## $6: input file name {located under $2}
## $7: index location {you can add many indexes seprated by plus sign}. Located under $1
## $8: type {PSQ, words, ...}. Check with the CLIR team for all supported types
## $9: cutoff {-1 means don't do cutoff}
## $10: target language {tl, sw, so}
## $11: output format {tsv, json}
#
#docker run --rm -e "QUERY=$6" -e "TYPE=$8" -e "INDEX=$7" -e "CUTOFF=$9"  -e "LANG=${10}" -e "OUTPUT=${11}" -v $1:/media/index -v $2:/media/queries -v $3:/media/log_dir --name $4 $5

#####################################Before version 11.0############################################################################################################
## $1: root to the index directory {EX: /storage/data/NIST-data}
## $2: path to the direcotry containing the output from the query-processor
## $3: path to a work directory. It'll also contain the output.
## $4: instance name
## $5: version
## $6: input file name {located under $2}
## $7: index location {you can add many indexes seprated by plus sign}. Located under $1
## $8: type {PSQ, words, ...}. Check with the CLIR team for all supported types
## $9: cutoff {-1 means don't do cutoff}
## $10: output format {tsv, json}
#
#docker run --rm -e "QUERY=$6" -e "TYPE=$8" -e "INDEX=$7" -e "CUTOFF=$9" -e "OUTPUT=${10}" -v $1:/media/index -v $2:/media/queries -v $3:/media/log_dir --name $4 $5

