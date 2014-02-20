/* *************************************************
 *
 * Copyright (2011) Willow Garage
 *
 * Author : Cedric Cagniart 
 * ************************************************* */

#ifndef RGBDRENDERER_H_DEFINED
#define RGBDRENDERER_H_DEFINED


#include "OpenGLContext.h"
#include <stdint.h>
#include <GL/gl.h>

class RGBDRenderer
{
	public :

	struct LabeledVertex {
		GLfloat x,y,z;
		GLuint  label;
	};

	static const int mW = 640;
	static const int mH = 480;

	static const GLint DMAP_FORMAT         = GL_LUMINANCE16UI_EXT;
	static const GLint DMAP_COLORFORMAT    = GL_LUMINANCE_INTEGER_EXT;
	static const GLint DMAP_COLORTYPE      = GL_UNSIGNED_SHORT;
	
	static const GLint LMAP_FORMAT         = GL_LUMINANCE8UI_EXT;
	static const GLint LMAP_COLORFORMAT    = GL_LUMINANCE_INTEGER_EXT;
	static const GLint LMAP_COLORTYPE      = GL_UNSIGNED_BYTE;

	RGBDRenderer();
	~RGBDRenderer();

	void draw( const float*         GLPMat,
	           const float*         GLMMat,
	           const uint16_t       BackgroundDepth,
	           const int            numVertices,
	           const int            numTriangles,
	           const LabeledVertex* vBuffer,
	           const GLuint*        iBuffer,
	           uint16_t*            depth,
	           uint8_t*             labels );

	protected :
	// the opengl context
	OpenGLContext mGLContext;

	// the textures and fbo
	GLuint mGLFBOId;
	GLuint mDMapTex;
	GLuint mLMapTex;
	GLuint mGLDepthTex;

	// the shader
	GLint mGLId_shad_vshader;
	GLint mGLId_shad_fshader;
	GLint mGLID_prog;

	// the matrices uniforms
	GLint mGLID_uniform_proj;
	GLint mGLID_uniform_mview;

	// the attribs uniforms
	GLint mGLID_attrib_coord;
	GLint mGLID_attrib_label;

};










#endif
