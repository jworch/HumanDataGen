############################
# export_labels.py
#
# author: Cedric Cagniart
# Willow Garage 2011
############################

import bpy

scn = bpy.context.scene
mesh_obj = scn.objects.active
mesh = mesh_obj.data

vertex_groups = mesh_obj.vertex_groups



labels = [  v.groups[0].group for v in mesh.vertices ]


fout = open("mesh.labels", "w")
fout.write("%d\n" % len(mesh.vertices) )
for l in labels :
    fout.write("%d\n" %l )
fout.close() 

fout = open("labelNames.txt", "w")
for i,v in enumerate(vertex_groups) :
    fout.write("%d %s\n" % (i,v.name) )
fout.close()

print("SUCCESSFULLY exported the labels for %d vertices" % len(labels) )