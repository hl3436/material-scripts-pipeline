# $1: path to a work directory. It will also contain the output file.
# $2: config file name. It is located under $1
# $3: Matcher name in $2 (ex: PSQ)
# $4: output file name (ex: matcher_unique_id.txt).
# $5: input type {matcher, ranker}
# $6: version (ex: matcher-unique-id:v1.3 )

docker run --rm -e "CONFIG=$2" -e "matName=$3" -e "idFileName=$4" -e "inputType=$5" -v $1:/media/work  $6







## $1: path to a work directory. It will also contain the output file.
## $2: config file name. It is located under $1
## $3: Matcher name in $2 (ex: PSQ)
## $4: output file name (ex: matcher_unique_id.txt).
## $5: version (ex: matcher-unique-id:v1.0)
#
#docker run --rm -e "CONFIG=$2" -e "matName=$3" -e "idFileName=$4" -v $1:/media/work  $5
