cmake_minimum_required(VERSION 2.4.6)
include($ENV{ROS_ROOT}/core/rosbuild/rosbuild.cmake)

# Set the build type.  Options are:
#  Coverage       : w/ debug symbols, w/o optimization, w/ code-coverage
#  Debug          : w/ debug symbols, w/o optimization
#  Release        : w/o debug symbols, w/ optimization
#  RelWithDebInfo : w/ debug symbols, w/ optimization
#  MinSizeRel     : w/o debug symbols, w/ optimization, stripped binaries
set(ROS_BUILD_TYPE Release)

rosbuild_init()

#set the default path for built executables to the "bin" directory
set(EXECUTABLE_OUTPUT_PATH ${PROJECT_SOURCE_DIR}/bin)
#set the default path for built libraries to the "lib" directory
set(LIBRARY_OUTPUT_PATH ${PROJECT_SOURCE_DIR}/lib)

#uncomment if you have defined messages
#rosbuild_genmsg()
#uncomment if you have defined services
#rosbuild_gensrv()


FIND_PACKAGE( CUDA REQUIRED )

#############################
# The G++ sources
FILE(GLOB SRC_MULTI src/openni_grabber.cpp)

INCLUDE_DIRECTORIES( include 
                     /usr/include/openni
                      ${libTreeLive_SOURCE_DIR}/include 
                      ${CUDA_INCLUDE_DIRS})

SET(CMAKE_CXX_FLAGS ${CMAKE_CXX_FLAGS} -fopenmp)
SET(CMAKE_C_FLAGS ${CMAKE_C_FLAGS} -fopenmp)


#LINK_LIBRARIES( profiler
#                opencv_highgui 
#                OpenNI 
#                TreeLive 
#                ${CUDA_CUDART_LIBRARY} )

LINK_LIBRARIES( opencv_highgui 
                OpenNI 
                TreeLive 
                ${CUDA_CUDART_LIBRARY} )

#common commands for building c++ executables and libraries
#rosbuild_add_library(${PROJECT_NAME} src/example.cpp)
#target_link_libraries(${PROJECT_NAME} another_library)
#rosbuild_add_boost_directories()
#rosbuild_link_boost(${PROJECT_NAME} thread)
rosbuild_add_executable(${PROJECT_NAME} ${SRC_MULTI})
#target_link_libraries(example ${PROJECT_NAME})
