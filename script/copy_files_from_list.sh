#!/bin/bash
if [[ !($# = 3 && -f "$1") ]]; then
	echo "Usage:"
	echo "copy_files_from_list.sh {/path/to/filenames_list} {from_path} {to_path}"
	exit
else
	filenames_list=$1
	from_path=$2
	to_path=$3
	cat "$filenames_list" | while read file_name; do
		cp "$from_path/$file_name" "$to_path/$file_name"
	done
fi
