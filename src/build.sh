#!/bin/bash

antlr4-build IML

cd IIML
antlr4-build -python

errorcode=$?
if [ $errorcode -ne 0 ]; then
    exit $errorcode
fi

cd ../


