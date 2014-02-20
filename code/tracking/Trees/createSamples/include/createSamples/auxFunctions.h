#ifndef AUXFUNCTIONS_H_DEFINED
#define AUXFUNCTIONS_H_DEFINED

#include <vector>

#include "commonTrees/TTrain.h"
#include "LabeledSplitPointBuff.h"

namespace Tree
{


/**
 * just generates a bunch of featureLocations by randomly sampling pairs of locations
 * with a given fixed max radius
 * returns false if it can't find at least one pixel that has a valid label
 */
//bool createData( int                                 numRandomPoints,
//				 int                                 W,
//				 int                                 H,
//				 const uint16_t*                     dmap,
//				 const Label*                        lmap,
//				 const std::vector<AttribLocation>&  alocs,
//				 std::vector<LabeledFeature>&        lfs );

bool createData( int                                 numRandomPoints,
				 int                                 W,
				 int                                 H,
				 const uint16_t*                     dmap,
				 const Label*                        lmap,
				 const std::vector<AttribLocation>&  alocs,
				 const std::vector<Attrib>&          threshs,
				 std::vector<LabeledSplitPointBuff>& lsps );

} // end namespace Tree





#endif
