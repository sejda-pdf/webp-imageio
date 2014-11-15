set(
  WEBP_HOME "${CMAKE_SOURCE_DIR}/libwebp"
  CACHE FILEPATH "libwebp source directory"
)

set(
  DEC_OBJS
  ${WEBP_HOME}/src/dec/alpha.c
  ${WEBP_HOME}/src/dec/buffer.c
  ${WEBP_HOME}/src/dec/frame.c
  ${WEBP_HOME}/src/dec/idec.c
  ${WEBP_HOME}/src/dec/io.c
  ${WEBP_HOME}/src/dec/quant.c
  ${WEBP_HOME}/src/dec/tree.c
  ${WEBP_HOME}/src/dec/vp8.c
  ${WEBP_HOME}/src/dec/vp8l.c
  ${WEBP_HOME}/src/dec/webp.c
)

set(
  DEMUX_OBJS
  ${WEBP_HOME}/src/demux/demux.c
)

set(
  DSP_DEC_OBJS
  ${WEBP_HOME}/src/dsp/alpha_processing.c
  ${WEBP_HOME}/src/dsp/alpha_processing_sse2.c
  ${WEBP_HOME}/src/dsp/cpu.c
  ${WEBP_HOME}/src/dsp/dec.c
  ${WEBP_HOME}/src/dsp/dec_clip_tables.c
  ${WEBP_HOME}/src/dsp/dec_mips32.c
  ${WEBP_HOME}/src/dsp/dec_neon.c
  ${WEBP_HOME}/src/dsp/dec_sse2.c
  ${WEBP_HOME}/src/dsp/lossless.c
  ${WEBP_HOME}/src/dsp/lossless_mips32.c
  ${WEBP_HOME}/src/dsp/lossless_neon.c
  ${WEBP_HOME}/src/dsp/lossless_sse2.c
  ${WEBP_HOME}/src/dsp/upsampling.c
  ${WEBP_HOME}/src/dsp/upsampling_neon.c
  ${WEBP_HOME}/src/dsp/upsampling_sse2.c
  ${WEBP_HOME}/src/dsp/yuv.c
  ${WEBP_HOME}/src/dsp/yuv_mips32.c
  ${WEBP_HOME}/src/dsp/yuv_sse2.c
)

set(
  DSP_ENC_OBJS
  ${WEBP_HOME}/src/dsp/enc.c
  ${WEBP_HOME}/src/dsp/enc_avx2.c
  ${WEBP_HOME}/src/dsp/enc_mips32.c
  ${WEBP_HOME}/src/dsp/enc_neon.c
  ${WEBP_HOME}/src/dsp/enc_sse2.c
)

set(
  ENC_OBJS
  ${WEBP_HOME}/src/enc/alpha.c
  ${WEBP_HOME}/src/enc/analysis.c
  ${WEBP_HOME}/src/enc/backward_references.c
  ${WEBP_HOME}/src/enc/config.c
  ${WEBP_HOME}/src/enc/cost.c
  ${WEBP_HOME}/src/enc/filter.c
  ${WEBP_HOME}/src/enc/frame.c
  ${WEBP_HOME}/src/enc/histogram.c
  ${WEBP_HOME}/src/enc/iterator.c
  ${WEBP_HOME}/src/enc/picture.c
  ${WEBP_HOME}/src/enc/picture_csp.c
  ${WEBP_HOME}/src/enc/picture_psnr.c
  ${WEBP_HOME}/src/enc/picture_rescale.c
  ${WEBP_HOME}/src/enc/picture_tools.c
  ${WEBP_HOME}/src/enc/quant.c
  ${WEBP_HOME}/src/enc/syntax.c
  ${WEBP_HOME}/src/enc/token.c
  ${WEBP_HOME}/src/enc/tree.c
  ${WEBP_HOME}/src/enc/vp8l.c
  ${WEBP_HOME}/src/enc/webpenc.c
)

set(
  EX_FORMAT_DEC_OBJS
  ${WEBP_HOME}/examples/jpegdec.c
  ${WEBP_HOME}/examples/metadata.c
  ${WEBP_HOME}/examples/pngdec.c
  ${WEBP_HOME}/examples/tiffdec.c
  ${WEBP_HOME}/examples/webpdec.c
)

set(
  EX_UTIL_OBJS
  ${WEBP_HOME}/examples/example_util.c
)

set(
  MUX_OBJS
  ${WEBP_HOME}/src/mux/muxedit.c
  ${WEBP_HOME}/src/mux/muxinternal.c
  ${WEBP_HOME}/src/mux/muxread.c
)

set(
  UTILS_DEC_OBJS
  ${WEBP_HOME}/src/utils/bit_reader.c
  ${WEBP_HOME}/src/utils/color_cache.c
  ${WEBP_HOME}/src/utils/filters.c
  ${WEBP_HOME}/src/utils/huffman.c
  ${WEBP_HOME}/src/utils/quant_levels_dec.c
  ${WEBP_HOME}/src/utils/random.c
  ${WEBP_HOME}/src/utils/rescaler.c
  ${WEBP_HOME}/src/utils/thread.c
  ${WEBP_HOME}/src/utils/utils.c
)

set(
  UTILS_ENC_OBJS
  ${WEBP_HOME}/src/utils/bit_writer.c
  ${WEBP_HOME}/src/utils/huffman_encode.c
  ${WEBP_HOME}/src/utils/quant_levels.c
)

set(
  JNI_OBJS
  ${CMAKE_CURRENT_SOURCE_DIR}/webp-imageio.c
)

set(
  LIBWEBPDECODER_OBJS
  ${DEC_OBJS}
  ${DSP_DEC_OBJS}
  ${UTILS_DEC_OBJS}
)

set(
  LIBWEBP_OBJS
  ${LIBWEBPDECODER_OBJS}
  ${ENC_OBJS}
  ${DSP_ENC_OBJS}
  ${UTILS_ENC_OBJS}
)

add_library( webp-imageio SHARED ${LIBWEBP_OBJS} ${JNI_OBJS} )
include_directories( "${WEBP_HOME}/src" )

find_package ( JNI REQUIRED COMPONENTS Development )
include_directories( ${JNI_INCLUDE_DIRS} )

find_package ( Threads )
if ( CMAKE_USE_PTHREADS_INIT )
  target_compile_definitions( webp-imageio PRIVATE WEBP_USE_THREAD )
  target_link_libraries( webp-imageio "${CMAKE_THREAD_LIBS_INIT}" )
elseif ( CMAKE_USE_WIN32_THREADS_INIT )
  target_compile_definitions( webp-imageio PRIVATE WEBP_USE_THREAD )
endif()

target_link_libraries( webp-imageio "m" )

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
