cmake_minimum_required(VERSION 3.22.1)

project("bridge")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(
        bridge
        SHARED
        bridge.cpp
        bridge.h
        misc.cpp
)

find_library(
        log-lib
        log
)

find_library(
        android-lib
        android
)

find_library(
        jnigraphics-lib
        jnigraphics
)

target_link_libraries(
        bridge
        ${log-lib}
        ${android-lib}
        ${jnigraphics-lib}
)