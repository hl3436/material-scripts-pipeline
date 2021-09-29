#################version 7.0 and up####################################################################
# $1: The directory name of the evidence combiantion output
# $2: Experiment json file name
# $3: The parent directory of $1 and $2
# $4: out directory
# $5: The path of the NIST-data directory
# $6: version

#docker run --rm -e "INDIR=$1" -e "CONFIG=$2" -v $3/:/media/input/ -v $4:/media/output  -v $5/:/media/data/relevance_data $6
docker run --rm -e "INDIR=$1" -e "CONFIG=$2" -v $3/:/media/input/ -v $4:/media/output  -v $5/:/media/data/input/  $6



#################Before version 7.0####################################################################
## $1: The directory name of the evidence combiantion output
## $2: Experiment json file name
## $3: beta value {20 if CLIR or 59.9 if CLIR+S}
## $4: The parent directory of $1 and $2
## $5: out directory
## $6: The path of the relevance_judgments directory
## $7: version
#
#docker run --rm -e "INDIR=$1" -e "CONFIG=$2" -e "BETA=$3" -v $4/:/media/input/ -v $5:/media/output  -v $6/:/media/data/relevance_data $7
