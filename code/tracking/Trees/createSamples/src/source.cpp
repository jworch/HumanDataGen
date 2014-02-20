/**
 * @brief this file creates the samples out of all the images
 * @authors Cedric Cagnairt, Koen Buys
 **/
#include "commonTrees/OptionParser.h"
#include "commonTrees/TTrain.h"
#include "commonTrees/TTrain_io.h"
#include "createSamples/auxFunctions.h"

#include <iostream>
#include <fstream>

#include <opencv/cv.h>
#include <opencv/highgui.h>

#include <cassert>

using namespace Tree;

int main(int argc, char** argv)
{
  OptionParser opt(argc, argv);
  std::string attribLocFile = opt.getOption<std::string>("-attribLoc");
  std::string threshFile    = opt.getOption<std::string>("-threshFile");
  std::string lmapBasename  = opt.getOption<std::string>("-lmapBasename");
  std::string dmapBasename  = opt.getOption<std::string>("-dmapBasename");
  int         F             = opt.getOption<int>("-F");
  int         L             = opt.getOption<int>("-L");
  std::string outName       = opt.getOption<std::string>("-o");

  // open the output file
  std::ofstream fout( outName.c_str(), std::ios::binary );
  if( !fout.is_open() ) throw( std::runtime_error(std::string("(E) could not open ") + outName) );

  std::vector<Attrib>         threshs;
  std::vector<AttribLocation> alocs;
  readAttribLocs(attribLocFile, alocs);
  readThreshs(threshFile, threshs);

  // just in case.. people should know that this is the limit
  assert(int(threshs.size()) <= int(std::numeric_limits<char>::max()) ); 

  // generate the stuff
  //std::vector<LabeledSplitPointBuff> lsps;

  #pragma omp parallel for
  for(int frame=F;frame<=L;++frame) 
  {
    std::string lfilename = buildFilename(lmapBasename, frame);
    std::string dfilename = buildFilename(dmapBasename, frame);

    cv::Mat limg = cv::imread(lfilename, -1);
    if(limg.data == NULL){
      std::cout << "(E) could not read " << lfilename << std::endl;
      continue;
    }
    cv::Mat dimg = cv::imread(dfilename, -1);
    if(dimg.data == NULL){
      std::cout << "(E) could not read " << dfilename << std::endl;
      continue;
    }

    if( limg.depth() != CV_8U || limg.channels() != 1 || dimg.depth() != CV_16U || dimg.channels() != 1 ){
      std::cout << "(E) files are not correct depth or channels " << lfilename << " " << dfilename << std::endl;
      continue;
    }

    if( limg.step != 640 || dimg.step != 640*sizeof(uint16_t) ){
      std::cout << "(E) image step is not correct  (exp resolution = 640)" << std::endl;
      continue;
    }

    const uint16_t* dmap = (const uint16_t*) dimg.data;
    const uint8_t*  lmap = (const uint8_t*)  limg.data;

    std::vector<LabeledSplitPointBuff> lsps;

    // create the array of labeled features from the image data
    createData( 2000, 640, 480, dmap, lmap, alocs, threshs, lsps);
    assert( lsps.size() == 2000);

    // write the array to file
    #pragma omp critical
    {
      writeLabeledSplitPointBuffVec( fout, lsps );
    }

//		std::cout<<"generated features for file: "<<lfilename<<"\r";
#ifdef VERBOSE
    std::cout<<"generated features for file: "<<lfilename<<"\n";
    std::cout.flush();
#endif
  }
  return 0;
}
