#!/bin/bash

txtrst=$(tput sgr0) # Text reset
txtred=$(tput setaf 1) # Red
echo "${txtred}## sourcing the variables for training ##${txtrst}" 


#TRAINING_DIR_LOCAL=~/data/trainingsets/test_2012-08-27
TRAINING_DIR_LOCAL=~/data/trainingsets/test_2012-08-28
TRAINING_DIR=$TRAINING_DIR_LOCAL
#TRAINING_DIR=~/data/trainingsets/test_2012-08-13

#path for data
#needs to be replaced: TTRAIN_ROOTDIR=/home/kbuys/data/training_human_detection/train29112011
TTRAIN_ROOTDIR=$TRAINING_DIR

BVHRef=$TTRAIN_ROOTDIR/reffile/reffile.bvh
BVHList=$TTRAIN_ROOTDIR/BVHList.txt
MotionFile=$TTRAIN_ROOTDIR/db.txt

VertsFile=$TTRAIN_ROOTDIR/mesh_files/mesh.verts
TrisFile=$TTRAIN_ROOTDIR/mesh_files/mesh.tris
BonesFile=$TTRAIN_ROOTDIR/mesh_files/mesh.bones
LabelsFile=$TTRAIN_ROOTDIR/mesh_files/mesh.labels


#lmapBasename=$TTRAIN_ROOTDIR/lmaps_small/l_%06d.png
#dmapBasename=$TTRAIN_ROOTDIR/dmaps_small/d_%06d.png

lmapBasename=$TTRAIN_ROOTDIR/lmaps/l_%06d.png
dmapBasename=$TTRAIN_ROOTDIR/dmaps/d_%06d.png

#firstFrame=10000
#lastFrame=39999
firstFrame=0
#lastFrame=10058
#lastFrame=7099
lastFrame=1299

# attribLocFile and maxradius ( how far we can look for attributes)
maxRadius=50
attribLocFile=$TTRAIN_ROOTDIR/configFiles/attribLoc.txt
threshFile=$TTRAIN_ROOTDIR/configFiles/attribThresh.txt

