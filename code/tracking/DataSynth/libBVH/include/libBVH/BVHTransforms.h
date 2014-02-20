/* *************************************************
 *
 * Copyright (2011) Willow Garage
 *
 * Author : Cedric Cagniart 
 * ************************************************* */

#ifndef BVHTRANSFORMS_H_DEFINED
#define BVHTRANSFORMS_H_DEFINED

#include "libBVH.h"

#include <eigen2/Eigen/Core>
#include <eigen2/Eigen/Geometry> // for the transforms and stuff

namespace BVH 
{
typedef Eigen::Matrix3f     Mat3;
typedef Eigen::Vector3f     Vec3;
typedef Eigen::Transform3f  Transform3;
typedef Eigen::Quaternionf  Quat;

/**
 * Computes the positions of joints in the rest state
 */
void computeRestStateJointPos( const std::vector<bvhJoint>& joints,
                               std::vector<Vec3>&           jointPos );

/**
 * Computes for each joint its 3D Rigid Transform given the value vector
 */
void computeTis(const std::vector<bvhJoint>& joints,
                const std::vector<Vec3>&     jointRestPos,
                const std::vector<float>&    values,
                std::vector<Transform3>&     Tis);

/**
 * Accumulates for each joint its transform with that of its parent 
 */
void computeTTis( const std::vector<bvhJoint>&   joints,
                  const std::vector<Transform3>& Tis,
                  std::vector<Transform3>&       TTis );

} // end namespace BVH

#endif
