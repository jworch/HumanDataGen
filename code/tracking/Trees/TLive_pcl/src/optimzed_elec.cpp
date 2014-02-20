#include <opencv2/core/core.hpp>

#include <vector>
#include <pcl/point_cloud.h>
#include <pcl/point_types.h>
#include <pcl/PointIndices.h>
#include <pcl/search/pcl_search.h>
#include <pcl/common/time.h>

//#include <pcl/point_types_conversion.h> // can't include because not inline function causes multiple definition errors
namespace pcl
{
   void PointXYZRGBtoXYZHSV (PointXYZRGB& in, PointXYZHSV& out);
}

#include <iostream>
#include <algorithm>
#include <numeric>

using namespace std;
using namespace pcl;
using namespace cv;

namespace
{
    class SearchD : public pcl::search::OrganizedNeighbor<pcl::PointXYZRGB>
    {
    public:
        typedef  pcl::search::OrganizedNeighbor<pcl::PointXYZRGB> Base;

        using Base::getProjectedRadiusSearchBox;
        /** \brief the projection matrix. Either set by user or calculated by the first / each input cloud */
        using Base::projection_matrix_;
        /** \brief inveser of the left 3x3 projection matrix which is K * R (with K being the camera matrix and R the rotation matrix)*/
        using Base::KR_;
        /** \brief inveser of the left 3x3 projection matrix which is K * R (with K being the camera matrix and R the rotation matrix)*/
        using Base::KR_KRT_;    
    };


    template<typename PointT1, typename PointT2>
    double sqnorm(const PointT1& p1, const PointT2& p2)
    {
        return (p1.getVector3fMap () - p2.getVector3fMap ()).squaredNorm ();
    }

    template<typename It, typename Val>
    void yota(It beg, It end, Val seed)
    {
        for(It pos = beg; pos < end;)
            *pos++ = seed++;
    }
}

void optimized_elec(const PointCloud<pcl::PointXYZRGB> &cloud, const cv::Mat& src_labels, float tolerance,
                    std::vector<std::vector<PointIndices> > &labeled_clusters,
                    unsigned int min_pts_per_cluster, unsigned int max_pts_per_cluster, unsigned int num_parts,
                    bool brute_force_border, float radius_scale)
{
    typedef unsigned char uchar;

    Size sz = src_labels.size();

    cv::Mat dst_labels(sz, CV_32S, Scalar(-1));
    cv::Mat wavefront(sz, CV_32SC2);
    cv::Mat region_sizes = Mat::zeros(sz, CV_32S);

    Point2i *wf = wavefront.ptr<Point2i>();
    int *rsizes = region_sizes.ptr<int>();

    int cc = -1;

    double squared_radius = tolerance * tolerance;

    for(int j = 0; j < sz.height; ++j)
    {
        for(int i = 0; i < sz.width; ++i)
        {
            if (src_labels.at<uchar>(j, i) >= num_parts || dst_labels.at<int>(j, i) != -1) // invalid label && has been labeled
                continue;

            Point2i* ws = wf; // initialize wavefront
            Point2i p(i, j);  // current pixel

            cc++;	// next label

            dst_labels.at<int>(j, i) = cc;
            int count = 0;	// current region size

            // wavefront propagation
            while( ws >= wf ) // wavefront not empty
            {
                // put neighbors onto wavefront
                const uchar* sl = &src_labels.at<uchar>(p.y, p.x);
                int*         dl = &dst_labels.at<  int>(p.y, p.x);
                const pcl::PointXYZRGB *sp = &cloud.at(p.x, p.y);

                //right
                if( p.x < sz.width-1 && dl[+1] == -1 && sl[+1] == sl[0])
                    if (sqnorm(sp[0], sp[+1]) <= squared_radius)
                    {
                        dl[+1] = cc;
                        *ws++ = Point2i(p.x+1, p.y);
                    }

                //left
                if( p.x > 0 && dl[-1] == -1 && sl[-1] == sl[0])
                    if (sqnorm(sp[0], sp[-1]) <= squared_radius)
                    {
                        dl[-1] = cc;
                        *ws++ = Point2i(p.x-1, p.y);
                    }

                //top
                if( p.y < sz.height-1 && dl[+sz.width] == -1 && sl[+sz.width] == sl[0])
                    if (sqnorm(sp[0], sp[+sz.width]) <= squared_radius)
                    {
                        dl[+sz.width] = cc;
                        *ws++ = Point2i(p.x, p.y+1);
                    }


                //top
                if( p.y > 0 && dl[-sz.width] == -1 && sl[-sz.width] == sl[0])
                    if (sqnorm(sp[0], sp[-sz.width]) <= squared_radius)
                    {
                        dl[-sz.width] = cc;
                        *ws++ = Point2i(p.x, p.y-1);
                    }

                // pop most recent and propagate
                p = *--ws;
                count++;
            }

            rsizes[cc] = count;
        } /* for(int i = 0; i < sz.width; ++i) */
    } /* for(int j = 0; j < sz.height; ++j) */

    Mat djset(sz, CV_32S);
    int* dj = djset.ptr<int>();
    yota(dj, dj + sz.area(), 0);


    if (brute_force_border)
    {
        pcl::ScopeTime time("border");

        SearchD search;
        search.setInputCloud(cloud.makeShared());

#define HT_USE_OMP

#ifdef HT_USE_OMP

        const int threads_num = 3;

        int range[threads_num + 1];
        for(int i = 0; i < threads_num+1; ++i)
            range[i] = i * num_parts/threads_num;

        #pragma omp parallel for num_threads(threads_num)
        for(int r = 0; r < threads_num; ++r)
        {
#endif
            for(int j = 1; j < sz.height-1; ++j)
            {
                int *dl = dst_labels.ptr<int>(j);

                for(int i = 1; i < sz.width-1; ++i)
                {
                    int cc = dl[i];

                    if (cc == -1)
                        continue;

#ifdef HT_USE_OMP
                    uchar lbl = src_labels.at<uchar>(j, i);
                    bool inRange =range[r] <= lbl && lbl < range[r+1];
                    if (!inRange)
                        continue;
#endif

                    if (cc == dl[i+1] && cc == dl[i-1] && cc == dl[i+sz.width] && cc == dl[i-sz.width])
                        continue; // inner point

                    int root = cc;
                    while(root != dj[root])
                        root = dj[root];

                    const pcl::PointXYZRGB& sp = cloud.at(i, j);
                    uchar sl = src_labels.at<uchar>(j, i);

                    if (!isFinite(sp))
                        continue;

                    unsigned left, right, top, bottom;
                    search.getProjectedRadiusSearchBox (sp, squared_radius, left, right, top, bottom);

                    if (radius_scale != 1.f)
                    {
                        left =  i - (i - left) * radius_scale;
                        right = i + (right - i) * radius_scale;
                        top =  j - (j - top) * radius_scale;
                        bottom = j + (bottom - j) * radius_scale;
                    }

                    for(int y = top; y < bottom + 1; ++y)
                    {
                        const uchar *sl1  = src_labels.ptr<uchar>(y);
                        const int   *dl1 = dst_labels.ptr<int>(y);
                        const pcl::PointXYZRGB* sp1 = &cloud.at(0, y);

                        for(int x = left; x < right + 1; ++x)
                        {
                            int cc1 = dl1[x];
                            // not the same cc, the same label, and in radius
                            if (cc1 != cc && sl1[x] == sl && sqnorm(sp, sp1[x]) <= squared_radius)
                            {
                                while(cc1 != dj[cc1])
                                    cc1 = dj[cc1];

                                dj[cc1] = root;
                            }
                        }
                    }
                }
            }
#ifdef HT_USE_OMP
        }
#endif

        for(int j = 0; j < sz.height; ++j)
        {
            int *dl = dst_labels.ptr<int>(j);

            for(int i = 0; i < sz.width; ++i)
            {
                int cc = dl[i];
                if (cc == -1)
                    continue;

                while(cc != dj[cc]) //disjoint set
                    cc = dj[cc];
                dl[i] = cc;
                rsizes[cc]++;
            }
        }

    } /* if (brute_force_border)*/

    //convert to output format
    labeled_clusters.clear();
    labeled_clusters.resize(num_parts);

    vector<int> remap(sz.area(), -1);

    for(int j = 0; j < sz.height; ++j)
    {
        const uchar *sl = src_labels.ptr<uchar>(j);
        const int   *dl = dst_labels.ptr<int>(j);

        for(int i = 0; i < sz.width; ++i)
        {
            int part = sl[i];
            int cc   = dl[i];

            if (cc == -1)
                continue;

            if (min_pts_per_cluster <= rsizes[cc] && rsizes[cc] <= max_pts_per_cluster)
            {

                int ccindex = remap[cc];
                if (ccindex == -1)
                {
                    ccindex = (int)labeled_clusters[part].size();
                    labeled_clusters[part].resize(ccindex + 1);
                    remap[cc] = ccindex;
                }

                labeled_clusters[part][ccindex].indices.push_back(j*sz.width + i);
            }
        }
    }
}

//////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////

void optimized_shs(const PointCloud<PointXYZRGB> &cloud, float tolerance, PointIndices &indices_in, PointIndices &indices_out, float delta_hue)
{
    // Create a bool vector of processed point indices, and initialize it to false
    std::vector<char> processed (cloud.points.size (), 0);

    double squared_radius = tolerance * tolerance;

    // Process all points in the indices vector
    for (size_t k = 0; k < indices_in.indices.size (); ++k)
    {
        int i = indices_in.indices[k];
        if (processed[i])
            continue;

        processed[i] = 1;

        std::vector<int> seed_queue;
        int sq_idx = 0;
        seed_queue.push_back (i);

        PointXYZRGB  p;
        p = cloud.points[i];
        PointXYZHSV h;
        PointXYZRGBtoXYZHSV(p, h);

        while (sq_idx < (int)seed_queue.size ())
        {
            int index = seed_queue[sq_idx];
            int ycoo = index / cloud.width;
            int xcoo = index - cloud.width * ycoo;

            const int winsz = 20;
            const int winsz2 = winsz/2;

            int ybeg = max(0, ycoo - winsz2);
            int yend = min((int)cloud.height-1, ycoo + winsz2) + 1;
            int xend = min((int)cloud.width-1, xcoo + winsz2) + 1;

            for(int y = ybeg; y < yend; ++y)
                for(int x = max(0, xcoo - winsz2); x < xend; ++x)
                {
                    int idx = y * cloud.width + x;
                    if (!processed[idx] && (sqnorm(h, cloud.at(x,y)) < squared_radius))
                    {
                        PointXYZHSV h_l;
                        PointXYZRGB p_l = cloud.at(x, y);
                        PointXYZRGBtoXYZHSV(p_l, h_l);

                        if (fabs(h_l.h - h.h) < delta_hue)
                        {
                            seed_queue.push_back (idx);
                            processed[idx] = 1;
                        }
                    }
                }

            sq_idx++;
        }
        // Copy the seed queue into the output indices
        for (size_t l = 0; l < seed_queue.size (); ++l)
            indices_out.indices.push_back(seed_queue[l]);
    }
    // This is purely esthetical, can be removed for speed purposes
    std::sort (indices_out.indices.begin (), indices_out.indices.end ());
}


void optimized_shs2(const PointCloud<PointXYZRGB> &cloud, float tolerance, PointIndices &indices_in, PointIndices &indices_out, float delta_hue)
{
    // Create a bool vector of processed point indices, and initialize it to false
    std::vector<char> processed (cloud.points.size (), 0);

    std::vector<int> nn_indices;
    std::vector<float> nn_distances;

    SearchD search;
    search.setInputCloud(cloud.makeShared());

    // Process all points in the indices vector
    for (size_t k = 0; k < indices_in.indices.size (); ++k)
    {
        int i = indices_in.indices[k];
        if (processed[i])
            continue;

        processed[i] = 1;

        std::vector<int> seed_queue;
        int sq_idx = 0;
        seed_queue.push_back (i);

        PointXYZRGB  p;
        p = cloud.points[i];
        PointXYZHSV h;
        PointXYZRGBtoXYZHSV(p, h);

        while (sq_idx < (int)seed_queue.size ())
        {
            int index = seed_queue[sq_idx];
            const PointXYZRGB& q = cloud.points[index];

            if(!isFinite (q))
                continue;

            // search window
            unsigned left, right, top, bottom;
            double squared_radius = tolerance * tolerance;
            search.getProjectedRadiusSearchBox (q, squared_radius, left, right, top, bottom);

            unsigned yEnd  = (bottom + 1) * cloud.width + right + 1;
            unsigned idx  = top * cloud.width + left;
            unsigned skip = cloud.width - right + left - 1;
            unsigned xEnd = idx - left + right + 1;

            for (; xEnd != yEnd; idx += skip, xEnd += cloud.width)
            {
                for (; idx < xEnd; ++idx)
                {
                    if (processed[idx])
                        continue;

                    if (sqnorm(cloud.points[idx], q) <= squared_radius)
                    {
                        PointXYZRGB  p_l;
                        p_l = cloud.points[idx];
                        PointXYZHSV h_l;
                        PointXYZRGBtoXYZHSV(p_l, h_l);

                        if (fabs(h_l.h - h.h) < delta_hue)
                        {
                            if(idx & 1)
                              seed_queue.push_back (idx);
                            processed[idx] = 1;
                        }

                    }
                }
            }
            sq_idx++;

        }
        // Copy the seed queue into the output indices
        for (size_t l = 0; l < seed_queue.size (); ++l)
            indices_out.indices.push_back(seed_queue[l]);
    }
    // This is purely esthetical, can be removed for speed purposes
    std::sort (indices_out.indices.begin (), indices_out.indices.end ());
}



void optimized_shs3(const PointCloud<PointXYZRGB> &cloud, float tolerance, PointIndices &indices_in, PointIndices &indices_out, float delta_hue)
{
    cv::Mat huebuf(cloud.height, cloud.width, CV_32F);
    float *hue = huebuf.ptr<float>();

    for(size_t i = 0; i < cloud.points.size(); ++i)
    {
        PointXYZHSV h;
        PointXYZRGB p = cloud.points[i];
        PointXYZRGBtoXYZHSV(p, h);
        hue[i] = h.h;
    }

    // Create a bool vector of processed point indices, and initialize it to false
    std::vector<char> processed (cloud.points.size (), 0);

    SearchD search;
    search.setInputCloud(cloud.makeShared());

    vector< vector<int> > storage(100);

    // Process all points in the indices vector
#pragma omp parallel for
    for (size_t k = 0; k < indices_in.indices.size (); ++k)
    {
        int i = indices_in.indices[k];
        if (processed[i])
            continue;

        processed[i] = 1;

        std::vector<int> seed_queue;
        seed_queue.reserve(cloud.size());
        int sq_idx = 0;
        seed_queue.push_back (i);

        PointXYZRGB p = cloud.points[i];
        float h = hue[i];

        while (sq_idx < (int)seed_queue.size ())
        {
            int index = seed_queue[sq_idx];
            const PointXYZRGB& q = cloud.points[index];

            if(!isFinite (q))
                continue;

            // search window
            unsigned left, right, top, bottom;
            double squared_radius = tolerance * tolerance;
            search.getProjectedRadiusSearchBox (q, squared_radius, left, right, top, bottom);

            unsigned yEnd  = (bottom + 1) * cloud.width + right + 1;
            unsigned idx  = top * cloud.width + left;
            unsigned skip = cloud.width - right + left - 1;
            unsigned xEnd = idx - left + right + 1;

            for (; xEnd != yEnd; idx += skip, xEnd += cloud.width)
            {
                for (; idx < xEnd; ++idx)
                {
                    if (processed[idx])
                        continue;

                    if (sqnorm(cloud.points[idx], q) <= squared_radius)
                    {
                        float h_l = hue[idx];

                        if (fabs(h_l - h) < delta_hue)
                        {
                            if(idx & 1)
                              seed_queue.push_back (idx);
                            processed[idx] = 1;
                        }

                    }
                }
            }
            sq_idx++;

        }
        // Copy the seed queue into the output indices
        int id = omp_get_thread_num();
        storage[id].insert(storage[id].end(), seed_queue.begin(), seed_queue.end());
    }

    indices_out.indices.clear();
    for(size_t i = 0; i < storage.size(); ++i)
        indices_out.indices.insert(indices_out.indices.begin(), storage[i].begin(), storage[i].end());

    std::sort (indices_out.indices.begin (), indices_out.indices.end ());
    indices_out.indices.erase(std::unique(indices_out.indices.begin (), indices_out.indices.end ()), indices_out.indices.end());
}
