/* *************************************************
 *
 * Copyright (2011) Willow Garage
 *
 * Author : Cedric Cagniart 
 * ************************************************* */

#ifndef BVHMOTIONFILE_H_DEFINED
#define BVHMOTIONFILE_H_DEFINED


#include "libBVH.h"

namespace BVH
{

/**
 * Small helper structure that will read in a huge motion file
 */
class BVHMotionFile {
	public :
	BVHMotionFile(const std::string& filename);
	~BVHMotionFile();

	bool readNextFrame( const std::vector<bvhJoint>& joints,
	                    std::vector<float>&          values );

	private :
	void* mPriv;
};



} // end namespace BVH






#endif
