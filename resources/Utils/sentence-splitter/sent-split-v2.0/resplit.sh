#!/bin/bash
set -e -o pipefail
dir="$(dirname "$0")"
sed 's/^ *$/<P>/' |"$dir"/split-sentences.perl -l en |sed 's/^<P>$//'
