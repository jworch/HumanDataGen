#!/usr/bin/python
# -*- coding: utf-8 -*-

##
# Standalone test for PCL/People makehuman export plugin.
# Runs without makehuman for quicker testing.
#
# Author: Jonas Hauquier, Koen Buys
##


from OpenGL.GL import *
from OpenGL.GLUT import *
from OpenGL.GLU import *
from OpenGL.GL import shaders
import sys
from PIL import Image

import maketargetlib
from bodyparts import BodyParts
from colormap import colors


RENDER_HELPERS = False

# Some api in the chain is translating the keystrokes to this octal string
# so instead of saying: ESCAPE = 27, we use the following.
ESCAPE = '\033'


class OGLDraw:
    def __init__(self):
        """Constructor"""
        # Number of the glut window.
        self.window = 0
        self.imageExported = False    # Stores if the image has been saved to disk

        self.loadGeometry()
        self.loadBodypartDefs()

    def InitGL(self, Width, Height):
        """OpenGL initialization"""
        self.colors = colors

        # Set viewport color
        bgColor = self.normalize(self.colors['Background'])
        glClearColor(bgColor[0], bgColor[1], bgColor[2], 0.0)
        # Enable dept-buffer clearing
        glClearDepth(1.0)
        # Set depth test type
        glDepthFunc(GL_LESS)
        # Enable depth testing
        glEnable(GL_DEPTH_TEST)
        # Enable smooth shading
        glShadeModel(GL_SMOOTH)

        # Use shader program
        self.initShaders()
        glUseProgram(self.shader)

        self.defineCamera(Width, Height)

    def initShaders(self):
        self.shader_vp = '''
            #version 120
            // Vertex shader

            // Output parameters
            varying float lin_z;

            // Entry point
            void main() {
                gl_Position = gl_ModelViewMatrix * gl_Vertex;
                lin_z = gl_Position.z;
                gl_Position = gl_ProjectionMatrix * gl_Position;
                gl_FrontColor = gl_Color;
            }
        '''

        self.shader_fp = '''
            #version 120
            // Fragment (pixel) shader

            // Input parameters
            // also the output parameters for the vertex shader
            varying float lin_z;

            // Entry point
            void main() {
                gl_FragColor = gl_Color;
                // Store scaled depth value in alpha channel
                gl_FragColor.a = lin_z/100;
                    // Divide by far plane (TODO calculate from matrix)
            }
        '''
        self.vertex_shader = shaders.compileShader(self.shader_vp, 
                                                        GL_VERTEX_SHADER)
        self.fragment_shader = shaders.compileShader(self.shader_fp,
                                                        GL_FRAGMENT_SHADER)
        self.shader = shaders.compileProgram(self.vertex_shader,
                                             self.fragment_shader)


    def loadGeometry(self):
        print '[OGLDraw::loadGeometry] : Loading human geometry'
        obj = maketargetlib.Obj("data/base.obj")
        self.verts = obj.verts
        self.faces = obj.faces
        print '[OGLDraw::loadGeometry] : Done loading'

    def loadBodypartDefs(self):
        print '[OGLDraw::loadBodypartsDefs] : Loading human body parts'
        self.hDef = BodyParts()
        self.hDef.readVertexDefinitions()
        print '[OGLDraw::loadBodypartsDefs] : Done loading'

    def getProjMat(self, f, W, H, zNear, zFar):
        '''buildPMat(float f, int W, int H, float zNear, float zFar, float* PMat)'''
        PMat = [ [2.0*f/W, 0,       0,                            0],
                 [0,       2.0*f/H, 0,                            0],
                 [0,       0,       (zFar+zNear)/(zFar-zNear),    1],
                 [0,       0,       -(2*zFar*zNear)/(zFar-zNear), 0] ]
        # we ll have to add 1/H and 1/W to get the 1/2 pixel offet later
        return PMat

    def defineCamera(self, width, height, simple=False):
        if simple:
            # Simple test projection
            glMatrixMode(GL_PROJECTION)
            glLoadIdentity()
            gluPerspective(45.0, float(Width)/float(Height), 0.1, 100.0)
            glMatrixMode(GL_MODELVIEW)
            glLoadIdentity()
            # Move camera backwards from origin.
            glTranslatef(0.0, 0.0, -25.0)
            return

        # Kinect projection
        glMatrixMode(GL_PROJECTION)
        glLoadIdentity()
        pMat = self.getProjMat(640, width, height, 0.1, 100.0)
        glLoadMatrixf(pMat)

        glMatrixMode(GL_MODELVIEW)
        glLoadIdentity()
        # Move camera backwards from origin.
        glTranslatef(0.0, 0.0, 25.0)
        # Rotate camera 180 degrees around center (along the up vector)
        glRotatef(180.0, 0.0, 1.0, 0.0)


    def drawHuman(self):
        """
        i = 0
        while (i < len(self.verts)-3):
            self.drawQuad(self.verts[i], self.verts[i+1], self.verts[i+2], self.verts[i+3])
            i = i + 4
        """

        '''
        idx = 0
        grp = 6
        v = 1
        i = 0

        glBegin(GL_POINTS)

        # DEBUG: draw a single point
        color = self.normalize(self.colors[self.hDef.bodyParts[grp]])
        glColor3f(color[0], color[1], color[2])    # choose color
        vert = self.verts[v]
        glVertex3f(vert.x, vert.y, vert.z)
        glEnd()

        # DEBUG: draw points of one body part
        for v in self.hDef.vertices[grp]:
            print "Draw pt "+str(v)
            color = self.normalize(self.colors[self.hDef.bodyParts[grp]])
            glColor3f(color[0], color[1], color[2])    # choose color
            vert = self.verts[v-1]
            glVertex3f(vert.x, vert.y, vert.z)
            i = i+1
            if i > 100:
                break
        glEnd()
        '''

        #return

        idx = 0
        for f in self.faces:
            color = self.getColor(f[0], f[1], f[2], f[3])
            #self.drawQuad(self.verts[f[0]-1], self.verts[f[1]-1], self.verts[f[2]-1], self.verts[f[3]-1], color)
            self.drawQuad(f[0]-1, f[1]-1, f[2]-1, f[3]-1, color)
            idx = idx +1
            if not RENDER_HELPERS and idx > 13100:
                break

    def drawQuad(self, vert1Idx, vert2Idx, vert3Idx, vert4Idx, color):
        if vert1Idx not in self.hDef.groups or \
           vert2Idx not in self.hDef.groups or \
           vert3Idx not in self.hDef.groups or \
           vert4Idx not in self.hDef.groups :
            return

        #glColor3f(color[0], color[1], color[2])    # choose color
        # Draw a square (quadrilateral)
        # Start drawing a quad
        glBegin(GL_QUADS)
        color = self.normalize(self.colors[self.hDef.bodyParts[self.hDef.groups[vert1Idx][0]]])
        glColor3f(color[0], color[1], color[2])    # choose color
        vert1 = self.verts[vert1Idx]
        glVertex3f(vert1.x, vert1.y, vert1.z)

        color = self.normalize(self.colors[self.hDef.bodyParts[self.hDef.groups[vert2Idx][0]]])
        glColor3f(color[0], color[1], color[2])    # choose color
        vert2 = self.verts[vert2Idx]
        glVertex3f(vert2.x, vert2.y, vert2.z)

        color = self.normalize(self.colors[self.hDef.bodyParts[self.hDef.groups[vert3Idx][0]]])
        glColor3f(color[0], color[1], color[2])    # choose color
        vert3 = self.verts[vert3Idx]
        glVertex3f(vert3.x, vert3.y, vert3.z)

        color = self.normalize(self.colors[self.hDef.bodyParts[self.hDef.groups[vert4Idx][0]]])
        glColor3f(color[0], color[1], color[2])    # choose color
        vert4 = self.verts[vert4Idx]
        glVertex3f(vert4.x, vert4.y, vert4.z)
        glEnd()

    def normalize(self, color):
        return [float(color[0])/255, float(color[1])/255, float(color[2])/255]

    def getColor(self, vertIdx1, vertIdx2, vertIdx3, vertIdx4):
        vGroup = self.getVertGroup(vertIdx1, vertIdx2, vertIdx3, vertIdx4)
        if vGroup == -1:
            return [1.0, 1.0, 1.0]
        return self.normalize(self.colors[self.hDef.bodyParts[vGroup]])


    def getVertGroup(self, vertIdx1, vertIdx2, vertIdx3, vertIdx4):
        if vertIdx1 not in self.hDef.groups or \
           vertIdx2 not in self.hDef.groups or \
           vertIdx3 not in self.hDef.groups or \
           vertIdx4 not in self.hDef.groups :
            return -1
        '''
        return self.hDef.groups[vertIdx1][0]

        for g1 in self.hDef.groups[vertIdx1]:
            for g2 in self.hDef.groups[vertIdx2]:
                if g2 != g1:
                    continue
                for g3 in self.hDef.groups[vertIdx3]:
                    if g3 != g2:
                        continue
                    for g4 in self.hDef.groups[vertIdx4]:
                        if g4 != g3:
                            continue
                        else:
                            return g4
            return -1
        '''
        groups = dict()
        for v in [vertIdx1, vertIdx2, vertIdx3, vertIdx4]:
            for g in self.hDef.groups[v]:
                if g not in groups:
                    groups[g] = 1
                else:
                    groups[g] = groups[g] +1
        maxc = 0
        bestg = None
        for g, c in groups.items():
            if c > maxc:
                maxc = c
                bestg = g
        return bestg

    def ReSizeGLScene(self, Width, Height):
        """Called when the window is resized"""
        if Height == 0: # Prevent A Divide By Zero If The Window Is Too Small 
            Height = 1

        # Reset The Current Viewport And Perspective Transformation
        glViewport(0, 0, Width, Height)
        self.defineCamera(Width, Height)


    def DrawGLScene(self):
        """Main drawing function"""
        # Clear The Screen And The Depth Buffer
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)

        self.drawHuman()

        # Export rendered images to file
        if not self.imageExported:
            self.imageExported = True

            print "[OGLDraw::DrawGLScene] : Color plane depths: "
            print '  R '+str(glGetIntegerv( GL_RED_BITS ))+' bits'
            print '  G '+str(glGetIntegerv( GL_GREEN_BITS ))+' bits'
            print '  B '+str(glGetIntegerv( GL_BLUE_BITS ))+' bits'
            print '  A '+str(glGetIntegerv( GL_ALPHA_BITS ))+' bits'

            # Read pixels back and store in image file (using imagemagick/PIL)
            data = glReadPixels(0,0, 640, 480, GL_RGBA, GL_UNSIGNED_BYTE)
            image = Image.fromstring("RGBA", (640, 480), data, "raw", "RGBA", 0, -1)

            alphaData = image.tostring("raw", "A")
            alphaImage = Image.fromstring("L", image.size, alphaData)
            alphaImage.show()
            alphaImage.save("depth.png")

            fullOpaque = Image.new("1", (640, 480), 1)
            image.putalpha(fullOpaque)
            image.show()
            image.save('labels.png', 'PNG')

        #  since this is double buffered, swap the buffers to display what just got drawn. 
        glutSwapBuffers()


    def keyPressed(self, *args):
        """Keypress input handler"""
        # If escape is pressed, kill everything.
        if args[0] == ESCAPE:
            sys.exit()


def main():
    """Main method, entry point"""

    draw = OGLDraw()

    # Init GLUT
    glutInit(sys.argv)

    # Select type of Display mode:   
    #  Double buffer 
    #  RGBA color
    # Alpha components supported 
    # Depth buffer
    glutInitDisplayMode(GLUT_RGBA | GLUT_ALPHA | GLUT_DOUBLE | GLUT_DEPTH)

    # 640 x 480 window 
    glutInitWindowSize(640, 480)

    # the window starts at the upper left corner of the screen 
    glutInitWindowPosition(0, 0)

    # Create and store the window handle as global variable
    window = glutCreateWindow("People export standalone")

    # Pass a reference to our drawing function for the GLUT callback
    glutDisplayFunc(draw.DrawGLScene)

    # Uncomment this line to get full screen.
    #glutFullScreen()

    # When we are doing nothing, redraw the scene.
    glutIdleFunc(draw.DrawGLScene)

    # Register the function called when our window is resized.
    glutReshapeFunc(draw.ReSizeGLScene)

    # Register the function called when the keyboard is pressed.
    glutKeyboardFunc(draw.keyPressed)

    # Initialize our window.
    draw.InitGL(640, 480)

    print "[Main] : glut initialisation done"

    # Start Event Processing Engine
    glutMainLoop()

# Print message to console, and kick off the main to get it rolling.
print "[Main] : Hit ESC key to quit."
main()

