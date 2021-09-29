#!/usr/bin/env python3
import json
import sys
import io

if len(sys.argv) == 1:
  sys.stderr.write("N-best json splitter.  Takes in a JSON with repeated ids and splits them to separate files.\nUsage: " + sys.argv[0] + "in.json out_base_name N\nWhere N is the length of the n-best list.\n")
  sys.exit(1)
input_file = sys.argv[1]
out_file_base_name = sys.argv[2]
out_files_num = int(sys.argv[3])
files = []
for i in range(1,out_files_num+1):
    files.append(out_file_base_name+'.'+str(i))

files = [io.open(f, "w", encoding='utf-8') for f in files]

fin = io.open(input_file, 'r', encoding='utf-8', newline='\n')

decoder = json.JSONDecoder()
previous_line = None
previous_number = "-1"
index = -1
for line in fin:
  #Sample didn't have proper line endings
  line = line.strip() + "\n"
  obj = decoder.decode(line)
  number = obj["id"]
  if number != previous_number:
    if previous_line is not None:
      for backfill in range(index, len(files)):
        files[backfill].write(previous_line)
    index = 0
    previous_number = number
  if index < len(files):
    files[index].write(line)
    index += 1
    previous_line = line
if previous_line is not None:
  for backfill in range(index, len(files)):
    files[backfill].write(previous_line)
for f in files:
  f.close()
fin.close()
