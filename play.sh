#! /bin/bash
play="play \"$1\""
cmd=""
while read line 
do
    commented='^#.*'
    if [[ ! $line =~ $commented ]]; then
    	cmd="$cmd$line "
    fi
done < .env
eval "$cmd$play"
