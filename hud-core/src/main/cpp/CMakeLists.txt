cmake_minimum_required(VERSION 3.22.1)
project("devhud")

add_library(
    devhud
    SHARED
    hud_jni.cpp
    proc_reader.cpp
    cpu_monitor.cpp
    mem_monitor.cpp
    net_monitor.cpp
)

target_include_directories(devhud PRIVATE include/)

find_library(log-lib log)

target_link_libraries(devhud ${log-lib})
