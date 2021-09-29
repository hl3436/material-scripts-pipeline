# $1: input directory
# $2: output directory
# $3: version (ex: mt_augmentation:v1.0)
docker run --rm --gpus all -v $1:/media/input -v $2:/media/output $3


#========================================Old postedit
# $1: input directory
# $2: output directory
# $3: langauge {sw, tl ,so}
# $4: version (ex: cu-postedit:v1.0)
#docker run --rm -e "lang=$3" -v $1:/path_to_input_directory -v $2:/path_to_output_directory  $4
