# $1: input Directory
# $2: output Directory
# $3: language {sw, tl, so, lt, bg}
# $4: version
docker run  --rm -v $1:/root/in -v $2:/root/out $4 $3 /root/in /root/out
