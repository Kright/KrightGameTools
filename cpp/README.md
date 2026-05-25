### Code structure

I use C++ 20 because of Unreal Engine 5 requirements.
Library is header-only, no external dependencies.
License is MIT.

* [**pga3d:**](pga3d) same as pga3d for Scala for geometric algebra. It contains generated code for multivector and its subclasses (planes, lines, points, quaternions, etc.).
* [**pga3dphysics:**](pga3dphysics) the code on top of pga3d for physics (inertia, force, acceleration, friction, etc.)
* **fused** all the code, fused into one header (pga3d for math only and pga3dphysics with both physics and math)
* [**test:**](test) tests with doctest

Implementation should work in the same way as in Scala and have the same methods, but may have some differences because of different language features.

Example code is in [main.cpp](test/main.cpp)

## How to use

Just include the header from `fused` directory.

### CMake example

```cmake
cmake_minimum_required(VERSION 3.20)
project(MyPhysicsProject)

set(CMAKE_CXX_STANDARD 20)

include_directories(${CMAKE_CURRENT_SOURCE_DIR}/path/to/gametools/cpp/fused)

add_executable(MyPhysicsProject main.cpp)
```
