set(
  WEBP_HOME "${CMAKE_SOURCE_DIR}/libwebp"
  CACHE FILEPATH "libwebp source directory"
)

set(
  JNI_OBJS
  ${CMAKE_CURRENT_SOURCE_DIR}/webp-imageio.c
)

add_library( webp-imageio SHARED ${JNI_OBJS} )
include_directories( "${WEBP_HOME}/src" )
target_link_libraries( webp-imageio webp )

find_package ( JNI REQUIRED COMPONENTS Development )
include_directories( ${JNI_INCLUDE_DIRS} )

if( NOT MSVC )
  target_compile_options( webp-imageio PRIVATE
    -Wextra
    -Wold-style-definition
    -Wmissing-prototypes
    -Wmissing-declarations
    -Wdeclaration-after-statement
    -Wshadow
  )
endif()

if ( WIN32 )
  # Strip 'lib' prefix when compiling for windows
  set_target_properties ( webp-imageio PROPERTIES PREFIX "" )

  if( NOT MSVC )
    set_target_properties( webp-imageio PROPERTIES LINK_FLAGS "-Wl,--kill-at")
  endif()
endif()

if ( APPLE )
  target_compile_options( webp-imageio PRIVATE -mmacosx-version-min=10.5 )
  set_target_properties( webp-imageio PROPERTIES MACOSX_RPATH OFF )
endif()
