#!/bin/bash

export LD_LIBRARY_PATH=/usr/local/lib:$LD_LIBRARY_PATH
echo "Export done, will now start your application in profiling mode"
echo "========================================================================="
env CPUPROFILE=/tmp/mybin.prof ./TLive_pcl -numTrees 3 -tree0 ~/data/results/forest1/tree_20.txt -tree1 ~/data/results/forest2/tree_20.txt -tree2 ~/data/results/forest3/tree_20.txt -tree3 ~/data/results/forest3/tree_20.txt -mask 1 -FG 1
echo "Application stopped, will now convert profiling data to callgrind version"
echo "========================================================================="
pprof --callgrind ./TLive_pcl /tmp/mybin.prof > /tmp/mybin.callgrind

echo "Conversion done, now use kcachegrind /tmp/mybin.callgrind"
echo "========================================================================="
#pprof --gv ./TLive_pcl /tmp/mybin.prof
#kcachegrind /tmp/mybin.callgrind 
#pprof --callgrind ./TLive_pcl -numTrees 3 -tree0 ~/data/results/forest1/tree_20.txt -tree1 ~/data/results/forest2/tree_20.txt -tree2 ~/data/results/forest3/tree_20.txt -tree3 ~/data/results/forest3/tree_20.txt -mask 1 -FG 1  /tmp/mybin.prof > /tmp/mybin.callgrind
