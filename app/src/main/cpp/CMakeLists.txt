cmake_minimum_required(VERSION 3.18.1)

project(rlottie_jni)

set(RLOTTIE_DIR ${CMAKE_CURRENT_LIST_DIR}/../../../../third_party/rlottie)

set(LOTTIE_MODULE OFF)
set(LOTTIE_THREAD ON)
set(LOTTIE_CACHE ON)
set(LOTTIE_MODULE_PATH "rlottie-image-loader")

configure_file(${RLOTTIE_DIR}/cmake/config.h.in ${CMAKE_CURRENT_BINARY_DIR}/config.h)

add_library(rlottie STATIC)
target_include_directories(rlottie
    PUBLIC ${RLOTTIE_DIR}/inc
    PRIVATE ${CMAKE_CURRENT_BINARY_DIR}
)
target_compile_features(rlottie PRIVATE cxx_std_14)

if(ANDROID_ABI STREQUAL "armeabi-v7a")
    set(ARCH "arm")
endif()

add_subdirectory(${RLOTTIE_DIR}/src/vector ${CMAKE_CURRENT_BINARY_DIR}/vector)
add_subdirectory(${RLOTTIE_DIR}/src/lottie ${CMAKE_CURRENT_BINARY_DIR}/lottie)
add_subdirectory(${RLOTTIE_DIR}/src/binding ${CMAKE_CURRENT_BINARY_DIR}/binding)

add_library(rlottie_jni SHARED
    native-rlottie.cpp
)

target_include_directories(rlottie_jni
    PRIVATE ${RLOTTIE_DIR}/inc
)

find_library(log-lib log)
find_library(jnigraphics-lib jnigraphics)

target_link_libraries(rlottie_jni
    rlottie
    ${log-lib}
    ${jnigraphics-lib}
)
