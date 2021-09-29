########################################## summarizer:v3.0 #################################################
# $1: NIST-data directory
# $2: OUTPUT directory
# $3: CLIR Output directory
# $4: Run name
# $5: number of processes
# $6: gpu_id
# $7: summarizer_version


echo "1=$1"
echo "2=$2"
echo "3=$3"
echo "4=$4"
echo "5=$5"
echo "6=$6"
echo "7=$7"

PIPELINE_DIR=$3
OUTPUT_DIR=$2

CLIR=$PIPELINE_DIR/UMD-CLIR-workECDir
NIST_VOL="-v $1:/NIST-data"
EXPERIMENT_VOL="-v $PIPELINE_DIR:/experiment"
CLIR_VOL="-v $CLIR:/clir"
OUTPUT_VOL="-v $OUTPUT_DIR:/outputs"

VERBOSE="-e VERBOSE=true"
run_name=$4
work_dir=$OUTPUT_DIR
num_procs=$5
gpu_id=$6

docker run -v /var/run/docker.sock:/var/run/docker.sock \
        $NIST_VOL $OUTPUT_VOL $CLIR_VOL $EXPERIMENT_VOL --user "$(id -u):$(id -g)" --group-add $(stat -c '%g' /var/run/docker.sock) --rm $VERBOSE \
        --name sumtest --ipc=host $7 $run_name $work_dir $num_procs $gpu_id
