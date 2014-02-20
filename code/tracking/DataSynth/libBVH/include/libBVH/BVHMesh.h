/* *************************************************
 *
 * Copyright (2011) Willow Garage
 *
 * Author : Cedric Cagniart 
 * ************************************************* */

#ifndef BVHMESH_H_DEFINED
#define BVHMESH_H_DEFINED

#include "BVHTransforms.h"


namespace BVH
{

struct Triangle{
	int v0, v1, v2;
};


/**
 * just load a triangle list
 */
void loadMesh_Tri( const char*            filename,
                   std::vector<Triangle>& tris );

/**
 * load the vertex-joint associations
 */
void loadMesh_VJ( const char*                  filename,
                  const std::vector<bvhJoint>& joints,
                  std::vector<int>&            vjs );


/**
 * load the vertex labels 
 */
void loadMesh_labels( const char*              filename,
                      std::vector<int>&        labels );


/**
 * load the vertices rest positions
 */
void loadMesh_Vertices( const char*          filename,
                        std::vector<Vec3>&   X0 );

/**
 * transform the vertices 
 */
void transformMesh( const std::vector<Transform3>& transforms,
                    const std::vector<int>&        VJ,
                    const std::vector<Vec3>&       X0,
                    std::vector<Vec3>&             X );







} // end namespace BVH








#endif

