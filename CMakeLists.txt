cmake_minimum_required( VERSION 2.8.12 )
set(CMAKE_OSX_DEPLOYMENT_TARGET "10.5" CACHE STRING "Minimum OS X deployment version")
project ( webp-imageio )

set(CMAKE_POSITION_INDEPENDENT_CODE ON)

add_subdirectory( "libwebp" )
add_subdirectory( "src/main/c" )
