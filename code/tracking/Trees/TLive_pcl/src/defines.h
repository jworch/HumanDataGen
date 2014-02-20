//@TODO: read this from a configuration parameter
#define WIDTH       640
#define HEIGHT      480
#define RES_V       HEIGHT        //vertical resolution
#define RES_H       WIDTH         //horizontal resolution
#define RES         WIDTH*HEIGHT
#define NUM_PARTS   25

//@TODO: learn this
#define AREA_THRES      200
#define AREA_THRES2     100
#define CLUST_TOL       0.05
#define CLUST_TOL_SHS   0.05
#define DELTA_HUE_SHS   5
#define MAX_CLUST_SIZE  25000

//@TODO: get this from configuration
#define NODE_NAME     "human_tracking"

// Comment these out for speed
//#define MULTIPLE_PERS
//#define VERBOSE
//#define VERBOSE2
//#define DISPL_INTERM
//#define DISPL_PRE_TREE
//#define DISPL_BINMASK
//#define DISPL_FLOWER
//#define DISPL_FLOWER_MAT
#define DISPL_LABEL2
#define WRITE                     // Write images to disk
