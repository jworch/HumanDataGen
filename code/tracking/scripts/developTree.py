#!/usr/bin/python

# #########################################
# importing stuff
# #########################################
#parse arguments 
import argparse
# the python subprocess module to launch executables
import subprocess
# we need os to do path operations
import os
import os.path
import glob #unix type pathname pattern expansion
import shutil
# misc
#import Queue
import collections



BINDIR="."





# #########################################
# The function wrappers
# #########################################
def run_splitAttribs( featureFile, attribBaseName, maxMem ):
	print "--> splitAttribs"
	args = ["%s/TTrain_splitAttribs" %BINDIR, 
	        "-maxMem", "%d" % maxMem,
	        "-featureFile", featureFile,
	        "-attribBaseName", attribBaseName]
#	for f in args : print f
	subprocess.check_call(args)

def run_accumHistograms( attribBaseName, thresholdFile, infoFile ):
	print "--> accumHistograms"
	args = ["%s/TTrain_accumHistograms" %BINDIR, 
	        "-attribBaseName", attribBaseName ,
	        "-thresholds", thresholdFile,
	        "-infoFile", infoFile ]
#	for f in args : print f
	subprocess.check_call(args)

def run_splitSamples( infoFile, featureFile, featureFile_0, featureFile_1):
	print "-->splitSamples"
	args = ["%s/TTrain_splitSamples" %BINDIR,
	        "-infoFile", infoFile,
	        "-featureFile", featureFile,
	        "-featureFile_0", featureFile_0,
	        "-featureFile_1", featureFile_1 ]
	subprocess.check_call(args)

# ########################################
# The node split job
# ########################################
def NodeSplit( nodeDepth, nodeDirectory, inmem_queue, outmem_queue, THRESHOLDFILE):
	dataDirectory  = nodeDirectory + "/data"
	featureFile    = dataDirectory + "/features.feat"
	attribBaseName = dataDirectory + "/attrib_%04d.attrib"
	splitInfoFile  = nodeDirectory + "/splitInfo.txt"
	child0         = nodeDirectory + "/0"
	child1         = nodeDirectory + "/1"

	if( not os.path.exists(featureFile) ):
		print "####### is already split %s" % nodeDirectory 
	# if the data is still there, run the split
	else :
		print "####### Splitting %s" % nodeDirectory 
		# run_splitAttribs    -> *.attrib files
		run_splitAttribs( featureFile, attribBaseName, 1073741824 )
		# run_accumHistograms -> *.splitInfo file
		run_accumHistograms( attribBaseName, THRESHOLDFILE, splitInfoFile )
		# remove the histogram accums
		for f in glob.glob(dataDirectory+"/*.attrib"): os.remove(f)
		# handle the children now
		os.mkdir(child0)
		os.mkdir(child1)
		# create two subdirectories in there
		os.mkdir(child0+"/data")
		os.mkdir(child1+"/data")
		# run_SplitSamples to feature files in these dirs -> *.feat file 
		run_splitSamples( splitInfoFile, featureFile, child0+"/data/features.feat", child1+"/data/features.feat")
		outmem_queue.append((nodeDepth+1, child0))
		outmem_queue.append((nodeDepth+1, child1))

	# delete our *.feat file and *.attribs
	
	for f in glob.glob(dataDirectory+"/*.feat"): os.remove(f)
	os.rmdir(dataDirectory)



# ########################################
# The Job scheduler 
# We're going to push (depth, directory) elements on these queues
# IN_MEM_QUEUES are to be processed as priority and will mount to a ramdisk
# ########################################
def developTree( baseDir, maxDepth, featureFile0, thresholdFile):

	# if we dont even have a tree.. create the root and the original sy
	if not os.path.exists(baseDir+"/data"):
		os.mkdir(baseDir+"/data")
	if not os.path.exists( baseDir+"/data/features.feat" ):
		os.symlink(featureFile0, baseDir+"/data/features.feat")

	# initialise the jobs
	OUTMEM_jobs = collections.deque()
	INMEM_jobs = collections.deque()
	OUTMEM_jobs.append( (0, baseDir ) )

	# loop until done
	while ( OUTMEM_jobs or INMEM_jobs ):
		# empty the inmem queue
		while ( INMEM_jobs ) :
			(nodeDepth, nodeDirectory) = INMEM_jobs.pop() # the inmeme queue is a stack
			if nodeDepth < maxDepth: NodeSplit( nodeDepth, nodeDirectory, INMEM_jobs, OUTMEM_jobs, thresholdFile)
		if( OUTMEM_jobs ) :
			# pop the outmem queue
			(nodeDepth, nodeDirectory) = OUTMEM_jobs.popleft() # the outmem queue is a queue 
			if nodeDepth < maxDepth: NodeSplit( nodeDepth, nodeDirectory, INMEM_jobs, OUTMEM_jobs, thresholdFile)




# ######################################################################
# ######################################################################
# ######################################################################
# Run our job
# ######################################################################
# ######################################################################
# ######################################################################


# ################################################
# ################################################
# parse arguments 
# ################################################
# ################################################
parser = argparse.ArgumentParser()
parser.add_argument('-binDir',        type=str)
parser.add_argument('-baseDir',       type=str)
parser.add_argument('-maxDepth',      type=int)
parser.add_argument('-attribLocFile', type=str)
parser.add_argument('-thresholdFile', type=str)
parser.add_argument('-featureFile0',  type=str)
args = parser.parse_args()


BINDIR=args.binDir
# clean up the tree ( for now.. hopefully we later have an alg than can be restart
shutil.rmtree(args.baseDir, ignore_errors=True)
os.mkdir(args.baseDir)
# go go go
developTree( args.baseDir, args.maxDepth,  args.featureFile0, args.thresholdFile)




#BINDIR="/wg/stor2a/ccagniart/code/bin"
#TTRAIN_ROOTDIR="/home/cedric/data/toy"
#FEATUREFILE0="/home/cedric/data/toy" + "/features.feat"
##sudo mount -t tmpfs -o size=1G tmpfs ramdisk/
##sudo mount -t ramfs -o size=30g ramfs /mnt/ram
#RAMDISK="/home/cedric/ramdisk"

## tree development dir
#TTRAIN_DEVDIR=TTRAIN_ROOTDIR+"/tree"
### attribLocFile and maxradius ( how far we can look for attributes)
##maxRadius=50
##attribLocFile=TTRAIN_ROOTDIR + "/attribLoc.txt"
## thresholdFile
#THRESHOLDFILE=TTRAIN_ROOTDIR + "/attribThresh.txt"
