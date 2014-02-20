#!/usr/bin/python

# #########################################
# importing stuff
# #########################################
# the python subprocess module to launch executables

from sys import argv
import struct 
from numpy import array
fname = argv[1]

fin = open( fname, 'rb')
labelstring = fin.read(1) # 1 uint8 + 2000 uint16t
paddingstring = fin.read(1)
attribsstring = fin.read(2)
label = struct.unpack('B', labelstring)
feature = struct.unpack('h', attribsstring)
print label,feature
