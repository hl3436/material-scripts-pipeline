#!/bin/sh

echo "START"

if [ -z "$1" ]; then
    echo "No work directory supplied"
    exit 1
fi

WORKDIR=$(echo $1 | sed 's:/*$::')
echo "WORKDIR=${WORKDIR}"

java -jar scripts-release-20210929-0.1/scripts-release-20210929-0.1.jar \
    -w ${WORKDIR}/workDir \
    -o ${WORKDIR}/output \
    -c ${WORKDIR}/cache \
    -i ${WORKDIR}/configs
echo "COPYING LOG FILES"
cp *.log* ${WORKDIR}/

echo "FIX PERMISSIONS"
chmod -R 777 ${WORKDIR}/
echo "DONE"