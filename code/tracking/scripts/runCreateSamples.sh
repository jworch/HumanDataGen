#!/bin/bash

source variables.sh

[ ! -n "$TTRAIN_ROOTDIR" ] && echo "Please set all variables, source variables.sh after correcting" && exit 1;

echo "last frame" 
echo $lastFrame

#~/ros/git/human_tracking/Trees/createSamples/bin/createSamples \
rosrun createSamples createSamples \
              -attribLoc      $attribLocFile \
              -threshFile     $threshFile \
              -lmapBasename   $lmapBasename \
              -dmapBasename   $dmapBasename \
              -F              $firstFrame \
              -L 1298		-o /data2/training_human_detect/test0828/splitBuffs.dat
              #-L              `ls $TTRAIN_ROOTDIR/lmaps | wc -l` -o		/data2/training_human_detect/test0828/splitBuffs.dat


#-L	          $lastFrame \
	      #-o 	          /data2/training_human_detect/test_2012_08_13/splitBuffs.dat
		## a new -o parameter to test whole the process with fewer data (in that way upload and upload_sp don't take that much time)


