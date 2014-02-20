#!/usr/bin/python

import numpy
from numpy import array
import sys
from math import sqrt

filename = sys.argv[1]

def parse( l ):
	strings = l.split()
	return strings[0], array( map(float,strings[1:]) , dtype=numpy.uint64)

# load file
fin = open(filename) 
matrices = array( [ parse(l)[1] for l in fin ] )
fin.close()


# get the number of classes
numClasses = sqrt( matrices[0].shape[0] )
assert matrices[0].shape[0] == numClasses*numClasses

# average them
matrix = numpy.sum(matrices, axis=0)
matrix.shape = (numClasses, numClasses)


matfloat = array(matrix, dtype=numpy.double)
for row in xrange(numClasses) :
	nSamples = sum(matrix[row,:])
	if not nSamples == 0 : matfloat[row,:] = matfloat[row,:] / nSamples

print numpy.diag(matfloat)
numpy.savetxt("confmat.txt",matfloat,  "%5.5f")
