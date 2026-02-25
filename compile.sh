#!/bin/bash


usage() {
    echo "Usage: $0 [-a auxilary] [-d destiny_path] [-m main_file] file"
    echo "  -h: show this help"
    echo "  -a auxilary: add auxilary files to the list [you can use this option multiple times]"
    echo "  -d destiny_path: specify the destiny path"
    echo "  -m main_file: specify the main output file name"
    exit 1
}

destiny_path=""
mainoutput="run.py"
# auxiliary is a list of files
auxiliary=""

while getopts ":a:d:m:h" opt; do
    case $opt in
        a)
            ## add auxilary files to the list
            auxiliary="$auxilary ${OPTARG}"
            ;;

        d)
            destiny_path=$OPTARG
            ;;
        m)
            mainoutput=$OPTARG
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

main="$1"

if [ -z "$destiny_path" ]; then
    destiny_path=$(dirname "$main")
fi


if [ -z "$main" ]; then
    echo "File to compile not specified"
    exit 1
fi

if [ ! -f $main ]; then
    echo "File to compile not specified"
    exit 1
fi

mkdir -p $destiny_path

# Copy auxiliary files
for file in $auxiliary; do
    if [ -f "$file" ]; then
        cp "$file" "$destiny_path"
    elif [ -d "$file" ]; then
        cp -r "$file" "$destiny_path"
    else
        echo "Warning: auxiliary file '$file' not found, skipping."
    fi
done

antlr4-run $main > "$destiny_path/$mainoutput"

if [ $? -ne 0 ]; then
    [ -f "$file" ] && rm "$file"
    exit 1
fi

echo "$destiny_path/$mainoutput"

cp ./IIML/*.py $destiny_path

