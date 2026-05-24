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

# Required for profilers (Android Studio, Simpleperf, Perfetto) to correctly
# unwind native call stacks. Without this, frame pointer is omitted as an
# optimization and stack traces appear truncated or incorrect.
target_compile_options(devhud PRIVATE -fno-omit-frame-pointer)

find_library(log-lib log)

target_link_libraries(devhud ${log-lib})
