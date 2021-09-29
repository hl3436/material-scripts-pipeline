# $1: port number
# $2: root to the index directory {EX: /storage/data/NIST-data}
# $3: pathe to the direcotry containing the ouptut from the query-processor
# $4: path to a work directory. It'll also contain the output.
# $5: instance name
# $6: version

echo "Matcher Server Command: docker run --rm -p $1:$1 -e \"PORT=$1\" -v $2:/media/index  -v $3:/media/queries -v $4:/media/log_dir --name $5 $6"
docker run --rm -p $1:$1 -e "PORT=$1" -v $2:/media/index  -v $3:/media/queries -v $4:/media/log_dir --name $5 $6
