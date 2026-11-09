cmake_minimum_required(VERSION 3.22.1)

project("voiceflow")

# Whisper.cpp submodule — disable unnecessary build targets
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(GGML_METAL OFF CACHE BOOL "" FORCE)
set(GGML_CUDA OFF CACHE BOOL "" FORCE)

# Only add the whisper submodule if it has been populated
if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp/CMakeLists.txt")
    add_subdirectory(whisper.cpp)
    set(WHISPER_AVAILABLE TRUE)
else()
    message(WARNING "whisper.cpp submodule not found — building stub JNI only")
    set(WHISPER_AVAILABLE FALSE)
endif()

add_library(
    voiceflow-jni
    SHARED
    jni_bridge.cpp
)

target_include_directories(
    voiceflow-jni
    PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

if(WHISPER_AVAILABLE)
    target_include_directories(
        voiceflow-jni
        PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp
    )
    target_link_libraries(
        voiceflow-jni
        whisper
        android
        log
    )
else()
    # Stub build — links without whisper for CI/IDE indexing
    target_link_libraries(
        voiceflow-jni
        android
        log
    )
    target_compile_definitions(voiceflow-jni PRIVATE WHISPER_STUB=1)
endif()
