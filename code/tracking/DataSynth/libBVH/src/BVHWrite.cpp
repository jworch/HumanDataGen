/*

#include <libBVH/libBVH.h>
#include <fstream>


namespace BVH
{


void buildChildVec( const std::vector<bvhJoint>& joints,
                    std::vector<int>             child,
                    std::vector<int>             child_bounds )
{

	int numJoints = joints.size();
	child_bounds.resize(numJoints+1);

	// compute the bounds vector
	std::fill(child_bounds.begin(), child_bounds.end(), 0);
	for(int ji=0;ji<numJoints;++ji) {
		int parent = joints[ji].parent;
		if( parent != -1 ) child_bounds[parent+1]++;
	}
	std::vector<int> child_bounds(numJoints+1, 0);
		for(int ji=0;ji<numJoints;++ji) {
		child_bounds[pi+1] += child_bounds[pi];
	}


	// compute the child vector
	child.resize( child_bounds[numJoints] );
	std::vector<int> offset(child_bounds);
	for(int ji=0;ji<numJoints;++ji) {
		int parent = joint[ji].parent;
		if( parent != -1 ) {
			child[offset[parent]] = ji;
			offset[parent]++;
		}
	}
}


std::vector<int> findRoots( const std::vector<bvhJoint>& joints ) {
	int numJoints = joints.size();
	std::vector<int> res;
	for(int ji=0;ji<numJoints;++ji) {
		if( joints[ji].parent == -1 ) res.push_back(ji);
	}
}








bool bvhwriteHierarchy( const std::string&            filename,
                        const std::vector<bvhJoint>&  joints )
{

	std::ofstream fout(filename.c_str() );
	if( !fout.is_open() ) throw std::runtime_error( std::string("could not open: ") + filename );

	int numJoints = joints.size();

	// another representation of the graph
	std::vector<int> child;
	std::vector<int> child_bounds;
	buildChildVec(joints, child, child_bounds);

	// find the roots
	std::vector<int> roots = findRoots(joints);


	for( std::vector<int>::const_iterator root_itr = roots.begin(); root_itr != roots.end(); ++roots ) {

	}

	return true;
}



} // end namespace BVH

*/
