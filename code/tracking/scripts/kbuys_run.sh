#!/bin/bash

#source ~/ros/git/human_tracking/scripts/variables.sh
source variables.sh	


[ ! -n "$TTRAIN_ROOTDIR" ] && echo "Please set all variables, source
variables.sh after correcting" && exit 1;


suffix=$1

splitBuffFile=$TTRAIN_ROOTDIR/splitbuffs.dat
#splitBuffFile=/data2/training_human_detect/test_2012_08_13/splitbuffs.dat

outDir=$TTRAIN_ROOTDIR/tree_files
featSeqFile=featFiles/features_$suffix.seq
rootDir=Tree_$suffix

#### changing to splitFile							OK
#bin/hadoop jar $jarfile HadoopTree.apps.Driver \
#ConvertFeatToSplit $featSeqFile $rootDir

#bin/hadoop fs -rmr $rootDir/0
#bin/hadoop fs -rmr $rootDir/1
#bin/hadoop fs -rmr $rootDir/splitInfo.seq

# upload the file to the DFS							OK
#echo "${txtred}##uploading the splitBuffs file##${txtrst}"
#$HADOOP_COMMON_HOME/bin/hadoop jar $jarfile HadoopTree.apps.Driver \
#upload_sp $splitBuffFile $rootDir/splitbuffs.seq

# upload the attribThresh file							OK
#echo "${txtred}uploading the attribThresH file${txtrst}"
#$HADOOP_COMMON_HOME/bin/hadoop fs -put
$TTRAIN_ROOTDIR/configFiles/attribThresh.txt attribThresh.txt

#training the tree
echo "${txtred}## training the tree ##${txtrst}"
$HADOOP_COMMON_HOME/bin/hadoop jar $jarfile HadoopTree.apps.Driver \
trainTree $rootDir 20 $outDir $attribLocFile

#bin/hadoop jar $jarfile HadoopTree.apps.Driver \
#countLabels $rootDir $rootDir/count

#copying the file
#bin/hadoop jar $jarfile HadoopTree.apps.Driver \
#upload $featFile $featSeqFile

#bin/hadoop jar $jarfile HadoopTree.apps.Driver \
#upload /home/cedric/data/db_$suffix/features.feat $rootDir/features.seq

#bin/hadoop fs -put /home/cedric/data/db1/attribThresh.txt attribThresh.txt
#bin/hadoop jar $jarfile HadoopTree.apps.Driver \
#findBeistSplit $rootDir

#bin/hadoop jar $jarfile HadoopTree.apps.Driver \
#splitSamples 1496 -229 $rootDir

#bin/hadoop jar $jarfile HadoopTree.apps.Driver \
#countLabels $rootDir/0/splitbuffs.seq $rootDir/0/count

#bin/hadoop jar $jarfile HadoopTree.apps.Driver \
#countLabels $rootDir/1/splitbuffs.seq $rootDir/1/count

#bin/hadoop fs -rmr results
#bin/hadoop fs -rm Hadoop_countLabels
#bin/hadoop fs -put /u/ccagniart/code/bin/Hadoop_countLabels Hadoop_countLabels
#bin/hadoop pipes -conf
/u/ccagniart/code/KINECT/Trees/Hadoop_countLabels/config.xml \
#-libjars $jarfile \
#-input Tree/features.seq \
#-output results

# executing code
#bin/hadoop jar /u/ccagniart/code/hadooptest/testIO/dist/testIO.jar \
#train Tree/
