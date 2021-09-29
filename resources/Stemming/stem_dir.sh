# $1: input directory
# $2: output directory
# $3: log directory
# $4: the list of stemmers you wish to run on the documents separated by spaces(ex: char_3_gram char_4_gram char_5_gram)
# $5: version (ex: document-stemmer:v1.0)
nGrams="$4"
docker run --rm -e "STEMMER=$nGrams" -v $1:/media/in_dir/ -v $2:/media/out_dir/ -v $3:/media/log_dir/ $5
