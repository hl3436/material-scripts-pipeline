################################################version  v10.3 and up###################################################
# $1: input tsv file name
# $2: target language {tl, sw, so, lt, bg}
# $3: input direcotry that contain $1
# $4: out directory
# $5: out log directory
# $6: instance name
# $7: version
# $8: network name
# $9: MT server specs in this format instancename:portNumber (ex: edinmt:3000)
# $10:query expansion flag. If false --> no expansion. If true --> run expansion

if [ $8 = "null" ]; then
    echo "SA QP1 Command: docker run --rm  -e \"QUERY=$1\" -e \"LNG=$2\" -e \"EdiNMT=$9\" -e \"EXPANSION=${10}\"  -v $3:/media/in_dir/ -v $4:/media/out_dir/ -v $5:/media/log_dir/ --name $6 $7"
    docker run --rm  -e "QUERY=$1" -e "LNG=$2" -e "EdiNMT=$9" -e "EXPANSION=${10}"  -v $3:/media/in_dir/ -v $4:/media/out_dir/ -v $5:/media/log_dir/ --name $6 $7
else
    echo "SA QP2 Command: docker run --rm --network $8 -e \"QUERY=$1\" -e \"LNG=$2\" -e \"EdiNMT=$9\" -e \"EXPANSION=${10}\"  -v $3:/media/in_dir/ -v $4:/media/out_dir/ -v $5:/media/log_dir/ --name $6 $7"
    docker run --rm --network $8 -e "QUERY=$1" -e "LNG=$2" -e "EdiNMT=$9" -e "EXPANSION=${10}"  -v $3:/media/in_dir/ -v $4:/media/out_dir/ -v $5:/media/log_dir/ --name $6 $7
fi



#################################################before version  v10.3###################################################
## $1: input tsv file name
## $2: target language {tagalog, swahili, or somali}
## $3: input direcotry that contain $1
## $4: out directory
## $5: out log directory
## $6: instance name
## $7: version
#
#docker run --rm -e "QUERY=$1" -e "LANG=$2" -v $3:/media/in_dir/ -v $4:/media/out_dir/ -v $5:/media/log_dir/ --name $6 $7
