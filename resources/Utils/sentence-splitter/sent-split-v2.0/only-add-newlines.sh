#!/bin/bash
set -e -o pipefail
sed 's/^/<P>\n/' | "$(dirname "$0")"/split-sentences.perl -l en |fgrep -vx "<P>"
