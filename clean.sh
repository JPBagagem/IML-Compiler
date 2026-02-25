#!/bin/bash


usage() {
    echo "Usage: $0 [-p]"
    echo "  -h: show this help"
    echo "  -p: also cleans Python files"
    echo "  -d: specifies the output directory to clean"
    exit 1
}

pathToClean="../examples"
removeCompiled=0

while getopts ":pd:h" opt; do
    case $opt in
        d)
            pathToClean=$OPTARG
            ;;
        p)
            removeCompiled=1
            ;;
        h)
            usage
            ;;
        \?)
            echo "Invalid option: -$OPTARG"
            usage
            ;;
        :)
            echo "Option -$OPTARG requires an argument."
            usage
            ;;
    esac
done 

shift $((OPTIND -1))


antlr4-clean
antlr4-clean -python

if [ ! -d $pathToClean ]; then
    echo "Couldnt found output directory"
    exit 1
fi

cd $pathToClean
antlr4-clean -python

if [ $removeCompiled -eq 1 ]; then
    rm *.py
fi

