/* *************************************************
 *
 * Copyright (2010) Cedric Cagniart
 *
 * Author : Cedric Cagniart 
 * ************************************************* */


#include <OpenGLContext.h>
#include <iostream>
#include <cstdlib>


OpenGLContext::OpenGLContext(const char *displayName):
mDisplayName(displayName)
{
  std::cout << "(I): OpenGLContext constructor with display" << mDisplayName << std::endl;
  int attrib[] = { GLX_RGBA,
    GLX_RED_SIZE, 8,
    GLX_GREEN_SIZE, 8,
    GLX_BLUE_SIZE, 8,
    //GLX_DOUBLEBUFFER,
    GLX_STENCIL_SIZE, 1,
    GLX_DEPTH_SIZE, 24,
    None };


	// Open the display
	mDisplay  = XOpenDisplay(displayName);  // TODO : very big todo: take this as argument from shell $DISPLAY variable
	if(!mDisplay)
		std::cout<<"(E): OpenGLContext(): Could not open display"<<std::endl;

	// Create a Visual
	mVinfo    = glXChooseVisual(mDisplay, XDefaultScreen(mDisplay), attrib);
	if(!mVinfo)
		std::cout<<"(E): OpenGLContext(): Could not define a proper visual"<<std::endl;

	// Create a context
	mCtx      = glXCreateContext(mDisplay,
								 mVinfo,
								 NULL, 	   // Non sharing resources with another context
								 GL_TRUE); // DRI ON Direct rendering means we are talking directly to the hardware
										   // and not being forwarded through X
	if(!mCtx)
		std::cout<<"(ERROR) Could not create context"<<displayName<<std::endl;

	// create Pbuffer
	int nPbufferConfigs = 0;
	GLXFBConfig * pbufferConfigs = glXGetFBConfigs(mDisplay,XDefaultScreen(mDisplay), &nPbufferConfigs);
	int pbufferAttrib[] = { GLX_PBUFFER_WIDTH,  64, GLX_PBUFFER_HEIGHT, 64, None};
	mPBuffer =  glXCreatePbuffer(mDisplay, pbufferConfigs[0], pbufferAttrib);
	XFree(pbufferConfigs);

	// make the context current
	glXMakeCurrent(mDisplay, mPBuffer, mCtx);
	//glewInit();

	XSync( mDisplay, False );

}

OpenGLContext::~OpenGLContext()
{
	glXDestroyPbuffer(mDisplay, mPBuffer);
	glXDestroyContext(mDisplay, mCtx);
	XFree(mVinfo);
	XSetCloseDownMode(mDisplay, DestroyAll);
	XCloseDisplay(mDisplay);
}

const std::string& OpenGLContext::getDisplayName()const
{
	return mDisplayName;
}

void OpenGLContext::makeCurrent()
{
	glXMakeCurrent(mDisplay, mPBuffer, mCtx);
}

