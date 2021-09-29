#!/bin/bash
set -e -o pipefail
if [ ${#1} == 0 ]; then
  echo "Split sentences inside existing lines." 1>&2
  echo "Usage: $0 language" 1>&2
  echo "Where language is ISO 639-1" 1>&2
  exit 1
fi
"$(dirname "$0")"/split-sentences.perl -q -l $1 -s 50 -k
