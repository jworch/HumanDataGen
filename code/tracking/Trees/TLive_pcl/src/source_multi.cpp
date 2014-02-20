/**
 * @brief This file is the execution node of the Human Tracking 
 * @copyright Copyright (2011) Willow Garage
 * @authors Cedric Cagniart, Koen Buys, Caroline Pantofaru
 **/

// opencv
#include <opencv2/opencv.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>

// Standard includes
#include <iostream>
#include <csignal>
#include <fstream>
#include <time.h>

// libTreeLive
#include <libTreeLive/TreeLive.h>
#include "commonTrees/OptionParser.h"

// ROS related
#include "ros/ros.h"

#include "sensor_msgs/Image.h"
#include <sensor_msgs/PointCloud2.h>

#include <cv_bridge/CvBridge.h>
#include <image_transport/image_transport.h>

#include <visualization_msgs/Marker.h>
#include <visualization_msgs/MarkerArray.h>

// PCL related
#include "pcl_ros/point_cloud.h"
#include <pcl/pcl_base.h>
#include <pcl/point_types.h>
#include <pcl/point_cloud.h>
#include <pcl/io/pcd_io.h>
#include <pcl/kdtree/kdtree.h>
#include <pcl/segmentation/extract_clusters.h>
#include <pcl/segmentation/extract_labeled_clusters.h>
#include <pcl/segmentation/seeded_hue_segmentation.h>
#include <pcl/people/conversions.h>
#include <pcl/filters/passthrough.h>

#include <pcl/common/time.h>

void optimized_elec(const pcl::PointCloud<pcl::PointXYZRGB> &cloud, const cv::Mat& src_labels, float tolerance,
                    std::vector<std::vector<pcl::PointIndices> > &labeled_clusters,
                    unsigned int min_pts_per_cluster, unsigned int max_pts_per_cluster, unsigned int num_parts,
                    bool brute_force_border = false, float radius_scale = 1.f);

void optimized_shs(const pcl::PointCloud<pcl::PointXYZRGB> &cloud, float tolerance, pcl::PointIndices &indices_in, pcl::PointIndices &indices_out, float delta_hue);
void optimized_shs2(const pcl::PointCloud<pcl::PointXYZRGB> &cloud, float tolerance, pcl::PointIndices &indices_in, pcl::PointIndices &indices_out, float delta_hue);
void optimized_shs3(const pcl::PointCloud<pcl::PointXYZRGB> &cloud, float tolerance, pcl::PointIndices &indices_in, pcl::PointIndices &indices_out, float delta_hue);

#include <pcl/gpu/containers/device_array.h>
typedef pcl::gpu::DeviceArray2D<unsigned char> Labels;
typedef pcl::gpu::DeviceArray2D<unsigned short> Depth;
void smoothLabelImage(const Labels& src, const Depth& depth, Labels& dst, int num_parts, int  patch_size = 5, int  depthThres = 300);



// libLabelSkel
#include "libLabelSkel/blob2.h"
#include "libLabelSkel/segment.h"
#include "libLabelSkel/display.h"
#include "libLabelSkel/tree.h"
#include "libLabelSkel/tree2.h"
#include "libLabelSkel/conversion.h"

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

//#define GPU_SMOOTH_LABEL

using namespace std;
using namespace sensor_msgs;
using namespace TreeLive;

MultiTreeLiveProc*                    m_proc;
cv::Mat                               m_lmap;
cv::Mat                               m_cmap;
cv::Mat                               cmap;
cv::Mat                               m_bmap;
vector<LabelSkel::Blob2>              blobs;
vector<vector<LabelSkel::Blob2> >     sorted;

image_transport::Publisher            g_pub_blob_;
image_transport::Publisher            g_pub_label_;
image_transport::Publisher            g_pub_label2_;
image_transport::Publisher            g_pub_flower_;

ros::Publisher                        pub_pc_;
ros::Publisher                        pub_pc_blob_;
ros::Publisher                        pub_pc_flower_;

ros::Publisher                        pub_eig_;
ros::Publisher                        pub_tree_;

ros::Subscriber                       sub_pc_;

bool                                  first_cloud = true;
bool                                  cam_info_rec_;

visualization_msgs::MarkerArray       markerArray_tree; 

unsigned int u,v;

/**
 * @brief this is the roscallback routine, gets a Pointcloud in
 * Publishes all info
 **/
void dataCallback( const PointCloud2ConstPtr& point_cloud)
{
  // skip the first cloud that comes in, cause it might show problems
  if(first_cloud == true){
    first_cloud = false;
    return;
  }
  /// @todo rewrite this to a pointcloud::Ptr
  pcl::PointCloud<pcl::PointXYZRGB> cloud_in;
  pcl::PointCloud<pcl::PointXYZRGB> cloud_in_filt;
  pcl::fromROSMsg(*point_cloud, cloud_in);

  cv::Mat dmat(cloud_in.height, cloud_in.width, CV_16U);

  // Project pointcloud back into the imageplane
  LabelSkel::makeDepthImage16FromPointCloud(dmat, cloud_in);

  // Process the depthimage (CUDA)
  m_proc->process(dmat, m_lmap);

  cv::Mat lmap(cloud_in.height, cloud_in.width, CV_8UC1);
  // smoothLabelImage seem to give a little bit less noise then medianBlur, but speed advantages

#ifndef GPU_SMOOTH_LABEL
  LabelSkel::smoothLabelImage(m_lmap, dmat, lmap);
#else
  //warning GPU allocations are expensive
  Depth dmat_dev;
  Labels m_lmap_dev, lmap_dev(lmap.rows, lmap.cols);
  m_lmap_dev.upload((void*)m_lmap.data, m_lmap.step, m_lmap.rows, m_lmap.cols);
  dmat_dev.upload((void*)dmat.data, dmat.step, dmat.rows, dmat.cols);
  smoothLabelImage(m_lmap_dev, dmat_dev, lmap_dev, NUM_PARTS);
  lmap_dev.download(lmap.data, lmap.step);
#endif
  // cv::medianBlur(m_lmap, lmap, 3);

  pcl::PointCloud<pcl::PointXYZRGBL> cloud_labels;

  pcl::colorLabelPointCloudfromArray(cloud_in, lmap.data, cloud_labels);

  // Creating the Search object for the search method of the extraction
  //pcl::search::KdTree<pcl::PointXYZRGBL>::Ptr stree (new pcl::search::KdTree<pcl::PointXYZRGBL>);
  pcl::search::OrganizedNeighbor<pcl::PointXYZRGBL>::Ptr stree (new pcl::search::OrganizedNeighbor<pcl::PointXYZRGBL>);
  stree->setInputCloud(cloud_labels.makeShared());

  std::vector<std::vector<pcl::PointIndices> > cluster_indices;
  cluster_indices.resize(NUM_PARTS);

  // Make all the clusters
  //pcl::extractLabeledEuclideanClusters<pcl::PointXYZRGBL>(cloud_labels, stree, CLUST_TOL, cluster_indices, AREA_THRES, MAX_CLUST_SIZE, NUM_PARTS);
  optimized_elec(cloud_in, lmap, CLUST_TOL, cluster_indices, AREA_THRES, MAX_CLUST_SIZE, NUM_PARTS);

  // Create a new struct to put the results in
  std::vector<std::vector<LabelSkel::Blob2> >       sorted;
  //clear out our matrix before starting again with it
  sorted.clear();
  //Set fixed size of outer vector length = number of parts
  sorted.resize(NUM_PARTS);
  //create the blob2 matrix
  LabelSkel::sortIndicesToBlob2 ( cloud_labels, AREA_THRES, sorted, cluster_indices );
#ifdef VERBOSE2
  LabelSkel::giveSortedBlobsInfo ( sorted );
#endif
  //Build relationships between the blobs
  LabelSkel::buildRelations ( sorted );

  // ////////////////////////////////////////////////////////////////////////////////////////// //

  // DISPLAY INTERMEDIATE RESULTS UPTILL FINDING OF THE TREES, NOT EVALUATING TREES YET
#ifdef DISPL_INTERM
  // color
  TreeLive::colorLMap( lmap, cmap );

  // Publish this on a image topic
  IplImage cv_image_out_label = cmap;
  sensor_msgs::ImagePtr msg_label = sensor_msgs::CvBridge::cvToImgMsg(&cv_image_out_label, "bgr8");
  msg_label->header.frame_id = point_cloud->header.frame_id;
  g_pub_label_.publish(msg_label);

  // Display the blob aligned pointcloud
  pcl::PointCloud<pcl::PointXYZRGB> blob_cloud;
  // Make a new pointcloud with only the blob pixels in it
  LabelSkel::makeBlobPointCloud(sorted, cmap, cloud_in, blob_cloud);
  PointCloud2 blob;
  pcl::toROSMsg(blob_cloud, blob);
  blob.header.frame_id = point_cloud->header.frame_id;
  pub_pc_blob_.publish(blob);
#endif

  // ////////////////////////////////////////////////////////////////////////////////////////////// //
  // if we found a neck display the tree, and continue with processing
  if(sorted[10].size() != 0)
  {
  unsigned int c = 0;
#ifdef MULTIPLE_PERS
  for(; c < sorted[10].size(); c++)
  {
#endif

      LabelSkel::Tree2 t;
      LabelSkel::buildTree(sorted, cloud_in, Neck, c, t);

#ifdef VERBOSE2
      std::cout << c << " " << t << std::endl;
      for(u = 0; u < NUM_PARTS; u++)
      {
        if(t.parts_lid[u] != NO_CHILD)
          std::cout << " has part " << u << " with lid " << t.parts_lid[u] << std::endl;
      }
#endif

#ifdef DISPL_PRE_TREE
      // Display the tree pointcloud
      pcl::PointCloud<pcl::PointXYZRGB> tree_cloud;
      // Make a new pointcloud with only the blob pixels in it
      LabelSkel::makeTreePointCloud(sorted, 10, c, cloud_in, tree_cloud);
      PointCloud2 tree;
      pcl::toROSMsg(tree_cloud, tree);
      tree.header.frame_id = point_cloud->header.frame_id;
      pub_pc_.publish(tree);
#endif

      cv::Mat mask(cloud_in.height, cloud_in.width, CV_8UC1, cv::Scalar(0));

      LabelSkel::makeFGMaskFromPointCloud(mask, t.indices, cloud_in);

      pcl::PointIndices seed;
#ifdef DISPL_BINMASK
      cv::Mat binmask(cloud_in.height, cloud_in.width, CV_8UC3, cv::Scalar(0));
#endif
      for(v = 0; v < cloud_in.height; v++)
      {
        for(u = 0; u < cloud_in.width; u++)
        {
          if(mask.at<char>(v,u) == cv::GC_PR_FGD)
          {
#ifdef DISPL_BINMASK
            binmask.at<cv::Vec3b>(v,u)[0] = cloud_in.points[v*cloud_in.width + u].b;
            binmask.at<cv::Vec3b>(v,u)[1] = cloud_in.points[v*cloud_in.width + u].g;
            binmask.at<cv::Vec3b>(v,u)[2] = cloud_in.points[v*cloud_in.width + u].r;
#endif
            seed.indices.push_back(v*cloud_in.width + u);
          }
        }
      }
#ifdef DISPL_BINMASK
      // Publish this on a image topic
      IplImage cv_image_out_fg = binmask;
      sensor_msgs::ImagePtr msg_fg = sensor_msgs::CvBridge::cvToImgMsg(&cv_image_out_fg, "bgr8");
      msg_fg->header.frame_id = point_cloud->header.frame_id;
      g_pub_blob_.publish(msg_fg);
#endif

      // //////////////////////////////////////////////////////////////////////////////////////////////// //
      // The second kdtree evaluation = seeded hue segmentation

#ifdef VERBOSE
      std::cout << "(I) : before seededHueSegmentation(): found " << seed.indices.size() << " points"  << std::endl;
#endif
      // Reuse the fist searchtree for this, in order to NOT build it again!
      pcl::PointIndices flower;
      //pcl::seededHueSegmentation(cloud_in, stree, CLUST_TOL_SHS, seed, flower, DELTA_HUE_SHS);
      //optimized_shs(cloud_in, CLUST_TOL_SHS, seed, flower, DELTA_HUE_SHS);
      //optimized_shs2(cloud_in, CLUST_TOL_SHS, seed, flower, DELTA_HUE_SHS);
      {
          pcl::ScopeTime time("shs3");
      optimized_shs3(cloud_in, CLUST_TOL_SHS, seed, flower, DELTA_HUE_SHS);
      }

#ifdef VERBOSE
      std::cout << "(I) : after seededHueSegmentation(): found " << flower.indices.size() << " points"  << std::endl;
#endif
#ifdef DISPL_FLOWER
      pcl::PointCloud<pcl::PointXYZRGB> flower_out;
      for(u = 0; u < flower.indices.size(); u++)
        flower_out.points.push_back( cloud_in.points[flower.indices[u]] );
      flower_out.width = 1;
      flower_out.height = flower.indices.size();

      PointCloud2 flowerpc;
      pcl::toROSMsg(flower_out, flowerpc);
      flowerpc.header.frame_id = point_cloud->header.frame_id;
      pub_pc_flower_.publish(flowerpc);
#endif

      cv::Mat flowermat(cloud_in.height, cloud_in.width, CV_8UC3, cv::Scalar(0));
      LabelSkel::makeImageFromPointCloud(flowermat, flower, cloud_in);
      cv::Mat flowergrownmat(cloud_in.height, cloud_in.width, CV_8UC3, cv::Scalar(0));

      int erosion_size = 2;
      cv::Mat element = cv::getStructuringElement(cv::MORPH_RECT ,
                                                  cv::Size( 2*erosion_size + 1, 2*erosion_size+1 ),
                                                  cv::Point( erosion_size, erosion_size ) );

      cv::dilate(flowermat, flowergrownmat, element);

#ifdef DISPL_FLOWER_MAT
      IplImage cv_image_out_flower = flowergrownmat;
      sensor_msgs::ImagePtr msg_flower = sensor_msgs::CvBridge::cvToImgMsg(&cv_image_out_flower, "bgr8");
      msg_flower->header.frame_id = point_cloud->header.frame_id;
      g_pub_flower_.publish(msg_flower);
#endif

      cv::Mat dmat2(cloud_in.height, cloud_in.width, CV_16U);
      for(v = 0; v < cloud_in.height; v++)
      {
        for(u = 0; u < cloud_in.width; u++)
        {
          if(flowergrownmat.at<cv::Vec3b>(v,u)[0] != 0 || flowergrownmat.at<cv::Vec3b>(v,u)[1] != 0 || flowergrownmat.at<cv::Vec3b>(v,u)[2] != 0)
          {
            dmat2.at<short>(v,u) = dmat.at<short>(v,u);
          }
          else
          {
            dmat2.at<short>(v,u) = std::numeric_limits<short>::max();
          }
        }
      }

      // //////////////////////////////////////////////////////////////////////////////////////////////// //
      // The second label evaluation

      // Process the depthimage
      m_proc->process(dmat2, m_lmap);
      cv::Mat lmap2(cloud_in.height, cloud_in.width, CV_8UC1);
#ifndef GPU_SMOOTH_LABEL
      LabelSkel::smoothLabelImage(m_lmap, dmat2, lmap2);
#else
      m_lmap_dev.upload((void*)m_lmap.data, m_lmap.step, m_lmap.rows, m_lmap.cols);
      dmat_dev.upload((void*)dmat2.data, dmat2.step, dmat2.rows, dmat2.cols);
      smoothLabelImage(m_lmap_dev, dmat_dev, lmap_dev, NUM_PARTS);
      lmap_dev.download(lmap2.data, lmap2.step);
#endif
      // cv::medianBlur(m_lmap, lmap, 3);




      //cv::medianBlur(m_lmap, lmap2, 3);

#ifdef DISPL_LABEL2
      // Publish this on a image topic
      TreeLive::colorLMap( lmap2, cmap );
      IplImage cv_image_out_label2 = cmap;
      sensor_msgs::ImagePtr msg_label2 = sensor_msgs::CvBridge::cvToImgMsg(&cv_image_out_label2, "bgr8");
      msg_label2->header.frame_id = point_cloud->header.frame_id;
      g_pub_label2_.publish(msg_label2);
#endif

      pcl::PointCloud<pcl::PointXYZRGBL> cloud_labels2;
      pcl::colorLabelPointCloudfromArray(cloud_in, lmap2.data, cloud_labels2);

      std::vector<std::vector<pcl::PointIndices> > cluster_indices2;
      cluster_indices2.resize(NUM_PARTS);

      // avoid having the search tree to be build again
      //pcl::extractLabeledEuclideanClusters<pcl::PointXYZRGBL>(cloud_labels2, stree, CLUST_TOL, cluster_indices2, AREA_THRES2, MAX_CLUST_SIZE, NUM_PARTS);
      optimized_elec(cloud_in, lmap2, CLUST_TOL, cluster_indices2, AREA_THRES2, MAX_CLUST_SIZE, NUM_PARTS);

      std::vector<std::vector<LabelSkel::Blob2> >       sorted2;
      //clear out our matrix before starting again with it
      sorted2.clear();
      //Set fixed size of outer vector length = number of parts
      sorted2.resize(NUM_PARTS);
      //create the blob2 matrix
      LabelSkel::sortIndicesToBlob2 ( cloud_labels2, AREA_THRES2, sorted2, cluster_indices2 );
#ifdef VERBOSE2
      LabelSkel::giveSortedBlobsInfo ( sorted2 );
#endif
      LabelSkel::buildRelations ( sorted2 );

      // //////////////////////////////////////////////////////////////////////////////////////////////// //
      // Test if the second tree is build up correctly
      if(sorted2[10].size() != 0)
      {
        // Delete previous markers
        if(markerArray_tree.markers.size() != 0){
          for(u = 0; u < markerArray_tree.markers.size(); u++){
            markerArray_tree.markers[u].action = visualization_msgs::Marker::DELETE;
          }
          pub_tree_.publish(markerArray_tree);
          markerArray_tree.markers.clear();
        }
        // Draw the new labels
        LabelSkel::drawPrettyMarkerTree(sorted2, 10, 0, markerArray_tree );
      }
#ifdef MULTIPLE_PERS
  }
#endif
  pub_tree_.publish(markerArray_tree);
  }
  // ////////////////////////////////////////////////////////////////////////////////////////// //
}

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

  ros::init(argc, argv, NODE_NAME);
  ros::NodeHandle nh;

  image_transport::ImageTransport it_out_label(nh);
  g_pub_label_ = it_out_label.advertise("/ht_label", 1);
  image_transport::ImageTransport it_out_blob(nh);
  g_pub_blob_     = it_out_blob.advertise("/ht_blob", 1);
  image_transport::ImageTransport it_out_flower(nh);
  g_pub_flower_   = it_out_flower.advertise("/ht_flower", 1);
  image_transport::ImageTransport it_out_label2(nh);
  g_pub_label2_ = it_out_label2.advertise("/ht_label2", 1);

  pub_pc_         = nh.advertise<PointCloud2> ("/ht_labeled_pc", 1);
  pub_pc_blob_    = nh.advertise<PointCloud2> ("/ht_blob_pc", 1);
  pub_pc_flower_  = nh.advertise<PointCloud2> ("/ht_flower_pc", 1);

  pub_tree_       = nh.advertise<visualization_msgs::MarkerArray> ("/ht_tree_vis", 1);

  sub_pc_         = nh.subscribe("/camera/rgb/points", 1, dataCallback);

  ros::spin();

  return 0;
}
