#!/bin/bash
# $1: Threshold (use 10.0 for now)
# $2: Output of the ASR system
# $3: Output of query analyzer containing all query sets ( QUERY{1,2,3} )
# $4: output directory
# $5: version


docker run --rm -e "THRESHOLD=$1" -v $2:/opt/app/asr-output -v $4:/opt/app/asr-output-expanded/ -v $3:/opt/app/tmp/doc-exp/queries $5
