import os.path

class BodyParts:
    def readVertexDefinitions(self):
        self.bodyParts = dict()    # List of all body part groups
        self.vertices = dict()     # Dict per vertgroup index, all vertex indices
        self.groups = dict()

        mappingFile = "../data/people_export/vertgroup_mapping.txt"
        print "[BodyParts::readVertexDefinitions] : opening "+mappingFile
        infile = open(mappingFile, "r")
        lineCnt = 0
        for line in infile:
            lineCnt = lineCnt +1
            line = line.strip()
            # Ignore comments and empty lines
            if(not line or line.startswith("#")):
                continue
            # Define bodypart vertex group
            if(line.startswith("vertgroup")):
                items = line.split()
                try:
                    gIdx = int(items[1])
                    gName = items[2]
                    self.bodyParts[gIdx] = gName
                    continue
                except:
                    print "[BodyParts::readVertexDefinitions] : Warning: error at line "+str(lineCnt)+" of file "+ os.path.abspath(infile.name)+"!"
                    continue
            # Parse vertex - vertgroups assignment
            try:
                items = line.split()
                vertIdx = int(items[0])
                if(len(items) == 1):
                    # print "[BodyParts::readVertexDefinitions] : Warning: vertex "+str(vertIdx)+" at line "+str(lineCnt)+" of file "+ os.path.abspath(infile.name)+" is not assigned to any vertex group!"
                    continue
                self.groups[vertIdx] = list()
                # Assign vertex groups
                for i in range(1,len(items)):
                    vGroupIdx = int(items[i])
                    if(vGroupIdx in self.vertices):
                        vList = self.vertices[vGroupIdx]
                    else:
                        vList = list()
                        self.vertices[int(vGroupIdx)] = vList
                    #print "Adding "+str(vertIdx)+" to group "+str(vGroupIdx)
                    vList.append(vertIdx)
                    self.groups[vertIdx].append(vGroupIdx)
            except:
                print "[BodyParts::readVertexDefinitions] : Warning: Parsing error at line "+str(lineCnt)+" of file "+ os.path.abspath(infile.name)+"!"

'''
i = humandef()
i.readVertexDefinitions()

total = 0
for c, b in i.vertices.items():
    print str(len(b))
    total = total + len(b)
print " "
print str(total)
print " "
for c, b in i.vertices.items():
    print str(c)
    print b
    print "\n"


for vgIdx, vectors in i.vertices.items():
            print str(vgIdx)+" - "+i.bodyParts[vgIdx]+" ("+str(len(vectors))+" vertices): "
            total= total + len(vectors)
            print vectors
            print "\n"
print 'Total: '+str(total)+" vertices"
'''

