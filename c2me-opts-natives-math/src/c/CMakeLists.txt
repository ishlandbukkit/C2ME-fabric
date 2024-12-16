cmake_minimum_required(VERSION 3.25)

set(CMAKE_TRY_COMPILE_TARGET_TYPE "STATIC_LIBRARY")

project(c2me-opts-natives-math C)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_COMPILER clang)

SET(CMAKE_C_FLAGS_RELWITHDEBINFO "-O3 -g")

set(CMAKE_C_STANDARD_LIBRARIES "")
set(CMAKE_CXX_STANDARD_LIBRARIES "")

add_library(c2me-opts-natives-math SHARED
        exports.c
        system_isa_x86_64.c
        exports_x86_64_nogather.c
        system_isa_aarch64.c
        flibc.c
)

if(CMAKE_SYSTEM_PROCESSOR MATCHES "(x86)|(X86)|(amd64)|(AMD64)")
    set_source_files_properties(exports_x86_64_nogather.c PROPERTIES COMPILE_FLAGS "-mno-gather -mno-scatter")
endif()

execute_process(COMMAND llvm-config --prefix OUTPUT_VARIABLE LLVM_PREFIX OUTPUT_STRIP_TRAILING_WHITESPACE)

target_include_directories(c2me-opts-natives-math PRIVATE includes/)
target_compile_options(c2me-opts-natives-math PRIVATE $<$<COMPILE_LANGUAGE:C>:-Wall -Wextra -Wpedantic -ffreestanding -ffile-prefix-map=${CMAKE_SOURCE_DIR}=. -fdebug-compilation-dir=. -fdebug-prefix-map=${CMAKE_SOURCE_DIR}=. -fdebug-prefix-map=${LLVM_PREFIX}=.../llvm-prefix -fno-math-errno -mprefer-vector-width=512 -ffp-contract=off -mno-stack-arg-probe -fsave-optimization-record "SHELL:-mllvm -extra-vectorizer-passes" "SHELL:-mllvm -slp-vectorize-hor-store" "SHELL:-mllvm -slp-min-tree-size=1" "SHELL:-mllvm -slp-min-reg-size=64" "SHELL:-mllvm -slp-threshold=-1" "SHELL:-mllvm -enable-epilogue-vectorization">)
target_link_options(c2me-opts-natives-math PRIVATE -v -nostdlib -fuse-ld=lld -ffile-prefix-map=${CMAKE_SOURCE_DIR}=. -fdebug-compilation-dir=. -fdebug-prefix-map=${CMAKE_SOURCE_DIR}=. -fdebug-prefix-map=${LLVM_PREFIX}=.../llvm-prefix)

