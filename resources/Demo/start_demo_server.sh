# $1: absolute path to the docker-compose.deploy.yml file (for the CLIR server)
# $2: NIST-data directory
# $3: Work direcotry. It should contains the experiment config file
# $4: output directory
# $5: instance name
# $6: summarizer  version (ex: summarizer:v1.9)
##########Start CLIR server#########################################
docker-compose -f $1 -p elasticscripts up -d
sleep 30
docker run --rm --network=elasticscripts_default index:v1.2 &

##########Start summarization server################################
docker run --entrypoint server_demo --env OMP_NUM_THREADS=2 -v $2:/NIST-data -v $3:/experiment -v $4:/outputs --user "$(id -u):$(id -g)" --rm --name $5 $6
