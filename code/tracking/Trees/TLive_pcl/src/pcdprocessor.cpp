/**
 * @brief This file is the execution node of the Human Tracking 
 * @copyright Copyright (2011) Willow Garage
 * @authors Cedric Cagniart, Koen Buys, Caroline Pantofaru
 **/

#include "includes.h"
#include "defines.h"

#include <iostream>
#include <pcl/io/pcd_io.h>
#include <pcl/point_types.h>

void optimized_elec(const pcl::PointCloud<pcl::PointXYZRGB> &cloud, const cv::Mat& src_labels, float tolerance,
                    std::vector<std::vector<pcl::PointIndices> > &labeled_clusters,
                    unsigned int min_pts_per_cluster, unsigned int max_pts_per_cluster, unsigned int num_parts,
                    bool brute_force_border = false, float radius_scale = 1.f);

void optimized_shs(const pcl::PointCloud<pcl::PointXYZRGB> &cloud, float tolerance, pcl::PointIndices &indices_in, pcl::PointIndices &indices_out, float delta_hue);
void optimized_shs2(const pcl::PointCloud<pcl::PointXYZRGB> &cloud, float tolerance, pcl::PointIndices &indices_in, pcl::PointIndices &indices_out, float delta_hue);

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

bool                                  first_cloud = true;
bool                                  cam_info_rec_;

visualization_msgs::MarkerArray       markerArray_tree; 

unsigned int u,v;
unsigned int counter = 0;

/**
 * @brief this is the roscallback routine, gets a Pointcloud in
 * Publishes all info
 **/
void dataCallback( const pcl::PointCloud<pcl::PointXYZRGB>::Ptr cloud)
{
  /// @todo rewrite this to a pointcloud::Ptr
  pcl::PointCloud<pcl::PointXYZRGB> cloud_in;
  pcl::PointCloud<pcl::PointXYZRGB> cloud_in_filt;

  cloud_in = *cloud;

  cv::Mat dmat(cloud_in.height, cloud_in.width, CV_16U);

  // Project pointcloud back into the imageplane
  LabelSkel::makeDepthImage16FromPointCloud(dmat, cloud_in);

  // Process the depthimage (CUDA)
  m_proc->process(dmat, m_lmap);

  cv::Mat lmap(cloud_in.height, cloud_in.width, CV_8UC1);
  LabelSkel::smoothLabelImage(m_lmap, dmat, lmap);

#ifdef WRITE
  stringstream ss;
  ss << "d_"  << counter << ".png";
  cv::imwrite(ss.str(), dmat);
  ss.str("");
  ss << "l_"  << counter << ".png";
  cv::imwrite(ss.str(), m_lmap);
  ss.str("");
  ss << "s_"  << counter << ".png";
  cv::imwrite(ss.str(), lmap);

  cv::Mat input(cloud_in.height, cloud_in.width, CV_8UC3);
  LabelSkel::makeImageFromPointCloud(input, cloud_in);

  ss.str("");
  ss << "i_"  << counter << ".png";
  cv::imwrite(ss.str(), input);
#endif

  pcl::PointCloud<pcl::PointXYZRGBL> cloud_labels;
  pcl::colorLabelPointCloudfromArray(cloud_in, lmap.data, cloud_labels);

  // Creating the Search object for the search method of the extraction
  pcl::search::OrganizedNeighbor<pcl::PointXYZRGBL>::Ptr stree (new pcl::search::OrganizedNeighbor<pcl::PointXYZRGBL>);
  stree->setInputCloud(cloud_labels.makeShared());

  std::vector<std::vector<pcl::PointIndices> > cluster_indices;
  cluster_indices.resize(NUM_PARTS);

  // Make all the clusters
  optimized_elec(cloud_in, lmap, CLUST_TOL, cluster_indices, AREA_THRES, MAX_CLUST_SIZE, NUM_PARTS);
  // Create a new struct to put the results in
  std::vector<std::vector<LabelSkel::Blob2> >       sorted;
  //clear out our matrix before starting again with it
  sorted.clear();
  //Set fixed size of outer vector length = number of parts
  sorted.resize(NUM_PARTS);
  //create the blob2 matrix
  LabelSkel::sortIndicesToBlob2 ( cloud_labels, AREA_THRES, sorted, cluster_indices );
  //Build relationships between the blobs
  LabelSkel::buildRelations ( sorted );

  // ////////////////////////////////////////////////////////////////////////////////////////// //

  // DISPLAY INTERMEDIATE RESULTS UPTILL FINDING OF THE TREES, NOT EVALUATING TREES YET
#if defined(WRITE)
  // color
  TreeLive::colorLMap( lmap, cmap );
  ss.str("");
  ss << "c_" << counter << ".png";
  cv::imwrite(ss.str(), cmap);
#endif

  // ////////////////////////////////////////////////////////////////////////////////////////////// //
  // if we found a neck display the tree, and continue with processing
  if(sorted[10].size() != 0)
  {
      unsigned int c = 0;
      LabelSkel::Tree2 t;
      LabelSkel::buildTree(sorted, cloud_in, Neck, c, t);

      cv::Mat mask(cloud_in.height, cloud_in.width, CV_8UC1, cv::Scalar(0));

      LabelSkel::makeFGMaskFromPointCloud(mask, t.indices, cloud_in);

      pcl::PointIndices seed;
#if defined(DISPL_BINMASK) || defined(WRITE)
      cv::Mat binmask(cloud_in.height, cloud_in.width, CV_8UC3, cv::Scalar(0));
#endif
      for(v = 0; v < cloud_in.height; v++)
      {
        for(u = 0; u < cloud_in.width; u++)
        {
          if(mask.at<char>(v,u) == cv::GC_PR_FGD)
          {
#if defined(DISPL_BINMASK) || defined(WRITE)
            binmask.at<cv::Vec3b>(v,u)[0] = cloud_in.points[v*cloud_in.width + u].b;
            binmask.at<cv::Vec3b>(v,u)[1] = cloud_in.points[v*cloud_in.width + u].g;
            binmask.at<cv::Vec3b>(v,u)[2] = cloud_in.points[v*cloud_in.width + u].r;
#endif
            seed.indices.push_back(v*cloud_in.width + u);
          }
        }
      }
#ifdef WRITE
      ss.str("");
      ss << "b_"<< counter << ".png";
      cv::imwrite(ss.str(), binmask);
#endif

      // //////////////////////////////////////////////////////////////////////////////////////////////// //
      // The second kdtree evaluation = seeded hue segmentation
      // Reuse the fist searchtree for this, in order to NOT build it again!
      pcl::PointIndices flower;
      //pcl::seededHueSegmentation(cloud_in, stree, CLUST_TOL_SHS, seed, flower, DELTA_HUE_SHS);
      optimized_shs2(cloud_in, CLUST_TOL_SHS, seed, flower, DELTA_HUE_SHS);

      cv::Mat flowermat(cloud_in.height, cloud_in.width, CV_8UC3, cv::Scalar(0));
      LabelSkel::makeImageFromPointCloud(flowermat, flower, cloud_in);

#ifdef WRITE
      ss.str("");
      ss << "f_" << counter << ".png";
      cv::imwrite(ss.str(), flowermat);
#endif
      cv::Mat flowergrownmat(cloud_in.height, cloud_in.width, CV_8UC3, cv::Scalar(0));

      int erosion_size = 2;
      cv::Mat element = cv::getStructuringElement(cv::MORPH_RECT ,
                                                  cv::Size( 2*erosion_size + 1, 2*erosion_size+1 ),
                                                  cv::Point( erosion_size, erosion_size ) );

      cv::dilate(flowermat, flowergrownmat, element);

#ifdef WRITE
      ss.str("");
      ss << "g_" << counter << ".png";
      cv::imwrite(ss.str(), flowergrownmat);
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
      LabelSkel::smoothLabelImage(m_lmap, dmat2, lmap2);
      //cv::medianBlur(m_lmap, lmap2, 3);

#ifdef WRITE
      ss.str("");
      ss << "d2_" << counter << ".png";
      cv::imwrite(ss.str(), dmat2);
      ss.str("");
      ss << "l2_" << counter << ".png";
      cv::imwrite(ss.str(), m_lmap);
      ss.str("");
      ss << "s2_" << counter << ".png";
      cv::imwrite(ss.str(), lmap2);
      // Publish this on a image topic
      TreeLive::colorLMap( lmap2, cmap );
      ss.str("");
      ss << "c2_"  << counter << ".png";
      cv::imwrite(ss.str(), cmap);
#endif

      pcl::PointCloud<pcl::PointXYZRGBL> cloud_labels2;
      pcl::colorLabelPointCloudfromArray(cloud_in, lmap2.data, cloud_labels2);

#ifdef WRITE
      pcl::io::savePCDFileASCII ("2de_it_colorLabeledPointCloud.pcd", cloud_labels2);
      std::cout << "Saved " << cloud_labels2.points.size () << " data points to 2de_it_colorLabeledPointCloud.pcd." << std::endl; 
#endif

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
      LabelSkel::buildRelations ( sorted2 );

      // //////////////////////////////////////////////////////////////////////////////////////////////// //
      // Test if the second tree is build up correctly
      if(sorted2[10].size() != 0)
      {
        LabelSkel::Tree2 t2;
        LabelSkel::buildTree(sorted, cloud_in, Neck, c, t2);
        int par = 0;
        for(int f=0;f<NUM_PARTS;f++)
        {
          if(t2.parts_lid[f] == NO_CHILD)
          {
            std::cerr << "1;";
            par++;
          }
          else
            std::cerr << "0;";
        }
        std::cerr<< t2.nr_parts << ";";
        std::cerr<< par << ";";
        std::cerr<< t2.total_dist_error << ";";
        std::cerr<< t2.norm_dist_error << ";";
        std::cerr<< counter << ";" << std::endl;
      }
      counter++;
  }
  // ////////////////////////////////////////////////////////////////////////////////////////// //
}

int main(int argc, char** argv)
{
  if(argc < 7)
  {
    std::cout << "Usage: : " << argv[0] << std::endl;
    std::cout << "\t-numTrees <number of trees to actually use" << std::endl;
    std::cout << "\t-tree0" << std::endl;
    std::cout << "\t-tree1" << std::endl;
    std::cout << "\t-tree2" << std::endl;
    std::cout << "\t-tree3" << std::endl;
    std::cout << "\t-pcd" << std::endl;
    return 0;
  }

  OptionParser opt(argc, argv);
  std::string treeFilenames[4];
  std::string topicname;
  std::string pcdname;
  int         numTrees  = opt.getOption<int>("-numTrees");
  treeFilenames[0]      = opt.getOption<std::string>("-tree0");
  treeFilenames[1]      = opt.getOption<std::string>("-tree1");
  treeFilenames[2]      = opt.getOption<std::string>("-tree2");
  treeFilenames[3]      = opt.getOption<std::string>("-tree3");
  topicname             = opt.getOption<std::string>("-topic");
  pcdname               = opt.getOption<std::string>("-pcd");

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

  pcl::PointCloud<pcl::PointXYZRGBA>::Ptr cloud (new pcl::PointCloud<pcl::PointXYZRGBA>);
  pcl::PointCloud<pcl::PointXYZRGB>::Ptr cloud2 (new pcl::PointCloud<pcl::PointXYZRGB>);

  if (pcl::io::loadPCDFile<pcl::PointXYZRGBA> (pcdname, *cloud) == -1) //* load the file
  {
    PCL_ERROR ("Couldn't read file %s \n", pcdname);
    return (-1);
  }
  std::cout << "Loaded "
            << cloud->width * cloud->height
            << " data points from " << pcdname
            << std::endl;

  for(size_t i = 0; i < cloud->points.size (); i++)
  {
    pcl::PointXYZRGB p;
    p.x = cloud->points[i].x;
    p.y = cloud->points[i].y;
    p.z = cloud->points[i].z;
    p.r = cloud->points[i].r;
    p.g = cloud->points[i].g;
    p.b = cloud->points[i].b;
    cloud2->points.push_back(p);
  }
  cloud2->width = cloud->width;
  cloud2->height = cloud->height;
  
  dataCallback(cloud2);

  return 0;
}
