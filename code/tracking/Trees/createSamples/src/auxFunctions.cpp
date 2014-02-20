#include "commonTrees/TexFetch.h"
#include "createSamples/auxFunctions.h"

// boost random
#include<boost/random/mersenne_twister.hpp> // the randomness source 
#include<boost/random/uniform_real.hpp> // the distribution
#include<boost/random/uniform_int.hpp> // the distribution
#include <boost/random/variate_generator.hpp>

#include <ctime>

namespace Tree
{
/**
 * returns -1 if it cant find a valid pixel inside
 * this function returns inclusive bounds ... so umax, vmax are effectively seen..
 */
int getBBox( int W, int H, const Label* lmap, int& umin, int& vmin, int& umax, int& vmax )
{
	umin = W;
	vmin = H;
	umax = 0;
	vmax = 0;

	// NOLABEL is the outlier class
	for(int y=0;y<H;++y) 
	{
		const Label* const l_ptr = lmap + W*y;
		int cbeg = 0;
		int cend = W-1;

		while( cbeg != W ) {// ffwd until we meed a valid label
			if (l_ptr[cbeg] != NOLABEL ) break;
			cbeg++;
		}

		while( cend != 0 ) {// rewind until we meet a valid label
			if (l_ptr[cend] != NOLABEL ) break;
			cend--;
		}

		if (cbeg < umin ) umin = cbeg;
		if (cend > umax ) umax = cend+1;
		if ( vmin == H && cbeg != W ) vmin = y; // if umin hasnt been set yet and we re not an empty line
		if ( cbeg != W )              vmax = y; // we ll just set them all until the last non empty line
	}

	if( umin == W ) return -1; // if we never ever met a labeled pixel
	return 0;
}



//public static byte findSplitPoint(int val, int[] threshs) {
//        // we look for the index of the first element >= val ( the attrib)
//        // then we will accum false for all the smaller and true for all the others
//        int splitPoint = Arrays.binarySearch(threshs, val);
//        // if we have [0,1,2,3,4,5,6,7] as thresh array
//        // # 4 as incoming sample
//        //   we get 4 as split point
//        //   then we accum true on [0,1,2,3]
//        //                false on [4,5,6,7]
//        // # 3.5 as incoming sample
//        //   we get -5 as split point ( -4 (insertion point) - 1 )
//        //   we transform it to 4 because we want the same behaviour
//        if( splitPoint  < 0 ) {
//            splitPoint = -splitPoint-1;
//        }
//        // # 10 as incoming sample
//        // we got -8-1 (-list size-1) as split point
//        // we transform it to 8
//        if(splitPoint > threshs.length) return (byte)( threshs.length);
//        else                            return (byte) splitPoint;
//    }

inline char findSplitPoint(Attrib val, const std::vector<Attrib>& threshs) {
	std::vector<Attrib>::const_iterator f_itr = std::lower_bound( threshs.begin(), threshs.end(), val);
	return char(f_itr-threshs.begin()); 
}


bool createData( int                                 numRandomPoints,
				 int                                 W,
				 int                                 H,
				 const uint16_t*                     dmap,
				 const Label*                        lmap,
				 const std::vector<AttribLocation>&  alocs, // should have numattribs elements
				 const std::vector<Attrib>&          threshs,
				 std::vector<LabeledSplitPointBuff>& lsps )
{
	// 1 - find the bbox
	int umin, vmin, umax, vmax;
	int res = getBBox( W, H, lmap, umin, vmin, umax, vmax );
	if ( res == -1 ) return false;

	// 2 - sample until we have all enought points
	boost::mt19937 rng; // the random number generator
	rng.seed( static_cast<unsigned int>(std::time(0)) );
	boost::uniform_int<> distrib_u(umin, umax);
	boost::uniform_int<> distrib_v(vmin, vmax);
	boost::variate_generator<boost::mt19937&, boost::uniform_int<> > u( rng, distrib_u);
	boost::variate_generator<boost::mt19937&, boost::uniform_int<> > v( rng, distrib_v);

	// 3 - alloc some memory
	assert( alocs.size() == NUMATTRIBS );
	LabeledSplitPointBuff lsp;
	Tex2Dfetcher tfetch( dmap, W, H ); // the tex fetcher

	// 4 - query little guy, query !
	lsps.clear();
	for( int pi=0;pi<numRandomPoints;++pi )
	{
		// first, we ll find a pixel that effecitvely has a label
		int pu;
		int pv;
		Label label;
		do {
			// crazyness... doing this gets (pu-umin)/(umax-umin) == (pv-vmin)/(vmax-vmin)
			// solved... needed the variate generator to take a mt199937& and not a mt199937 as template param....
			pu = u();
			pv = v(); 
			label = lmap[pv*W+pu];
		} while( label == NOLABEL );

		// then we ll query for his features
		uint16_t depth = tfetch(pu,pv);
		double scale = focal / depth;
		int32_t attribINF = std::numeric_limits<Attrib>::min();
		int32_t attribSUP = std::numeric_limits<Attrib>::max();
		for(int ai=0;ai<NUMATTRIBS;++ai) {
			const AttribLocation& aloc = alocs[ai];
			uint16_t d1       = tfetch(pu+aloc.du1*scale, pv+aloc.dv1*scale);
			uint16_t d2       = tfetch(pu+aloc.du2*scale, pv+aloc.dv2*scale);
			int32_t delta     = int32_t(d1) - int32_t(d2);
			Attrib attrib;
			// clamp it onto a stupid int16_t
			if( delta < attribINF )      attrib = Attrib(attribINF);
			else if( delta > attribSUP ) attrib = Attrib(attribSUP);
			else                         attrib = Attrib(delta);
			lsp.data[ai+2] = findSplitPoint(attrib, threshs);
		}
		// write the label to the Labeled feature
		lsp.data[0] = label;
		// add to the array
		lsps.push_back(lsp);
	}
	return true;
}

} // end namespace Tree
