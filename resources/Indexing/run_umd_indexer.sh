# $1: data_store_structure.txt path
# $2: indri parameter file name. Ex:indri_index_param.txt
# $3: Path to $2
# $4: data root path (Ex: the path to the directory containing NIST-data)
# $5: path to log directory
# $6: version

echo "INDEXER Command: docker run --rm -e \"INDEX_PARAMS=$2\"  -e \"REINDEX=False\" -v $1:/media/data/data_store_structure.txt -v $3:/media/params_dir -v $4:/media/data/source -v $5:/media/log_dir $6"
docker run --rm -e "INDEX_PARAMS=$2"  -e "REINDEX=False" -v $1:/media/data/data_store_structure.txt -v $3:/media/params_dir -v $4:/media/data/source -v $5:/media/log_dir $6


# version 1.1
#docker run --rm -e "INDRI_PARAMS=$2" -v $1:/media/data/data_store_structure.txt -v $3:/media/params_dir -v $4:/media/data/source -v $5:/media/log_dir $6


# version 1
## $1: data_store_structure.txt path
## $2: indri_index_param.txt path
## $3: data root path (Ex: the path to the directory containing NIST-data)
## $4: version
#
#
#docker run --rm -v $1:/media/data/data_store_structure.txt -v $2:/media/data/indri_index_param.txt -v $3:/media/data/source $4
