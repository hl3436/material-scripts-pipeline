# $1: absolute path to the docker-compose.deploy.yml file (for the CLIR server)
# $2: summarization instance name
##########Shutdown CLIR server#########################################
docker-compose -f $1 -p elasticscripts down

##########Shutdown summarization server################################
docker stop  $2
