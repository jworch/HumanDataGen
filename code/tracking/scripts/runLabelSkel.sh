#!/bin/bash





gdb --args \
/u/ccagniart/release/human_tracking/build/bin/labelSkel \
              -weights partRelation.txt \
              -lmap l.png -dmap d.png


