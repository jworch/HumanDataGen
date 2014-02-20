/**
 * @copyright (2011) Willow Garage
 * @author Cedric Cagniart, Koen Buys
 **/

#include <libBVH/libBVH.h>
#include <libBVH/BVHTransforms.h>
//@todo: delete this "hack"
#include "../../CMUTransforms/CMUTransforms.h"

#include "OptionParser.h"

#include <list>

#include <cassert>
#include <iostream>
#include <fstream>
#include <stdexcept>
#include <cmath>

#define THRESH 0.05 // 5 cm

using namespace BVH;


/**
 * This function will simply compute the maximal displacement over all the
 * joints between two poses. 
 */
inline float posDist( const std::vector<Vec3>& pos1,
                      const std::vector<Vec3>& pos2 )
{
  int numJoints = pos1.size();
  assert( int (pos2.size() ) == numJoints );

  float maxDist = 0.;
  for(int ji=0;ji<numJoints;++ji) {
    Vec3 delta = pos1[ji] - pos2[ji];
    float d2 = delta.dot(delta);
    if( d2 > maxDist ) maxDist = d2;
  }

  return sqrt(maxDist);
}

/**
 * Load all the filenames from the BVHList.txt file
 */
void loadFilenames(const std::string&        file,
                   std::vector<std::string>& fileList )
{
  std::ifstream fin(file.c_str());
  if( !fin.is_open() ) throw std::runtime_error(std::string("(E): could not open ") + file );

  fileList.clear();
  while( !fin.eof() ) {
    std::string buff;
    fin >> buff;
    fileList.push_back( buff );
  }
}

int main(int argc, char** argv)
{
  OptionParser opt(argc, argv);
  std::string BVH_ref    = opt.getOption<std::string>("-BVH_ref");
  std::string BVH_list   = opt.getOption<std::string>("-BVH_list");
  std::string dbFilename = opt.getOption<std::string>("-DB");

  std::vector<bvhJoint>             joints_ref;
  float                             period_ref;
  std::vector<std::vector<float> >  values_ref;
  std::vector<Vec3>                 pos_ref;
  try {
    bvhparseFile(BVH_ref, joints_ref, period_ref, values_ref);
    computeRestStateJointPos( joints_ref, pos_ref);
  }  catch( std::exception& err ) { std::cout<<err.what()<<std::endl; exit(0); }
  int numJoints = joints_ref.size();

  // 1 - load the filenames
  std::vector<std::string> BVHFiles;
  loadFilenames(BVH_list, BVHFiles);

  std::cout <<"Got " << BVHFiles.size() << " files loaded" << std::endl;

  // 2 - create the pos
  std::ofstream fout( dbFilename.c_str() );
  std::list<std::vector<Vec3> > samples; // the crazy vector representing the accepted samples

  int totalParsedPoses = 0;
  // 3 - run through the database 
  #pragma omp parallel for
  for(unsigned int i = 0; i < BVHFiles.size(); i++)
  {
    std::cout<<"processing frame "<< i
             <<" / "<< BVHFiles.size() <<" and we have "<< samples.size()
             <<" elements out of "<< totalParsedPoses << std::endl;
    try {
      std::vector<bvhJoint>             joints;
      float                             period;
      std::vector<std::vector<float> >  values;
      std::vector<Transform3>           Tis;
      std::vector<Transform3>           TTis;
      std::vector<Vec3>                 pos(pos_ref);
      // load the bvh file 
      bool ret = bvhparseFile(BVHFiles[i], joints, period, values);

      if(!ret) continue;

      totalParsedPoses += values.size();

      // run through the poses in the bvh file
      for( std::vector<std::vector<float> >::const_iterator v_itr = values.begin(); 
                                                      v_itr != values.end(); ++v_itr)
      {
        // compute the transformations
        computeTis( joints_ref, pos_ref, *v_itr, Tis );

        // Reject the non standing poses .... so these that do not 
        if( !CMUisStanding(joints_ref,Tis) ) continue ;

        // rebase the mesh at the origin
        CMUScaleResetHips(joints_ref, Tis);
        computeTTis( joints, Tis, TTis );

        // compute the associated pose 
        for(int ji=0;ji<numJoints;++ji) pos[ji] = TTis[ji] * pos_ref[ji];
        // compare against the accepted samples
        float minDist = std::numeric_limits<float>::max();
        for( std::list<std::vector<Vec3> >::const_iterator s_itr = samples.begin(); 
                                                   s_itr != samples.end(); ++s_itr ) {
          float dist = posDist(*s_itr, pos );
          if( dist < minDist ) minDist = dist;
        }
        // if we are far away enough from all the previous samples, 
        // add to the samples and write the values to disk
        if(minDist > THRESH) {
          #pragma omp critical(update)
          {
            samples.push_back( pos );
            for( std::vector<float>::const_iterator itr = v_itr->begin(); 
                              itr != v_itr->end(); ++itr) fout<<*itr<<" ";
            fout<<"\n";
          }
        }
      }
    } catch( std::exception& err ) { std::cout<<err.what()<<std::endl; }
  }
  std::cout << std::endl <<  "Processed all to " << samples.size() << " samples" << std::endl;
  return 0;
}
