#!/bin/sh

# Scripts takes only one argument, the absolute path to the directory that contains NIST-data, configs, and properties directory.
# Example:
#     $ sh init.sh /storage/data


if [ -z "$1" ]; then
    echo "No arguments supplied"
    exit 1
fi


docker run --rm -it --name scripts-pipeline --gpus all \
    -v $1:$1 \
    -v /var/run/docker.sock:/var/run/docker.sock \
    -v $1/properties/scripts.properties:/app/scripts-release-20210929-0.1/config/scripts.properties \
    scripts-pipeline:v1.5 $1
