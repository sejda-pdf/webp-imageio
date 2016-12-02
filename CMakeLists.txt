cmake_minimum_required( VERSION 2.8.12 )
project ( webp-imageio )

set(CMAKE_POSITION_INDEPENDENT_CODE ON)

add_subdirectory( "libwebp" )
add_subdirectory( "src/main/c" )
