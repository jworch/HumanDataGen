/**
 * Copyright (2011) Willow Garage
 *
 * @authors Cedric Cagniart, Koen Buys, Caroline Pantofaru
 **/

// opencv
#include <opencv2/opencv.hpp>
#include <opencv2/highgui/highgui.hpp>

#include <iostream>
#include <csignal>
#include <fstream>

// libTreeLive
#include <libTreeLive/TreeLive.h>
#include "commonTrees/OptionParser.h"

// ROS related
#include "ros/ros.h"
#include "pcl_ros/point_cloud.h"

#include <pcl/pcl_base.h>
#include <pcl/point_types.h>
#include <pcl/point_cloud.h>
#include <pcl/io/pcd_io.h>
#include <pcl/kdtree/kdtree.h>
#include <pcl/segmentation/extract_clusters.h>
#include <pcl/segmentation/extract_labeled_clusters.h>
#include <pcl/people/conversions.h>

// libLabelSkel
#include "libLabelSkel/blob2.h"
#include "libLabelSkel/segment.h"
#include "libLabelSkel/display.h"
#include "libLabelSkel/conversion.h"
#include "libLabelSkel/tree.h"

//@TODO: read this from a configuration parameter
#define WIDTH       640
#define HEIGHT      480
#define RES_V       HEIGHT        //vertical resolution
#define RES_H       WIDTH         //horizontal resolution
#define RES         WIDTH*HEIGHT
#define NUM_PARTS   25

//@TODO: learn this
#define AREA_THRES  350

using namespace std;
using namespace sensor_msgs;
using namespace LabelSkel;
using namespace TreeLive;

MultiTreeLiveProc*          m_proc;
cv::Mat                     m_lmap;
cv::Mat                     m_cmap;
cv::Mat                     m_bmap;
vector<Blob2>               blobs;
vector<vector<Blob2> >      sorted;

int main(int argc, char** argv)
{
  OptionParser opt(argc, argv);
  std::string treeFilenames[4];
  int         numTrees  = opt.getOption<int>("-numTrees");
  treeFilenames[0]      = opt.getOption<std::string>("-tree0");
  treeFilenames[1]      = opt.getOption<std::string>("-tree1");
  treeFilenames[2]      = opt.getOption<std::string>("-tree2");
  treeFilenames[3]      = opt.getOption<std::string>("-tree3");

  assert(numTrees > 0 );
  assert(numTrees <= 4 );

  /// load the first tree file
  std::ifstream fin0(treeFilenames[0].c_str() );
  assert(fin0.is_open() );
  m_proc = new MultiTreeLiveProc(fin0);
  fin0.close();
  /// Load the other tree files
  for(int ti=1;ti<numTrees;++ti) {
    std::ifstream fin(treeFilenames[ti].c_str() );
    assert(fin.is_open() );
    m_proc->addTree(fin);
    fin.close();
  }

  // Read in the cloud data
  pcl::PCDReader reader;
  pcl::PointCloud<pcl::PointXYZRGB>::Ptr cloud_in (new pcl::PointCloud<pcl::PointXYZRGB>);
  reader.read ("koen.pcd", *cloud_in);
  std::cout << "PointCloud before filtering has: " << cloud_in->points.size () << " data points." << std::endl; //*

  cv::Mat imat(cloud_in->height, cloud_in->width, CV_8UC3);
  cv::Mat dmat(cloud_in->height, cloud_in->width, CV_16U);

  // Project pointcloud back into the imageplane
  LabelSkel::makeImageFromPointCloud(imat, *cloud_in);
  LabelSkel::makeDepthImage16FromPointCloud(dmat, *cloud_in);

  // Process the depthimage
  m_proc->process(dmat, m_lmap);

  pcl::PointCloud<pcl::PointXYZRGBL> cloud_labels;

  pcl::colorLabelPointCloudfromArray(*cloud_in, m_lmap.data, cloud_labels);

 // Creating the KdTree object for the search method of the extraction
  pcl::search::KdTree<pcl::PointXYZRGBL>::Ptr tree (new pcl::search::KdTree<pcl::PointXYZRGBL>);
  //pcl::search::OrganizedNeighbor<pcl::PointXYZRGBL>::Ptr tree (new pcl::search::OrganizedNeighbor<pcl::PointXYZRGBL>);

  std::vector<std::vector<pcl::PointIndices> > cluster_indices;
  cluster_indices.resize(NUM_PARTS);
  pcl::LabeledEuclideanClusterExtraction<pcl::PointXYZRGBL> ec;
  ec.setClusterTolerance (0.03); // 30cm
  //ec.setClusterTolerance (100); // 3pixels

  //@todo delete these magic numbers to something learned
  ec.setMinClusterSize (AREA_THRES);
  ec.setMaxClusterSize (25000);
  ec.setSearchMethod (tree);
  ec.setInputCloud( cloud_labels.makeShared());
  ec.setMaxLabels(NUM_PARTS);
  ec.extract (cluster_indices);

  for(unsigned int k = 0; k < cluster_indices.size(); k++)
  {
    std::cout << "For label " << k << " Found " << cluster_indices[k].size() << std::endl;
  }

  std::vector<std::vector<Blob2> >       sorted;
  //clear out our matrix before starting again with it
  sorted.clear();
  //Set fixed size of outer vector length = number of parts
  sorted.resize(NUM_PARTS);
  //create the blob2 matrix
  LabelSkel::sortIndicesToBlob2 ( cloud_labels, AREA_THRES, sorted, cluster_indices );
  LabelSkel::giveSortedBlobsInfo ( sorted );
  LabelSkel::buildRelations ( sorted );
////////////////////////////////////////////////////////
  pcl::PCDWriter writer;
  for (unsigned int i = 0; i < sorted.size(); i++)
  {
    for (unsigned int j = 0; j < sorted[i].size(); j++)
    {

      pcl::PointCloud<pcl::PointXYZRGBL>::Ptr cloud_cluster (new pcl::PointCloud<pcl::PointXYZRGBL>);

      for (unsigned int k = 0; k < sorted[i][j].indices.indices.size(); k++)
        cloud_cluster->points.push_back (cloud_labels.points[sorted[i][j].indices.indices[k]]);
      cloud_cluster->width = cloud_cluster->points.size ();
      cloud_cluster->height = 1;
      cloud_cluster->is_dense = true;

      std::cout << "PointCloud " << i << " " << j << " Cluster: " << cloud_cluster->points.size () << " data points." << std::endl;
      std::stringstream ss;
      ss << "cloud_cluster_" << i << "_" << j << ".pcd";
      writer.write<pcl::PointXYZRGBL> (ss.str (), *cloud_cluster, false); //*
    }
  }
/////////////////////////////////////////////////////

  return 0;
}
