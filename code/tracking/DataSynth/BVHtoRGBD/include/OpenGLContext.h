/* *************************************************
 *
 * Copyright (2010) Willow Garage
 *
 * Author : Cedric Cagniart, Koen Buys
 * ************************************************* */

#ifndef LIBOFFSCREENRENDERING_OPENGLCONTEXT_H_DEFINED
#define LIBOFFSCREENRENDERING_OPENGLCONTEXT_H_DEFINED

#include <X11/Xlib.h>
#include <GL/gl.h>
#include <GL/glx.h>
#include <string>

class OpenGLContext
{
  public :
  OpenGLContext(const char *displayName = NULL);
  ~OpenGLContext();
  
  void makeCurrent();
  const std::string &getDisplayName() const;
  
  protected :
  std::string		mDisplayName;
  
  Display* 		mDisplay;
  Colormap 		mColormap;
  XVisualInfo* 		mVinfo;
  GLXContext 		mCtx;
  GLXPbuffer 		mPBuffer;
};
#endif
