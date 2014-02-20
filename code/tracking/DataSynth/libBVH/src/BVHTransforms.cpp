/* *************************************************
 *
 * Copyright (2011) Willow Garage
 *
 * Author : Cedric Cagniart 
 * ************************************************* */

#include <libBVH/BVHTransforms.h>

namespace BVH
{

using Eigen::AngleAxisf;
using Eigen::Translation3f;

void computeRestStateJointPos( const std::vector<bvhJoint>& joints,
                               std::vector<Vec3>&           jointPos )
{
  int numJoints = joints.size();
  jointPos.resize(numJoints);

  for(int ji=0;ji<numJoints;++ji)
  {
    const bvhJoint& joint = joints[ji];
    const int parent      = joints[ji].parent;
    const Vec3 offset( joint.ox, joint.oy, joint.oz );
    if( parent == -1) jointPos[ji] = offset;
    else              jointPos[ji] = jointPos[parent] + offset;
  }
}

void computeTis(const std::vector<bvhJoint>& joints,
                const std::vector<Vec3>&     jointRestPos,
                const std::vector<float>&    values,
                std::vector<Transform3>&     Tis)
{
  int numJoints = joints.size();
  Tis.resize(numJoints);

  // iterate through values 
  std::vector<float>::const_iterator values_itr = values.begin();
  for(int ji=0;ji<numJoints;++ji)
  {
  	const bvhJoint& joint = joints[ji];
  	const Vec3 offset( joint.ox, joint.oy, joint.oz );
  	switch( joints[ji].ctype )
  	{
  		case BVH_EMPTY:
  		{
  			Tis[ji] = Transform3::Identity();
  			break;
  		}
  		case BVH_XYZ_ZYX:
  		{
  			float tx = *values_itr++;
  			float ty = *values_itr++;
  			float tz = *values_itr++;
  			Vec3 t = Vec3( tx,ty,tz);
  			float rz = (3.159/180.)* *values_itr++;
  			float ry = (3.159/180.)* *values_itr++;
  			float rx = (3.159/180.)* *values_itr++;
  			Quat R = AngleAxisf(rz, Vec3(0,0,1) ) * AngleAxisf(ry, Vec3(0,1,0) ) * AngleAxisf(rx, Vec3(1,0,0) ) ;
  			Tis[ji] = Translation3f(t) * Translation3f(jointRestPos[ji]) * R * Translation3f(-jointRestPos[ji]);
  			break;
  		}
  		case BVH_ZYX:
  		{
  			float rz = (3.159/180.)* *values_itr++;
  			float ry = (3.159/180.)* *values_itr++;
  			float rx = (3.159/180.)* *values_itr++;
  			Quat R = AngleAxisf(rz, Vec3(0,0,1) ) * AngleAxisf(ry, Vec3(0,1,0) ) * AngleAxisf(rx, Vec3(1,0,0) ) ;
  			Tis[ji] = Translation3f(jointRestPos[ji]) * R * Translation3f(-jointRestPos[ji]);
  			break;
  		}
  		default : assert(0); break;
  	}
  }
}


void computeTTis( const std::vector<bvhJoint>&   joints,
                  const std::vector<Transform3>& Tis,
                  std::vector<Transform3>&       TTis )
{
	int numJoints = joints.size();
	assert( int(Tis.size()) == numJoints );
	TTis.resize(numJoints);

	for(int ji=0;ji<numJoints;++ji) 
	{
		const int parent = joints[ji].parent;
		assert( parent < ji);
		if(parent == -1 ) TTis[ji] = Tis[ji];
		else              TTis[ji] = TTis[parent] * Tis[ji];
	}
}

} // end namespace BVH 
