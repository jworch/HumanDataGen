#include "commonTrees/OptionParser.h"
#include "commonTrees/TTrain.h"
#include "commonTrees/TTrain_io.h"

// boost random
#include<boost/random/mersenne_twister.hpp> // the randomness source 
#include<boost/random/normal_distribution.hpp> // the distribution
#include<boost/random/uniform_real.hpp> // the distribution
#include<boost/random/uniform_int.hpp> // the distribution
#include <boost/random/variate_generator.hpp>
#include <ctime>

#include <cmath>
#include <fstream>
#include <stdexcept>

using namespace Tree;


/**
 * just generates a bunch of featureLocations by randomly sampling pairs of locations
 * with a given fixed max radius
 */
void addCandidateAttribLocations( int maxRadius, std::vector<AttribLocation>& alocs )
{
	boost::mt19937 rng; // the random number generator
	rng.seed( static_cast<unsigned int>(std::time(0)) );
	// create the distributions
	boost::normal_distribution<> distrib_r(0.,maxRadius);       // the distribution of radius 
	// TODO it might be beneficial to try to make the radius go through a log to encourage features closer to the pixel
	boost::uniform_real<> distrib_theta(0.,3.14159*2);   // the distribution of angle

	// create the number generators
	boost::variate_generator<boost::mt19937&, boost::normal_distribution<> > r(rng, distrib_r);
	boost::variate_generator<boost::mt19937&, boost::uniform_real<> > theta(rng, distrib_theta);

	while(alocs.size() < NUMATTRIBS) 
	{
		// draw sample from the distrib
		double r1     = 0.; 
		double r2     = 0.;
		do { r1 = fabs(r()); } while( r1 >= double(maxRadius) );
		do { r2 = fabs(r()); } while( r2 >= double(maxRadius) );
		double theta1 = theta();
		double theta2 = theta();

		// create the feature location
		AttribLocation aloc;
		aloc.du1 = round( r1 * cos(theta1) );
		aloc.dv1 = round( r1 * sin(theta1) );
		aloc.du2 = round( r2 * cos(theta2) );
		aloc.dv2 = round( r2 * sin(theta2) );

		// push it to the output
		alocs.push_back( aloc );
	}
}

void generateFixedLoc( std::vector<AttribLocation>& alocs ) 
{
	const int d[] = {2,5,10,25,50};
	const int numFix = 5; 

	// add all the comparisons between the pixel itself and up,left,right,down
	for(int i=0;i<numFix;++i) alocs.push_back(AttribLocation(0,0,0,-d[i]));  //0,up
	for(int i=0;i<numFix;++i) alocs.push_back(AttribLocation(0,0,0, d[i]));  //0,down
	for(int i=0;i<numFix;++i) alocs.push_back(AttribLocation(0,0,-d[i], 0));  //0,left
	for(int i=0;i<numFix;++i) alocs.push_back(AttribLocation(0,0, d[i], 0)); //0,right

	// add all the comparisons between the symmetrical up,left,right,down
	for(int i=0;i<numFix;++i) alocs.push_back(AttribLocation(0,-d[i],0,d[i]));  //down,up
	for(int i=0;i<numFix;++i) alocs.push_back(AttribLocation(-d[i],0,d[i], 0));  //left,right
}

void generateThresh( int numThreshs, int maxThresh,
                     std::vector<Attrib>& threshs ) 
{
	boost::mt19937 rng; // the random number generator
	rng.seed( static_cast<unsigned int>(std::time(0)) );
	boost::normal_distribution<> distrib_t(0., maxThresh/4.);
	boost::variate_generator<boost::mt19937&, 
	                         boost::normal_distribution<> > t( rng, distrib_t);

	threshs.clear();
	while( int(threshs.size()) < numThreshs ) {
		Attrib rthresh = Attrib( t() );
		if( abs(rthresh) >= maxThresh ) continue;

		std::vector<Attrib>::const_iterator f_itr = 
		                 std::find(threshs.begin(), threshs.end(), rthresh);
		if( f_itr == threshs.end() ) threshs.push_back(rthresh);
	}
	
	std::sort(threshs.begin(), threshs.end());
}


/**
 * This could probably have been written in 20 lines of python
 */
int main(int argc, char** argv)
{
	OptionParser opt(argc, argv);
	int numThreshs          = opt.getOption<int>("-numThreshs");
	int maxThreshAbs        = opt.getOption<int>("-maxThreshAbs");
	int maxRadius           = opt.getOption<int>("-maxRadius");
	std::string outAttribs  = opt.getOption<std::string>("-oAttribLoc");
	std::string outThreshs  = opt.getOption<std::string>("-oThreshs");
	// ---------
	// 1 - generate the attrib locations
	std::vector<AttribLocation> alocs;
	generateFixedLoc( alocs);
	addCandidateAttribLocations( maxRadius, alocs );

	// ----------
	// 2 - write them to disk
	writeAttribLocs( outAttribs, alocs);
	std::cout<<"generated "<<NUMATTRIBS<<" attrib locations and wrote it to "
	                                                    <<outAttribs<<std::endl;
	// ---------
	// 3 - generate thresholds
	std::vector<Attrib> threshs;
	generateThresh(numThreshs, maxThreshAbs, threshs);
	writeThreshs( outThreshs, threshs );

	std::cout<<"generated "<<numThreshs<<" threshsolds and wrote it to "
	                                                    <<outThreshs<<std::endl;
	return 0;
}
