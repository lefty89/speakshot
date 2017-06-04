# Sets the minimum version of CMake required to build the native
# library. You should either keep the default value or only pass a
# value of 3.4.0 or lower.

cmake_minimum_required(VERSION 3.4.1)

#################################################################################################

# gets log library
find_library(log-lib log)

# builds the test lib and adds the log
add_library(test-lib SHARED
    # the interface
    ${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpp/test/test-lib.cpp
)
target_link_libraries(test-lib ${log-lib})

#################################################################################################

# collect all source files within the hunspell folder
file(GLOB IMPL_SRCS
    "${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpp/hunspell/src/*.cxx"
)

# remove the file utf_info.cxx from the list because it is not required
list(REMOVE_ITEM IMPL_SRCS ${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpp/hunspell/src/utf_info.cxx)

# builds the hunspell lib and adds the log
add_library(hunspell-lib SHARED
    # the interface
    ${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpp/hunspell/hunspell-lib.cpp
    # source files from the list
    ${IMPL_SRCS}
)
target_link_libraries(hunspell-lib ${log-lib})

#################################################################################################