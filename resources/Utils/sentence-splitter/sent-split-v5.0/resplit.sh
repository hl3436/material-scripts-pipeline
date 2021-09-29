#!/bin/bash
set -e -o pipefail
if [ ${#1} == 0 ]; then
  echo "Split sentences inside existing lines." 1>&2
  echo "Usage: $0 language" 1>&2
  echo "Where language is ISO 639-1" 1>&2
  exit 1
fi
dir="$(dirname "$0")"
sed 's/\x0D$//' | sed 's/^ *$/<P>/' |"$dir"/split-sentences.perl -l $1 -q -s 50 |sed 's/^<P>$//'
