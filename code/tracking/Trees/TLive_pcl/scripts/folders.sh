#!/bin/bash

for file in $1/*
do
  #echo $file
  if [[ $file == *.bag ]]
  then
    basename=${file%.bag}
    mkdir $basename
    mv $file $basename
    echo $basename
  fi
done;
