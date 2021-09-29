# $1: instance name
# $2: queries list
# $3: number of jobs

queries=`cat $2 | cut -f1`
docker exec $1 summarize_queries_fast "$queries" $3
