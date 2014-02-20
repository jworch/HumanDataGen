cmake_minimum_required(VERSION 2.4.6)
include($ENV{ROS_ROOT}/core/rosbuild/rosbuild.cmake)

rosbuild_init()

#SET(CMAKE_CXX_FLAGS “${CMAKE_CXX_FLAGS} -fopenmp”)
#SET(CMAKE_C_FLAGS “${CMAKE_C_FLAGS} -fopenmp”)

#set the default path for built executables to the "bin" directory
set(EXECUTABLE_OUTPUT_PATH ${PROJECT_SOURCE_DIR}/bin)
#set the default path for built libraries to the "lib" directory
set(LIBRARY_OUTPUT_PATH ${PROJECT_SOURCE_DIR}/lib)

set( SRC src/source.cpp
         src/auxFunctions.cpp )

rosbuild_add_executable(${PROJECT_NAME} ${SRC})
rosbuild_add_openmp_flags(${PROJECT_NAME})

