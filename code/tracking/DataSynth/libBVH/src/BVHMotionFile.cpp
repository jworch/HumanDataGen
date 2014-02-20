/* *************************************************
 *
 * Copyright (2011) Willow Garage
 *
 * Author : Cedric Cagniart 
 * ************************************************* */

#include <libBVH/BVHMotionFile.h>
#include <fstream>
#include <stdexcept>
#include <sstream>


namespace BVH
{

BVHMotionFile::BVHMotionFile(const std::string& filename)
{
	std::ifstream* finptr = new std::ifstream(filename.c_str());
	if( !finptr->is_open() ) {
		delete finptr;
		throw( std::runtime_error(std::string("(E)could not open: ") + std::string(filename)));
	}

	mPriv = finptr;
}



BVHMotionFile::~BVHMotionFile()
{
	std::ifstream* finptr = static_cast<std::ifstream*>(mPriv);
	delete finptr;
}



bool BVHMotionFile::readNextFrame( const std::vector<bvhJoint>& joints,
                                   std::vector<float>&          values )
{
	// recast private imp detail to real type
	std::ifstream* finptr = static_cast<std::ifstream*>(mPriv);

	// prepare to read
	int numJoints = joints.size();
	int paramSize = 0;
	for(int ji=0;ji<numJoints;++ji ) {
		paramSize += bvhChannelSize[ joints[ji].ctype ];
	}

	// resize
	values.resize(paramSize);


	std::string line;
	getline(*finptr, line);
	std::istringstream iss(line);
	// read in
	for(int vi=0;vi<paramSize;++vi) {
		iss >> values[vi];
	}

	if( iss.fail() ) return false;
	return true;
}





} // end namespace BVH


