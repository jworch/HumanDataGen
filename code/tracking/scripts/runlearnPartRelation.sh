#!/bin/bash



[ ! -n "$ENV_TTRAIN_DATA" ] && echo "Please set ENV_TTRAIN_DATA" && exit 1;
source $ENV_TTRAIN_DATA
echo $ENV_TTRAIN_DATA



#valgrind --tool=memcheck --leak-check=yes \
#gdb --args \
/u/ccagniart/release/human_tracking/build/bin/learnPartRelations \
              -lbasename $lmapBasename \
              -dbasename $dmapBasename \
              -F 0 -L 500 \
              -o partRelation.txt


#              -F $firstFrame -L $lastFrame \

