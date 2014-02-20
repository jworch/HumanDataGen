

// THIS IS KIND OF AN UGLY HACK
// This should be data...
// For now please make sure that you include libBVH before calling this 
#ifndef BVHTRANSFORMS_H_DEFINED
	#error "Please include BVHTransforms.h before this header"
#endif


namespace BVH 
{

using Eigen::AngleAxisf;
using Eigen::Translation3f; 





/**
 * applies the stupid scaling factor for the CMU Mocap database.
 * grounds the origin of skel to world origin.
 * orients the hips so that Z is up and they point to Y-.
 */
void CMUScaleResetHips( const std::vector<bvhJoint>&   joints,
                        std::vector<Transform3>&       Tis)
{
	for(size_t ji=0;ji<joints.size();++ji) 
	{
		if(joints[ji].parent == -1) {
			Tis[ji] = Transform3::Identity();
			Tis[ji].scale(0.056444);
		}
	}
}

bool CMUisStanding( const std::vector<bvhJoint>&   joints,
                    const std::vector<Transform3>& Tis) {
	for(size_t ji=0;ji<joints.size();++ji)  
	{
		if(joints[ji].parent == -1) {
			Vec3 upward = Tis[ji].linear() * Vec3(0,1,0);
			if( upward[1] > 0.85) return true; // 0.85 is 30 deg
		}
	}
	return false;
}


/**
 * applies the stupid scaling factor for the CMU Mocap database.
 * grounds the origin of skel to world origin.
 * orients the hips so that they mostly point to Y-
 */
static inline void CMU_scaleRotateHips( const std::vector<bvhJoint>&   joints,
                                        std::vector<Transform3>&       Tis,
                                        double                         angle=0.)
{
	for(size_t ji=0;ji<joints.size();++ji)  
	{
		if(joints[ji].parent == -1) {
			// important to take the linear part so we dont translate
			Vec3 Z = Tis[ji].linear() * Vec3(0,0,1); 
			float norm = sqrtf(Z[0]*Z[0] + Z[2]*Z[2]);
			float costheta = Z[2]/norm;
			float sintheta = Z[0]/norm;
			float theta = atan2(sintheta, costheta);


//			// this will rotate the model in its original pose 
//			// ( head up Y, Hips facing a vector in X,Z plane )
//			// so that it points towards the Z plane
			// then add "angle" to it
			Transform3 R2(AngleAxisf(-theta+angle,Vec3(0,1,0)));
			Transform3 S(Eigen::Scaling3f(0.056444));
			Tis[ji] = R2*S*Tis[ji];

			// we do not want to create an offset in the 
			// vertical direction ( Y+ is up in cmu)
			Vec3 centeroffset = Tis[ji].translation();
			centeroffset(1) = 0; 
			Tis[ji] = Translation3f(-centeroffset)*Tis[ji];
		}
	}
}

}
