#!/bin/bash

source variables.sh

[ ! -n "$TTRAIN_ROOTDIR" ] && echo "Please set all variables, source variables.sh after correcting" && exit 1;

pushd $lmap

declare -i i
i=1

#WARNING STILL TO DO: AVOID WABBIT BEHAVIOUR ON HUGE DATASETS
for f in `ls *.png`; do
  ((i++)) 
  #echo $f
  ~/ros/git/human_tracking/scripts/colorLMap.py $f ../cmaps/$f &
  if [ $i -eq $1 ]; then 
    i=1
    echo 'wait'
    wait
  fi
done;
wait
popd
