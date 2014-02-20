############################
# export_rig.py
#
# author: Cedric Cagniart
# Willow Garage 2011
############################

import bpy

scn = bpy.context.scene
mesh_obj = scn.objects.active
mesh = mesh_obj.data


# Exporting the bones
vertex_groups = mesh_obj.vertex_groups
bones = [  v.groups[0].group for v in mesh.vertices ]
fout = open("mesh.bones", "w")
fout.write("%d\n" % len(mesh.vertices) )
for b in bones :
    fout.write("%s\n" % vertex_groups[b].name )
fout.close() 
print("SUCCESSFULLY exported the bones for %d vertices" % len(bones) )

# exporting the triangles
fout = open("mesh.tris", "w")
fout.write("%d\n" % len(mesh.faces) )
for f in mesh.faces:
    fout.write("%d %d %d\n" % (f.vertices[0], f.vertices[1], f.vertices[2]) )
fout.close()
print("SUCCESSFULLY exported %d triangles" % len(mesh.faces) )

# exporting the vertices
fout = open("mesh.verts", "w")
fout.write("%d\n" % len(mesh.vertices) )
for v in mesh.vertices:
    fout.write("%f %f %f\n" % (v.co[0], v.co[1], v.co[2] ) )
fout.close()
print("SUCCESSFULLY exported %d vertices" % len(mesh.vertices) )
        
#(f[0].vertices[0], f[0].vertices[1], f[0].vertices[2])

