/* *************************************************
 *
 * Copyright (2010) Cedric Cagniart
 *
 * Author : Cedric Cagniart 
 * ************************************************* */

#ifndef GL_ERRORS_H_DEFINED
#define GL_ERRORS_H_DEFINED



void checkGLFrameBufferStatus();
void checkGLErrors(const char *label);
void checkGLProgram(unsigned int progId);
void checkGLShader(unsigned int shaderId);





#endif
