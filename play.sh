#! /bin/bash
play="play \"$1\""
cmd=""
while read line 
do
    commented='^#.*'
    if [[ ! $line =~ $commented ]]; then
        jvmOpt='.*\s-J-X.+'
        if [[ $line =~ $jvmOpt ]]; then
            line=${line//-J-X/-X}
        fi
    	cmd="$cmd$line "
    fi
done < .env
eval "$cmd$play"
