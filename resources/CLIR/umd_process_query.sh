# $1: host name (EX: localhost)
# $2: port number (must be same port of the server)
# $3: input tsv file name (The file must be placed in the work directory that is mounted in the server)
# $4: target language {tagalog or swahili}
# $5: instance name

echo "Process Query Command: docker exec -i $5 /client/run_client.sh $1 $2 $3 $4"
docker exec -i $5 /client/run_client.sh $1 $2 $3 $4
