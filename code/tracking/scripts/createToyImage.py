#!/usr/bin/python

from array import array
import cv
from cv import LoadImage, SaveImage, LUT, CreateImage, CreateImageHeader

import numpy

LMap = numpy.ones((640,480), dtype=numpy.uint8)*31
DMap = numpy.ones((640,480), dtype=numpy.uint16)*0xFFFF

# write a big square
for y in xrange( 160, 260) :
	for x in xrange( 270, 370 ) :
		DMap[y,x] = 1000
# write funny labels 
	# label 0
for y in xrange(160,210) : 
	for x in xrange (270,320) :
		LMap[y,x] = 0
for y in xrange(210,260) : 
	for x in xrange (320,370) : 
		LMap[y,x] = 0
	# label 1
for y in xrange(160,210) : 
	for x in xrange (320,370) : 
		LMap[y,x] = 1
for y in xrange(210,260) : 
	for x in xrange (270,320) : 
		LMap[y,x] = 1

#LMap = array('B') #unsigned char
#DMap = array('H') #unsigned short

lout = CreateImageHeader((640,480),8, 1)
cv.SetData(lout, LMap.tostring('F'))

dout = CreateImageHeader((640,480),16, 1)
cv.SetData(dout, DMap.tostring('F'))

SaveImage("lmap.png", lout)
SaveImage("dmap.png", dout)


