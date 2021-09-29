#!/usr/bin/env python3
import json
import sys
import io
import os

if len(sys.argv) == 1:
    sys.stderr.write(
        "N-best json splitter.  Takes in a a directory of JSON files with repeated ids and splits them to separate files.\nUsage: " +
        sys.argv[0] + "input directory  N\nWhere N is the length of the n-best list.\n")
    sys.exit(1)

def split_files(input_file, out_files_num):
    out_file_base_name = os.path.splitext(input_file)[0]  # file path without extension
    files = [out_file_base_name + '.txt']
    for i in range(2, out_files_num + 1):
        files.append(out_file_base_name + '.' + str(i) + '.top')

    files = [io.open(f, "w", encoding='utf-8') for f in files]

    fin = io.open(input_file, 'r', encoding='utf-8', newline='\n')

    decoder = json.JSONDecoder()
    previous_line = None
    previous_number = "-1"
    index = -1
    for line in fin:
        # Sample didn't have proper line endings
        line = line.strip() + "\n"
        obj = decoder.decode(line)
        number = obj["id"]
        translation = obj["translation"].strip() + "\n"
        if number != previous_number:
            if previous_line is not None:
                for backfill in range(index, len(files)):
                    files[backfill].write(previous_line)
            index = 0
            previous_number = number
        if index < len(files):
            files[index].write(translation)
            index += 1
            previous_line = translation
    if previous_line is not None:
        for backfill in range(index, len(files)):
            files[backfill].write(previous_line)
    for f in files:
        f.close()
    fin.close()


input_dir = sys.argv[1]

# nbest is zero based. So if =4, it should return 5 soultions
out_files_num = int(sys.argv[2]) + 1

for file in os.listdir(input_dir):
    if file.endswith(".txt"):
        # EDI-NMT returns json files with .txt extension. We want to fix that part
        input_txt_file = os.path.join(input_dir, file)
        renamed_json_file = os.path.splitext(input_txt_file)[0] + '.json'
        os.rename(input_txt_file, renamed_json_file)
        split_files(renamed_json_file, out_files_num)


