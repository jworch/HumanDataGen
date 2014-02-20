#!/bin/bash

source variables.sh

[ ! -n "$TTRAIN_ROOTDIR" ] && echo "Please set all variables, source variables.sh after correcting" && exit 1;

rosrun BVHDBSparsify BVHDBSparsify \
                 -BVH_ref  $BVHRef \
                  -BVH_list $BVHList \
                  -DB       $MotionFile
