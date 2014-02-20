#!/bin/bash

while read line; do
  echo $USER@$line
  ssh-copy-id $USER@$line
done < $1

