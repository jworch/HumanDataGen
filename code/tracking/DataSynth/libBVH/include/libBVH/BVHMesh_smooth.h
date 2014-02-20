/* *************************************************
 *
 * Copyright (2011) Willow Garage
 *
 * Author : Cedric Cagniart 
 * ************************************************* */

#ifndef BVHMESH_SMOOTH_H_DEFINED
#define BVHMESH_SMOOTH_H_DEFINED



#include "BVHTransforms.h"



namespace BVH
{


struct vertex_joint {
	int boneId;
	float weight;
};

void loadMesh_VJ_smooth( const char*                  filename,
                         const std::vector<bvhJoint>& joints,
                         std::vector<vertex_joint>&   vjs,          // sorted by vertices
                         std::vector<int>&            vjs_bounds ); // NVertices+1 long, vjs_bounds[i] is begin offset for vertex i, vjs_bounds[i+1] is end offset


void transformMesh_smooth( const std::vector<Transform3>&     transforms,
                           const std::vector<vertex_joint>&   vjs,
                           const std::vector<int>&            vjs_bounds,
                           const std::vector<Vec3>&           X0,
                           std::vector<Vec3>&                 X );



/**
 * duplicate label borders.. so that the boundaries of labels will be nice and clean
 */
/*
void duplicateLabelBorders( const std::vector<Vec3>&        X0,
                            const std::vector<Triangle>&    tris0,
                            const std::vector<int>&         labels,
                            const std::vector<vertex_joint> vjs,
                            const std::vector<int>          vjs_bounds,
                            std::vector<Vec3>&              X0_d, // d stands for duplicated 
                            std::vector<Vec3>&              tris0_d,
                            std::vector<Vec3>&              labeld_d,
                            std::vector<vertex_joint>&      vjs_d,
                            std::vector<int>&               vjs_bounds_d);

*/
} // end namespace BVH





















#endif
