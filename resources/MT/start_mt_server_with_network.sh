# $1: input lang {tl, sw, en, so}
# $2: output lang {tl, sw, en, so}
# $3: input source type {text, audio}
# $4: processing mode {fast, accurate}
# $5: name (the instance name. It can be anything: ex: scriptsmt-system-v-3-instance-1)
# $6: network name (It can be anything: ex: mt_network)
# $7: version (Docker version images to use. ex: scriptsmt/systems:v3.0)
# $8: gpu idx (ex: 0)
# $9: port number (ex: 3010) --> obsolete paramter. We don't need it after v20
#docker run --gpus all --rm -e "TYPE=$3" -e "MODE=$4" --name $5 --network $6 $7 $1$2:$8:$9 &

gpu=$8

echo "NET SERVER docker run --gpus all --rm -e TYPE=$3 -e MODE=$4 --name $5 --network $6 -e SRC=$1 -e TGT=$2 -e DEVICES=${gpu} $7 serve &"
docker run --gpus all --rm -e TYPE=$3 -e MODE=$4 --name $5 --network $6 -e SRC=$1 -e TGT=$2 -e DEVICES=${gpu} $7 serve &