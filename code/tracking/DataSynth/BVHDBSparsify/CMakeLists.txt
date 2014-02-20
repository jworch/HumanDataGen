cmake_minimum_required(VERSION 2.4.6)
include($ENV{ROS_ROOT}/core/rosbuild/rosbuild.cmake)

rosbuild_init()

#set the default path for built executables to the "bin" directory
set(EXECUTABLE_OUTPUT_PATH ${PROJECT_SOURCE_DIR}/bin)
#set the default path for built libraries to the "lib" directory
set(LIBRARY_OUTPUT_PATH ${PROJECT_SOURCE_DIR}/lib)

# the mesh/mesh app
set( SRC src/source.cpp )

rosbuild_add_executable(${PROJECT_NAME} ${SRC})
target_link_libraries(${PROJECT_NAME} libBVH)

rosbuild_add_executable(${PROJECT_NAME}_omp src/source_openmp.cpp)
rosbuild_add_openmp_flags(${PROJECT_NAME}_omp)
target_link_libraries(${PROJECT_NAME}_omp libBVH)
