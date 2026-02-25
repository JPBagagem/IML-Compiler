#!/bin/bash

main="$1"

if [ -z "$main" ]; then
    echo "File to compile not specified"
    exit 1
fi

if [ ! -f $main ]; then
    echo "File to compile not specified"
    exit 1
fi

home=$(dirname "$main")
file=$(basename "$main")

cd $home

python3 $file

