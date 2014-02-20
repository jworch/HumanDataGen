import bpy
import sys
import os.path

##
# Outputs vertex group assignments of first selected mesh to file
#
# Author: Jonas Hauquier
##

# TODO: output uv coordinates for each vertex
# TODO: maybe add vertex counts to header of file

try:
    outfile = open('vertgroup_mapping.txt', 'w')
    activeO = bpy.context.selected_objects[0]
    outfile.write("# MH Mesh vertex mapping for "+activeO.data.name +"\n")
    idx = 0
    for g in activeO.vertex_groups:
        outfile.write("vertgroup "+ str(idx) +" "+ g.name +"\n")
        idx = idx +1
    outfile.write("\n")
    idx = 0
    for v in activeO.data.vertices:
        line = str(idx) + "\t"
        for g in v.groups:
            line = line + str(g.group) + " "
        outfile.write(line + "\n")
        idx = idx +1
    outfile.close()
except:
    print ('Error writing to file ' +os.path.abspath(outfile.name))
else:
    if outfile: outfile.close()

print ('Done writing to file '+ os.path.abspath(outfile.name))

