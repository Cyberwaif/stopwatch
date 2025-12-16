cmake_minimum_required(VERSION 3.4.1)
project(memstress C)

add_definitions(-D__ANDROID__ -Wno-writable-strings -Wno-missing-declarations -Wno-sizeof-array-argument)

set(CMAKE_VERBOSE_MAKEFILE ON)
set(EXECUTABLE_OUTPUT_PATH  "${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}")
set(LIBRARY_OUTPUT_PATH     "${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}")
set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}")
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}")
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}")

#./gradlew externalNativeBuildDebug

add_executable(memstress ../../../../native/memstress.c)
target_compile_options(memstress PRIVATE -O2 -fPIE)
target_link_options(memstress PRIVATE -pie)
