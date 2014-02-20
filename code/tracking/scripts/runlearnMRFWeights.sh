#!/bin/bash



[ ! -n "$ENV_TTRAIN_DATA" ] && echo "Please set ENV_TTRAIN_DATA" && exit 1;
source $ENV_TTRAIN_DATA
echo $ENV_TTRAIN_DATA



#valgrind --tool=memcheck --leak-check=yes \
#gdb --args \
/u/ccagniart/release/human_tracking/build/bin/learnMRFWeights \
              -lmapBasename $lmapBasename \
              -dmapBasename $dmapBasename \
              -F 0 -L 0 \
              -o bla.txt


#              -F $firstFrame -L $lastFrame \

