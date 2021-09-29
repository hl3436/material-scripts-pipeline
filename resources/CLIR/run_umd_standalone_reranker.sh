# $1 is the list of matchers names separated by+ (e.g. UMD-CLIR-workQMDir-psqinter_PSQ+UMD-CLIR-workQMDir-hmm_PSQ+UMD-CLIR-workQMDir-neulex_words)
# $2 config name
# $3 path to log dir
# $4 path to NIST-data
# $5 path to the parent directory of $1
# $6 path to the parent directory of $2
# $7 version (e.g. reranker:v1.0)

docker run --rm --gpus all -e "INDIRS=$1" -e "CONFIG=$2" -v $3:/media/log -v $4:/media/data -v $5:/media/input -v $6:/media/config  $7
