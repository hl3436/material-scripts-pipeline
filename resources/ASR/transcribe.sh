#!/bin/bash
# $1: number of jobs
# $2: number of threads
# $3: input directory
# $4: metadata.tsv path
# $5: output directory
# $6: version



docker run --rm \
-v $3:/opt/app/input:ro \
-v $4:/opt/app/metadata/metadata.tsv:ro \
-v $5:/opt/app/output \
-e NUMBER_OF_JOBS=$1 \
-e NUMBER_OF_THREADS=$2 \
$6
