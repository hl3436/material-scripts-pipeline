#!/bin/bash
set -e -o pipefail
dir="$(dirname "$0")"
sed 's/^ *$/<P>/' |"$dir"/split-sentences.perl -l en  -s 50 |sed 's/^<P>$//'
