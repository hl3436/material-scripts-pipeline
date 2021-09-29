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

# PIPELINE_DIR=/storage/proj/dw2735/experiments/docker_test/ps/text
# OUTPUT_DIR=/storage/proj/dw2735/summarizer_output/docker_test/ps/text

# CLIR=$PIPELINE_DIR/UMD-CLIR-workECDir
# NIST_VOL="-v /storage/data/NIST-data:/NIST-data"
# EXPERIMENT_VOL="-v $PIPELINE_DIR:/experiment"
# CLIR_VOL="-v $CLIR:/clir"
# OUTPUT_VOL="-v $OUTPUT_DIR:/outputs"

VERBOSE="-e VERBOSE=true"
run_name=$4
work_dir=$OUTPUT_DIR
num_procs=$5
gpu_id=$6


# sh summarize_standalone2.sh /local/nlp/data/NIST-data $(pwd)/summ2 $(pwd)/run14_multipleok/FA_Q2_EVAL_text_clir/query-analyzer-umd-v16.0_matching-umd-v15.4_evidence-combination-v13.4/FA_QUERY2_EVAL_hmm_PSQ_neulex_words_indriinter_words_indri_p_D-tEDINMTbTN_indri_PSQ_indrijp_PSQ_Cutoff46 3

# docker run -it -v /var/run/docker.sock:/var/run/docker.sock \
#     -v $1:/NIST-data -v $2:/outputs -v $3:/clir -v $4:/experiment \
#     -it --user "$(id -u):$(id -g)" --group-add $(stat -c '%g' /var/run/docker.sock) --rm $VERBOSE \
#     --name sumtest --ipc=host summarizer:v3.0 $run_name $work_dir $num_procs $5


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
