# *** DO WHATEVER THE FUCK YOU WANT LICENSE ***
# Original website: http://nukengine.com/blender-addons/
# Modified by Jonas Hauquier to copy vertex group and material assignments 

bl_info = {
    "name": "Copy vertex order by UVs",
    "author": "fxbar/f00bar",
    "version": (0, 3),
    "blender": (2, 6,0),
    "api": 40900,
    "location": "Object > Copy vertex order by UVs",
    "description": "Copy vertex order by UVs",
    "warning": "",
    "wiki_url": "nukengine.com",
    "tracker_url": "",
    "category": "Mesh"}

import bpy
import math
import time
from operator import itemgetter


#globals:
EPSILON = 0.000001
#EPSILON = 0.0001
#EPSILON = 0.001
isBMesh = False


#handle blender 2.62 and bmesh transparently
def faces(mesh):
    if isBMesh:
        return mesh.polygons
    else:
        return mesh.faces

def uvs(mesh, f, F):
    if isBMesh:
        uv_loops = mesh.uv_loop_layers.active.data
        lstart = lend = F.loop_start
        lend += F.loop_total
        return [uv.uv for uv in uv_loops[lstart:lend]]
    else:
        return mesh.uv_textures.active.data[f].uv

def buildVertToFaceMap(mesh):
    VertexFaces = {}
    for fid, F in enumerate(faces(mesh)):
        for v in F.vertices:
            if not v in VertexFaces :
                VertexFaces[v] = []
            VertexFaces[v].append(fid)
    return VertexFaces

def buildDegreeOccuranceHeap(mesh, VertexFaces):
    degreeMap = {}
    for idx, f in VertexFaces.items():
        degree = len(f)
        if not degree in degreeMap:
            degreeMap[degree] = []
        degreeMap[degree].append(idx)
    occursHeap = []
    for degree, vList in degreeMap.items():
        occursHeap.append((len(vList), degree, vList))
    occursHeap = sorted(occursHeap, key=itemgetter(0,1))
    return occursHeap

    
    
def findMatchingVertsByUv(mesh1, v1, f1, mesh2, v2, f2, uvcache1, uvcache2):
    res = {}
    F1 = faces(mesh1)[f1]
    F2 = faces(mesh2)[f2]
    
    if len(F1.vertices) != len(F2.vertices):
        return {}
    
    if f1 in uvcache1:
        uvs1 = uvcache1[f1]
    else:
        uvs1 = uvs(mesh1, f1, F1)
        uvcache1[f1] = uvs1
        
    if f2 in uvcache2:
        uvs2 = uvcache2[f2]
    else:
        uvs2 = uvs(mesh2, f2, F2)
        uvcache2[f2] = uvs2
    
    if len(uvs1) != len(uvs2):
        return {}
    
    vidx1 = -1
    for idx1, vi1 in enumerate(F1.vertices):
        if v1 == vi1:
            vidx1 = idx1
            break
    vidx2 = -1
    for idx2, vi2 in enumerate(F2.vertices):
        if v2 == vi2:
            vidx2 = idx2
            break
    
    numVerts = len(F1.vertices)
    ok = True
    #print("DEBUG START************************** v: %i vs %i, f: %i vs %i" % (v1,v2, f1, f2))
    for i in range(numVerts):
        newIdx1 = (i + vidx1) % numVerts
        newIdx2 = (i + vidx2) % numVerts
        if abs(uvs1[newIdx1][0] - uvs2[newIdx2][0]) > EPSILON or abs(uvs1[newIdx1][1] - uvs2[newIdx2][1]) > EPSILON:
            ok = False
            #print("DEBUG: no match (newIdx1=%i, newIdx2=%i), conflict [%f, %f] vs. [%f, %f]" %(newIdx1, newIdx2, uvs1[newIdx1][0], uvs1[newIdx1][1], uvs2[newIdx2][0], uvs2[newIdx2][1]))
            # uvs = []
            # for x in mesh2.uv_textures.active.data[f2].uv:
                # uvs.append([x[0], x[1]])
            # print("DEBUG uvs of failed match %i: %s" % (f2, str(uvs)))
            break
        else:
            res[F1.vertices[newIdx1]] = F2.vertices[newIdx2]
            #print("DEBUG: match %i, %i with uvs %f, %f to %f, %f" % (F1.vertices[newIdx1], F2.vertices[newIdx2], uvs1[newIdx1][0], uvs1[newIdx1][1], uvs2[newIdx2][0], uvs2[newIdx2][1]))
    
    #print("DEBUG END  ************************** v1=%i v2=%i : %s" % (v1,v2, str(ok)))    
    if (ok):
        return res
    else:
        return {}
    
    
    
def mapByUv(mesh1, mesh2, vList1, vList2, VertexFaces1, VertexFaces2, uvcache1, uvcache2, mapping, invmapping, newMaps):
        refound = []
        for v1 in vList1:
                for f1 in VertexFaces1[v1]:
                    match = False
                    for v2 in vList2:
                        for f2 in VertexFaces2[v2]:
                            #if mesh1.vertics[v1]. not v1 in mapping
                            submatch = findMatchingVertsByUv(mesh1, v1, f1, mesh2, v2, f2, uvcache1, uvcache2)
                            if submatch:
                                #print("found submatch v1=%i v2=%i: map=%s" % (v1, v2, str(submatch)))
                                #mapping[v1] = v2
                                for v1x, v2x in submatch.items():
                                    if v1x in mapping:
                                        if mapping[v1x] != v2x:
                                            print("ERROR: found different mapping for vertex")
                                            print("original mapping %i,%i, new mapping %i,%i" % (v1x, mapping[v1x], v1x, v2x))
                                            raise Exception("ERROR: found different mapping for vertex")
                                        else:
                                            #print("DEBUG: refound mapping: v: %i,%i,  f: %i,%i" % (v1x, v2x, f1, f2))
                                            #FIXME: check: tricky bug if missing???
                                            #newMaps.append(v1x, v2x, f1, f2)
                                            refound.append((v1x, v2x, f1, f2))
                                    else:
                                        mapping[v1x] = v2x
                                        invmapping[v2x] = v1x
                                        newMaps.append((v1x, v2x, f1, f2))
                                        #print("DEBUG: found mapping: v: %i,%i,  f: %i,%i" % (v1x, v2x, f1, f2))
                                match = True
                    if not match:
                        print("ERROR: no match for face found:", f1)
                        uvsFail = []
                        for x in uvs(mesh1, f1, faces(mesh1)[f1]):
                            uvsFail.append([x[0], x[1]])
                        print("UVs mesh1 of face %i: %s" % (f1, str(uvsFail)))
                        for v2 in vList2:
                            for f2 in VertexFaces2[v2]:
                                uvsFail = []
                                for x in uvs(mesh2, f2, faces(mesh2)[f2]):
                                    uvsFail.append([x[0], x[1]])
                                print("UVs mesh2 of face %i: %s" % (f2, str(uvsFail)))
                        raise Exception("ERROR: no match for face found:", f1)
        #try to reduce data size
        #all faces found if here
        for vnew1, vnew2, f1, f2 in newMaps:
            #fixme: fix code that method is not called in this case
            if f1 in VertexFaces1[vnew1]:
                VertexFaces1[vnew1].remove(f1)
                VertexFaces2[vnew2].remove(f2)
                
        for vnew1, vnew2, f1, f2 in refound:
            #fixme: fix code that method is not called in this case
            if f1 in VertexFaces1[vnew1]:
                VertexFaces1[vnew1].remove(f1)
                VertexFaces2[vnew2].remove(f2)
                

#Algorithm:
#build min-heap of vertex lists by number of occurance of a certain vertex degree in the mesh (degree as number of faces containing a vertex)
#first step of the loop: map verts, candidate set is all unmapped verts with degree X [ aka map(pop(minheap)) ]
#second step of loop: loop: expand mappings found in step one: candidate set is all unmapped verts of all unmapped faces of a vertex that was mapped in step one or two.
def object_copy_indices (self, context):
    startTime = time.time()
    #create a copy of mesh1 (active), but with vertex order of mesh2 (selected)
    obj1 = bpy.context.active_object
    selected_objs = bpy.context.selected_objects[:]
    
    
    if not obj1 or len(selected_objs) != 2 or obj1.type != "MESH":
        raise Exception("Exactly two meshes must be selected. This Addon copies vertex order from mesh1 to copy of mesh2")
    
    selected_objs.remove(obj1)
    obj2 = selected_objs[0]
    
    if obj2.type != "MESH":
        raise Exception("Exactly two meshes must be selected. This Addon copies vertex order from mesh1 to copy of mesh2")
    
    
    mesh1 = obj1.data
    mesh2 = obj2.data
    
    #ugly block, but fast to implement
    global isBMesh
    try:
        face = mesh1.polygons
        print("is BMesh")
        isBMesh = True
    except:
        print("is not BMesh")
        face = mesh1.faces
        isBMesh = False
    if isBMesh:
        # be sure that both are bmesh, otherwise crash (should not be possible or I understand something wrong)
        face = mesh2.polygons
    
    if not mesh1.uv_textures or len(mesh1.uv_textures) == 0 or not mesh2.uv_textures or len(mesh2.uv_textures) == 0:
        raise Exception("Both meshes must have a uv mapping. This operator even assumes matching uv mapping!")
    if len(mesh1.vertices) != len(mesh2.vertices):
        raise Exception("Both meshes must have the same number of vertices. But it is %i:%i" % (len(mesh1.vertices), len(mesh2.vertices)))
    
    #FIXME: faces seem invalid later, or is there another bug? so we use face indices for now and look them up on use
    VertexFaces1 = buildVertToFaceMap(mesh1)
    VertexFaces2 = buildVertToFaceMap(mesh2)
    degreeHeap1 = buildDegreeOccuranceHeap(mesh1, VertexFaces1)
    degreeHeap2 = buildDegreeOccuranceHeap(mesh2, VertexFaces2)

    uvcache1 = {}
    uvcache2 = {}
    
    mapping = {}
    invmapping = {}
    passes = 0
    
    print("Trying to find initial mapping of all vertices with that degree (num faces) that occurs the fewest in the mesh")
    while len(mapping) < len(mesh1.vertices) and len(degreeHeap1) > 0:
        num1, degree1, vList1 = degreeHeap1.pop(0)
        num2, degree2, vList2 = degreeHeap2.pop(0)
        newMaps = []
        if num1 == num2 and degree1 == degree2 and len(vList1) == len(vList2):
            print("DEBUG: Looking at %i verts with degree %i" % (len(vList1), degree1))
            #remove all known from vlists (TODO: optimize)
            tmpList = []
            for vxx in vList1:
                if not vxx in mapping:
                    tmpList.append(vxx)
            vList1 = tmpList
            tmpList = []
            for vxx in vList2:
                if not vxx in invmapping:
                    tmpList.append(vxx)
            vList2 = tmpList
            
            print("DEBUG: relevant of those %i in mesh1 and %i in mesh2" % (len(vList1), len(vList2)))
            #first step of the loop: map verts, candidate set is all verts with degree X (degree as number of faces containing a vertex)
            mapByUv(mesh1, mesh2, vList1, vList2, VertexFaces1, VertexFaces2, uvcache1, uvcache2, mapping, invmapping, newMaps)
            passes += 1
            #expand over all neighbours of newly known vertex mappings
            #second step of loop: loop: expand mappings found in step one (or in this step)
            while len(newMaps) > 0:
                #print("DEBUG: handling newMaps: %s" % str(newMaps))
                newerMaps = []
                for vnew1, vnew2, f1, f2 in newMaps:
                    newFs1 = VertexFaces1[vnew1]
                    newFs2 = VertexFaces2[vnew2]
                    if newFs1 and newFs2:
                        vList1 = []
                        vList2 = []
                        for fx1 in newFs1:
                            for vx1 in faces(mesh1)[fx1].vertices:
                                if not vx1 in mapping:
                                    vList1.append(vx1)
                        for fx2 in newFs2:
                            for vx2 in faces(mesh2)[fx2].vertices:
                                if not vx2 in invmapping:
                                    vList2.append(vx2)
                        if vList1 and vList2:
                            tmpMap = []
                            #print("DEBUG: calling mapByUv to extend known mappings")
                            #candidate set is all verts of all faces (without already mapped faces) of a vertex that was mapped in step one or two
                            mapByUv(mesh1, mesh2, vList1, vList2, VertexFaces1, VertexFaces2, uvcache1, uvcache2, mapping, invmapping, tmpMap)
                            newerMaps = newerMaps + tmpMap
                            passes += 1
                            if passes % 500 == 0:
                                print("after %i extension runs of mapByUv (%s seconds) we have %i mappings." % (passes, str(time.time()-startTime),len(mapping)))
                                print("current newMaps size:", len(newMaps))
                newMaps = newerMaps
        else:
            print("ERROR: the meshes have a different topology.")
            raise Exception("ERROR: the meshes have a different topology.")
        print("DEBUG: ran %i executions of mapByUv to extend mapping" % passes)
        print("DEBUG: mappingsize=%i, verts=%i" % (len(mapping), len(mesh1.vertices)))
        if len(mapping) < 50:
            print("Mapping so far: %s" % (str(mapping)))

    if len(mapping) == len(mesh1.vertices):
        verts_pos=[]
        faces_indices=[]
        verts_indices=[]
        print("Found complete mapping")
        for v in mesh2.vertices:
            verts_pos.append(mesh1.vertices[invmapping[v.index]].co)
        
        for f in faces(mesh2):
            vs=[]
            for v in f.vertices:
                vs.append(v)
            faces_indices.append(vs)
        
        #create new mesh
        me=bpy.data.meshes.new("%s_v_order_%s" % (mesh1.name, mesh2.name))
        ob=bpy.data.objects.new("%s_v_order_%s" % (obj1.name, obj2.name) ,me)           
                 
        me.from_pydata(verts_pos, [], faces_indices)
        
        ob.matrix_world = obj1.matrix_world
        
        bpy.context.scene.objects.link(ob)
	
        me.update()

        transferVertexGroups(obj1, ob, invmapping)
        transferMaterials(obj1, ob, invmapping)
        print("New Object created. object=%s, mesh=%s in %s seconds" % (ob.name, me.name, str(time.time()-startTime)))
    else:
        print("ERROR: Process failed, did not find a mapping for all vertices")
        raise Exception("ERROR: Process failed, did not find a mapping for all vertices")


def transferVertexGroups(sourceObj, destObj, invVertMapping):
    # Create vertex groups
    for g in sourceObj.vertex_groups:
        destObj.vertex_groups.new(g.name)
    
    # Assign vertex groups to vertices
    sourceMesh = sourceObj.data
    destMesh   = destObj.data
    for v in destMesh.vertices:
        groups = sourceMesh.vertices[invVertMapping[v.index]].groups
        for group in groups:
            destObj.vertex_groups[group.group].add( [v.index], 1.0, 'REPLACE' )
    

def transferMaterials(sourceObj, destObj, invVertMapping):
    # Copy materials
    for mat in sourceObj.data.materials:
        #m = mat.copy()
        destObj.data.materials.append(mat)
    # Transfer material assignments based on vertex groups
    for face in destObj.data.faces:
        group = getGroup(face, destObj.data)
        if group != -1:
            name = destObj.vertex_groups[group].name
            matIdx = destObj.data.materials.find(name)
            if matIdx != -1:
                face.material_index = matIdx
                

def getGroup(face, mesh):
    groups = dict() # stores occurence counts for all groups of face
    for v in face.vertices:
        vert = mesh.vertices[v]
        for g in vert.groups:
            if g.group in groups:
                groups[g.group] = groups[g.group] + 1
            else:
                groups[g.group] = 1
    if len(groups) == 0:
        return -1
    for g, cnt in groups.items():
        if cnt == 4:
            return g  # We assume faces are quads
    # If that doesn't work:
    maxCnt = 0
    bestG = None
    for g, cnt in groups.items():
        if cnt > maxCnt:
            maxCnt = cnt
            bestG = g
    return bestG    # Return group with most vertex assignments


class NukeCopyIndices (bpy.types.Operator):
    bl_idname = "objects.copy_vertex_order_by_uvs"
    bl_label = "Copy vertex order by UVs"
    bl_description = "Copy vertex order from mesh1 to copy of mesh2"
    bl_options = {'REGISTER', 'UNDO'}

    def execute(self, context):
        object_copy_indices(self, context)
        return {'FINISHED'}



def add_copyindices_button(self, context):
    self.layout.operator(
        NukeCopyIndices.bl_idname,
        text="Copy vertex order by UVs",
        icon="PLUGIN")


def register():
    bpy.utils.register_class(NukeCopyIndices)
    bpy.types.VIEW3D_MT_object.append(add_copyindices_button)


def unregister():
    bpy.utils.unregister_class(NukeCopyIndices)
    bpy.types.VIEW3D_MT_object.remove(add_copyindices_button)


if __name__ == '__main__':
    register()

