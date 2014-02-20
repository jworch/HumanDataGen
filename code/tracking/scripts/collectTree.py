#!/usr/bin/python

# #########################################
# importing stuff
# #########################################
# we need os to do path operations
import os
import os.path
# we need argparse to be nice
import argparse
# containers
import Queue
import numpy
from numpy import zeros, savetxt




def readSplitInfo(filename) :
	fin = open(filename)
	attribId, thresh = map(int, fin.readline().split())
	gain = float(fin.readline())
	h0 = map(long, fin.readline().split() )
	h1 = map(long ,fin.readline().split() )
	return attribId, thresh, gain, h0, h1

def readAttribLocFile( filename ) :
	fin = open(filename)
	numAttribs = int(fin.readline())
	attribsLoc = [ map(int, l.split()) for l in fin ]
	return attribsLoc

# this will compute the index in the tree array from the bit mask
def bitsToIndex( bits ) :
	d = len(bits)
	return 2**d - 1 + sum ( [ 2**(d-i-1) * bits[i] for i in xrange(d) ] )


def getMaxLabel( h ):
	if sum(h) == 0 : return 31 # the error class
	else : return numpy.argmax(h)

# ################################################
# ################################################
# parse arguments 
# ################################################
# ################################################
parser = argparse.ArgumentParser()
parser.add_argument('-baseDir', type=str)
parser.add_argument('-maxDepth', type=int)
parser.add_argument('-attribLocFile', type=str)
parser.add_argument('-o', type=str)
args =parser.parse_args()

baseDirectory = args.baseDir
maxDepth      = args.maxDepth
attribLocFile = args.attribLocFile
fileOut       = args.o



# ################################################
# ################################################
# process
# ################################################
# ################################################
# read attriblocs
attribsLoc = readAttribLocFile(attribLocFile)
# create tree and leaves
numNodes = 2**(maxDepth)-1 #cause the leaves are there these are internal only
numLeaves = 2**(maxDepth)
tree       = zeros( (numNodes, 5), dtype=int ) #this as enough elements to store the whole tree, and du1,dv1, du2, dv2, thresh per node
leaves     = zeros( (numLeaves, 1), dtype=int)
# create queue
BFSqueue = Queue.Queue()
# go go go
BFSqueue.put([]) # insert root
while not BFSqueue.empty() :
	bits = BFSqueue.get()
	# find the index 
	index = bitsToIndex(bits)
	# find the directory
	directory = baseDirectory
	for b in bits: directory = directory +"/%d" % b
	# read the split info
	filename = directory+"/splitInfo.txt"
	attribId, thresh, gain, h0, h1 = readSplitInfo(filename) # will throw if not happy
	# get the real attribute 
	attrib = attribsLoc[attribId]
	# write to the tree structure
	tree[index] = [attrib[0], attrib[1], attrib[2], attrib[3], thresh]
	# children: if node... the root has length 0... the last row of internal 
	# nodes has element of length 
	if( len(bits) < maxDepth-1 ) :
		BFSqueue.put( bits + [0] ) 
		BFSqueue.put( bits + [1] )
	# children : if leaf
	if len(bits) == maxDepth-1 :
		leaveIndex_0 = bitsToIndex( bits + [0] ) - numNodes
		leaveIndex_1 = bitsToIndex( bits + [1] ) - numNodes
		leaves[leaveIndex_0] = getMaxLabel(h0)
		leaves[leaveIndex_1] = getMaxLabel(h1)

# output
print tree
print leaves
fout = open(fileOut,'w')
fout.write("%d\n" % maxDepth)
savetxt(fout, tree, fmt="%5d")
savetxt(fout, leaves, fmt="%d" )

