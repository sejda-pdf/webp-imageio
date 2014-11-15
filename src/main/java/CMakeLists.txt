find_package ( Java REQUIRED )

set( JAR_NAME "webp-imageio.jar" )
set( JAVA_OUTPUT_PATH "${CMAKE_CURRENT_BINARY_DIR}" )
set( JAR_PATH "${JAVA_OUTPUT_PATH}/${JAR_NAME}" )
set( CLASSES_DIR "${JAVA_OUTPUT_PATH}/classes" )
set( SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/src" )
set( RESOURCES_DIR "${CMAKE_CURRENT_SOURCE_DIR}/resources" )

file(
  GLOB_RECURSE JAVA_SOURCE_FILES
  "${SOURCE_DIR}/*.java"
)

file(
  GLOB_RECURSE JAVA_ABS_RESOURCE_FILES
  "${RESOURCES_DIR}/*"
)

file(
  GLOB_RECURSE JAVA_REL_RESOURCE_FILES
  RELATIVE ${RESOURCES_DIR}
  "${RESOURCES_DIR}/*"
)

add_custom_command(
      OUTPUT ${CLASSES_DIR}
      COMMAND ${CMAKE_COMMAND} -E make_directory "${CLASSES_DIR}"
      COMMAND ${Java_JAVAC_EXECUTABLE}
          ${CMAKE_JAVA_COMPILE_FLAGS}
          -d ${CLASSES_DIR}
          -encoding utf-8
          -source 1.6
          -target 1.6
          -nowarn
          ${JAVA_SOURCE_FILES}
      COMMAND ${CMAKE_COMMAND} -E touch "${CLASSES_DIR}"
      DEPENDS ${JAVA_SOURCE_FILES}
      WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
      COMMENT "Building Java objects for ${JAR_NAME}.jar"
)

add_custom_command(
      # NOTE: this command generates an artificial dependency file
      OUTPUT ${JAR_PATH}
      COMMAND ${Java_JAR_EXECUTABLE}
          -cf ${JAR_PATH}
          -C ${RESOURCES_DIR} .
          -C ${CLASSES_DIR} .
      DEPENDS ${CLASSES_DIR} ${JAVA_ABS_RESOURCE_FILES}
      WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
      COMMENT "Creating Java archive ${JAR_NAME}"
)

add_custom_target(${JAR_NAME} ALL DEPENDS ${JAR_PATH})
