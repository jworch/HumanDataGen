cmake_minimum_required(VERSION 2.4.6)

set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} "${CMAKE_SOURCE_DIR}/cmake/")


find_package(PCL REQUIRED)
include_directories(${PCL_INCLUDE_DIRS})
find_package(Eigen REQUIRED)

include_directories(libBVH/include BVHDBSparsify/include BVHtoRGBD/include ${EIGEN_INCLUDE_DIRS})


include($ENV{ROS_ROOT}/core/rosbuild/rosbuild.cmake)

# Set the build type.  Options are:
#  Coverage       : w/ debug symbols, w/o optimization, w/ code-coverage
#  Debug          : w/ debug symbols, w/o optimization
#  Release        : w/o debug symbols, w/ optimization
#  RelWithDebInfo : w/ debug symbols, w/ optimization
#  MinSizeRel     : w/o debug symbols, w/ optimization, stripped binaries
set(ROS_BUILD_TYPE Debug)

rosbuild_init()

#set the default path for built executables to the "bin" directory
set(EXECUTABLE_OUTPUT_PATH ${PROJECT_SOURCE_DIR}/bin)
#set the default path for built libraries to the "lib" directory
set(LIBRARY_OUTPUT_PATH ${PROJECT_SOURCE_DIR}/lib)

#uncomment if you have defined messages
#rosbuild_genmsg()
#uncomment if you have defined services
#rosbuild_gensrv()

set(SRC src/segment.cpp
        src/conversion.cpp
        src/display.cpp
        src/tree.cpp)

#common commands for building c++ executables and libraries
rosbuild_add_library(LabelSkel ${SRC})
target_link_libraries(LabelSkel ${PCL_LIBRARIES})
#rosbuild_add_compile_flags(LabelSkel -g -O0)
#target_link_libraries(${PROJECT_NAME} another_library)
#rosbuild_add_boost_directories()
#rosbuild_link_boost(${PROJECT_NAME} thread)
#rosbuild_add_executable(example examples/example.cpp)
#target_link_libraries(example ${PROJECT_NAME})
