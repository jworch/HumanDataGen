cmake_minimum_required(VERSION 2.4.6)
include($ENV{ROS_ROOT}/core/rosbuild/rosbuild.cmake)

rosbuild_init()

#set the default path for built executables to the "bin" directory
set(EXECUTABLE_OUTPUT_PATH ${PROJECT_SOURCE_DIR}/bin)
#set the default path for built libraries to the "lib" directory
set(LIBRARY_OUTPUT_PATH ${PROJECT_SOURCE_DIR}/lib)

#following two lines added for test
set( SRC src/libBVH.cpp 
         src/BVHTransforms.cpp
         src/BVHMesh.cpp
         src/BVHMesh_smooth.cpp
         src/BVHMotionFile.cpp )

SET( CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fPIC -O3 -march=athlon64 -m3dnow -mmmx -msse -msse2 -mfpmath=sse  -Wextra")

rosbuild_add_library(${PROJECT_NAME} ${SRC})
