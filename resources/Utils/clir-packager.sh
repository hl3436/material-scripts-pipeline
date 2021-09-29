# $1: The directory name of the evidence combiantion output
# $2: Experiment json file name
# $3: The parent directory of $1 and $2
# $4: out directory
# $5: version

docker run --rm -e "INDIR=$1" -e "CONFIG=$2" -v $3/:/media/input/ -v $4:/media/output  $5
