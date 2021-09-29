# $1: port number
# $2: work directory
# $3: out directory
# $4: instance name
# $5: version

echo "QP Server Command: docker run --rm -p $1:$1 -e \"PORT=$1\" -v $2:/client/in_dir/ -v $3:/client/out_dir/  --name $4 $5"
docker run --rm -p $1:$1 -e "PORT=$1" -v $2:/client/in_dir/ -v $3:/client/out_dir/  --name $4 $5

