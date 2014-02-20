#ifndef LABELEDSPLITPOINTBUFF_H_DEFINED
#define LABELEDSPLITPOINTBUFF_H_DEFINED


#include "commonTrees/Tree.h"
#include <vector>
#include <iostream>

namespace Tree 
{

struct LabeledSplitPointBuff {
	char data[1+1+NUMATTRIBS]; // we padd explicitely the label
};

static inline void writeLabeledSplitPointBuffVec( 
                               std::ostream& os,
                               const std::vector<LabeledSplitPointBuff>& lsps) 
{
	os.write( (const char*)(lsps.data()), lsps.size()*sizeof(LabeledSplitPointBuff));
}

}

#endif
