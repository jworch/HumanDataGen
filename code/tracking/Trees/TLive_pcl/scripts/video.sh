#!/bin/bash

mkdir video
cd images
cd d
ffmpeg -qscale 5 -r 20 -b 9600 -i d_%d.png ../../video/d.mp4
cd ..
cd i
ffmpeg -qscale 5 -r 20 -b 9600 -i i_%d.png ../../video/i.mp4
cd ..
cd l
ffmpeg -qscale 5 -r 20 -b 9600 -i l_%d.png ../../video/l.mp4
cd ..
cd s
ffmpeg -qscale 5 -r 20 -b 9600 -i s_%d.png ../../video/s.mp4
cd ..
cd c
ffmpeg -qscale 5 -r 20 -b 9600 -i c_%d.png ../../video/c.mp4
cd ..
cd b
ffmpeg -qscale 5 -r 20 -b 9600 -i b_%d.png ../../video/b.mp4
cd ..
cd f
ffmpeg -qscale 5 -r 20 -b 9600 -i f_%d.png ../../video/f.mp4
cd ..
cd g
ffmpeg -qscale 5 -r 20 -b 9600 -i g_%d.png ../../video/g.mp4
cd ..
cd d2
ffmpeg -qscale 5 -r 20 -b 9600 -i d2_%d.png ../../video/d2.mp4
cd ..
cd l2
ffmpeg -qscale 5 -r 20 -b 9600 -i l2_%d.png ../../video/l2.mp4
cd ..
cd s2
ffmpeg -qscale 5 -r 20 -b 9600 -i s2_%d.png ../../video/s2.mp4
cd ..
cd c2
ffmpeg -qscale 5 -r 20 -b 9600 -i c2_%d.png ../../video/c2.mp4
cd ..
cd ..

echo "all videos created"

