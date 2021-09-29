# $1: absolute path to query
# $2: source index to use {audio, text}
# $3: maximum number of retrieved documents
# $4: json file name
# $5: absolute path to put $4 in
# $6: summarization instance name
# $7: number of jobs

##############################Call the CLIR##############################
#curl $1 > $3/$2
query=`cat $1`
curl -G --data-urlencode "q=${query}" -d "source=$2" -d "size=$3" http://localhost:5000/search > $5/$4
##############################Call the summrizer#########################
docker exec $6 summarize_queries_demo $4 $7
