#!/bin/bash

source variables.sh

[ ! -n "$TTRAIN_ROOTDIR" ] && echo "Please set all variables, source variables.sh after correcting" && exit 1;

rosrun BVHtoRGBD BVHtoRGBD \
              -MMat         $TTRAIN_ROOTDIR/MMat.txt \
              -BVH_ref      $BVHRef \
              -MotionFile   $MotionFile \
              -verts        $VertsFile \
              -tris         $TrisFile \
              -bones        $BonesFile\
              -labels       $LabelsFile \
              -dout         $dmapBasename \
              -lout         $lmapBasename \
              -bgdepth_mm   10000 \
              -F            $firstFrame \
              -L            $lastFrame 

#cd lmaps

#for f in `ls *.png`; do 
#	echo $f
#	/u/ccagniart/code/KINECT/scripts/colorLMap.py $f ../cmaps/$f
#done;

#cd ..
