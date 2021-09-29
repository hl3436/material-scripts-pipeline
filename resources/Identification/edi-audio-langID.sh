# $1: input directory (The output from the ASR module)
# $2: output directory
# $3: metadata.tsv path
# $4: NB_THRESHOLD for narrow-band data
# $5: WB_THRESHOLD for and wide-band data
# $6: version

docker run --rm -v $3:/opt/app/metadata/metadata.tsv -v $1:/opt/app/asr-output  -v $2:/opt/app/language-verification-output -e NB_THRESHOLD=$4  -e WB_THRESHOLD=$5  $6
