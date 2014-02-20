/* *************************************************
 *
 * Copyright (2011) Willow Garage
 *
 * Author : Cedric Cagniart 
 * ************************************************* */

#ifndef LIBBVH_H_DEFINED
#define LIBBVH_H_DEFINED

#include <vector>
#include <string>

namespace BVH 
{

enum bvhJointType   { BVH_ROOT, BVH_JOINT, BVH_END };
enum bvhChannelType { BVH_EMPTY, BVH_XYZ_ZYX, BVH_ZYX };//BVH_ZXY};
static const int bvhChannelSize[] =  { 0, 6, 3 };

struct bvhJoint {
	std::string    name;
	bvhChannelType ctype;
	float          ox, oy, oz;
	int            parent;
};




bool bvhparseFile ( const std::string&                 filename,
                    std::vector<bvhJoint>&             skel,
                    float&                             period,
                    std::vector<std::vector<float> >&  values );
/*
bool bvhwriteHierarchy( const std::string&            filename,
                        const std::vector<bvhJoint>&  skel );
*/




} // end namespace BVH

#endif
