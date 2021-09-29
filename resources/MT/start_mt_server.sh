# $1: input lang {tl, sw, en, so}
# $2: output lang {tl, sw, en, so}
# $3: input source type {text, audio}
# $4: processing mode {fast, accurate}
# $5: name (the instance name. It can be anything: ex: scriptsmt-system-v-3-instance-1)
# $6: version (Docker version images to use. ex: scriptsmt/systems:v3.0)
# $7: gpu idx (ex: 0)
#nvidia-docker run --rm -e "TYPE=$3" --name $4 $5 $1$2:$6 &


gpu=$7

echo "SERVER COMMAND docker run --gpus all --rm -e "TYPE=$3" -e "MODE=$4" --name $5 $6 $1$2:${gpu} &"
docker run --gpus all --rm -e "TYPE=$3" -e "MODE=$4" --name $5 $6 $1$2:${gpu} &

