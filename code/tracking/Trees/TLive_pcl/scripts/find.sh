#!/bin/bash

for bag in $(find $1 -name '*.bag')
do
  echo $bag;
  basename=${bag%.bag}
  #echo $basename;
  dir=$(dirname $bag)
  echo $dir;

  #pushd $dir;

  ############ PROCESS #####################
  rosrun TLive_pcl bagprocessor bin/bagprocessor -numTrees 3 -tree0 ~/Data/results/forest1/tree_20.txt -tree1 ~/Data/results/forest2/tree_20.txt -tree2 ~/Data/results/forest3/tree_20.txt -tree3 ~/Data/results/forest3/tree_20.txt -mask 0 -FG 0 -topic /camera/rgb/points -bag $bag 2> output.csv;

  ############ CLEANUP #####################
  mkdir $dir/img
  #cd img
  mkdir $dir/img/d
  mkdir $dir/img/i
  mkdir $dir/img/l
  mkdir $dir/img/s
  mkdir $dir/img/c
  mkdir $dir/img/b
  mkdir $dir/img/f
  mkdir $dir/img/g
  mkdir $dir/img/d2
  mkdir $dir/img/l2
  mkdir $dir/img/s2
  mkdir $dir/img/c2
  #go back to $dir
  #cd ..
  echo "all dirs created"
  mv d_*  $dir/img/d/
  mv i_*  $dir/img/i/
  mv l_*  $dir/img/l/
  mv s_*  $dir/img/s/
  mv c_*  $dir/img/c/
  mv b_*  $dir/img/b/
  mv f_*  $dir/img/f/
  mv g_*  $dir/img/g/
  mv d2_* $dir/img/d2/
  mv l2_* $dir/img/l2/
  mv s2_* $dir/img/s2/
  mv c2_* $dir/img/c2/
  echo "all images moved"

  mv output.csv $dir/
  ############ VIDEO #####################
  #popd $dir;
done
