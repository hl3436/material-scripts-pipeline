############################# version 19 and up#############################
#!/bin/bash

# $1: input lang {tl, sw, en}
# $2: output lang {tl, sw, en}
# $3: input source type {text, audio}
# $4: input file (absolute path to the input file)
# $5: output file (absolute path to the output file without extension)
# $6: name (same instance name used while initializing the server)
# $7: mode {fast, accurate}
# $8: split script absolute path
nBest=5
if [ $7 = "fast" ]; then
    docker exec -e "TYPE=$3" -i $6 /mt/translate-client-nbest-words-json.sh $1 $2 < $4 > $5.json.bk
    python3 $8 $5.json.bk $5.tmp 1
    docker exec -i $6 /mt/nbest-scores2-onebest-flat.sh $1 $2 < $5.tmp.0 > $5
    rm $5.tmp.1
else
    docker exec -e "TYPE=$3" -i $6 /mt/translate-client-nbest-words-json.sh $1 $2 < $4 > $5.json
    python3 $8 $5.json $5 $nBest
    docker exec -i $6 /mt/nbest-scores2-onebest-flat.sh $1 $2 < $5.1 > $5.txt
    rm $5.1
    for ((i=2; i<=$nBest;i++))
    do
        docker exec -i $6 /mt/nbest-scores2-onebest-flat.sh $1 $2 < $5.$i > $5.$i.top
        rm  $5.$i
    done
fi

############################# version 4.1 and up#############################
## $1: input lang {tl, sw, en}
## $2: output lang {tl, sw, en}
## $3: input source type {text, audio}
## $4: input file (absolute path to the input file)
## $5: output file (absolute path to the output file without extension)
## $6: name (same instance name used while initializing the server)
## $7: mode {fast, accurate}
## nvidia-docker exec -i $5 /mt/translate-client-nbest-scores.sh $1 $2 < $3 > $4.json
##nvidia-docker exec -e "TYPE=$3" -i $6 /mt/translate-client-nbest-json.sh $1 $2 < $4 > $5.json
#
#if [ $7 = "fast" ]; then
#    docker exec -e "TYPE=$3" -i $6 /mt/translate-client-nbest-json.sh $1 $2 < $4 > $5.json.bk
#    docker exec -i $6 /mt/nbest-scores2-onebest-flat.sh $1 $2 < $5.json.bk > $5
#else
#    docker exec -e "TYPE=$3" -i $6 /mt/translate-client-nbest-json.sh $1 $2 < $4 > $5.json
#    docker exec -i $6 /mt/nbest-scores2-onebest-flat.sh $1 $2 < $5.json > $5.txt
#fi
#
#
############################### version before 4.1#############################
### $1: input lang {tl, sw, en}
### $2: output lang {tl, sw, en}
### $3: input file (absolute path to the input file)
### $4: output file (absolute path to the output file)
### $5: name (same instance name used while initializing the server)
##nvidia-docker exec -i $5 /mt/translate-client.sh $1 $2 < $3 > $4
