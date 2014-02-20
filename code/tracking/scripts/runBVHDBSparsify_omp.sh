#!/bin/bash

source variables.sh

[ ! -n "$TTRAIN_ROOTDIR" ] && echo "Please set all variables, source variables.sh after correcting" && exit 1;

#roscd Datasynth
#BVHDBSparsify/bin/BVHDBSparsify \
 #                 -BVH_ref  $BVHRef \
  #                -BVH_list $BVHList \
   #               -DB       $MotionFile

rosrun BVHDBSparsify BVHDBSparsify_omp \
                  -BVH_ref  $BVHRef \
                  -BVH_list $BVHList \
                  -DB       $MotionFile
