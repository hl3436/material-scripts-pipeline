#!/bin/bash
# $1: port number
# $2: input file name
# $3: index location
# $4: type {words, PSQ, wiktionary}
# $5: cutoff
# $6: output format {tsv, trec}
# $7: instance name

echo "Matcher Query Command: docker exec -i $7 python /usr/local/src/client/client.py --port=$1 --query=$2 --index=$3 --type=$4 --cutoff=$5 --output-format=$6"
docker exec -i $7 python /usr/local/src/client/client.py --port=$1 --query=$2 --index=$3 --type=$4 --cutoff=$5 --output-format=$6
