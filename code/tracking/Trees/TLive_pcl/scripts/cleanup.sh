#!/bin/bash

mkdir images
cd images
mkdir d
mkdir i
mkdir l
mkdir s
mkdir c
mkdir b
mkdir f
mkdir g
mkdir d2
mkdir l2
mkdir s2
mkdir c2
cd ..

echo "all dirs created"

mv d_* images/d/
mv i_* images/i/
mv l_* images/l/
mv s_* images/s/
mv c_* images/c/
mv b_* images/b/
mv f_* images/f/
mv g_* images/g/
mv d2_* images/d2/
mv l2_* images/l2/
mv s2_* images/s2/
mv c2_* images/c2/

echo "all images moved"
