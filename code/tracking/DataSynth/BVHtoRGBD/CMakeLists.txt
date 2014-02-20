cmake_minimum_required(VERSION 2.4.6)
include($ENV{ROS_ROOT}/core/rosbuild/rosbuild.cmake)

rosbuild_init()

#set the default path for built executables to the "bin" directory
set(EXECUTABLE_OUTPUT_PATH ${PROJECT_SOURCE_DIR}/bin)
#set the default path for built libraries to the "lib" directory
set(LIBRARY_OUTPUT_PATH ${PROJECT_SOURCE_DIR}/lib)

# the mesh/mesh app
set( SRC src/source.cpp 
         src/GLerrors.cpp 
         src/RGBDRenderer.cpp 
         src/OpenGLContext.cpp )

#common commands for building c++ executables and libraries
#rosbuild_add_library(${PROJECT_NAME} src/example.cpp)
#target_link_libraries(${PROJECT_NAME} another_library)
#rosbuild_add_boost_directories()
#rosbuild_link_boost(${PROJECT_NAME} thread)
#rosbuild_add_executable(example examples/example.cpp)
#target_link_libraries(example ${PROJECT_NAME})

# this was necessary cause /usr/lib/libGL.so links to the mesa implementation
LINK_DIRECTORIES( /usr/lib/nvidia-current) 
# necessary to access newer stuff
ADD_DEFINITIONS( -DGL_GLEXT_PROTOTYPES ) 

rosbuild_add_executable(${PROJECT_NAME} ${SRC})
target_link_libraries(${PROJECT_NAME} libBVH X11 m GL GLU glut)

