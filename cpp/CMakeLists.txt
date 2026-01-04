cmake_minimum_required(VERSION 4.0)
project(cpp)

set(CMAKE_CXX_STANDARD 20)

include_directories(.)
include_directories(pga3d)
include_directories(pga3dphysics)

add_compile_options(-O3)
add_compile_options(-march=native)
#add_compile_options(-Ofast)
#add_compile_options(-ffast-math -fno-math-errno)

add_executable(cpp
        main.cpp
        pga3d/Bivector.h
        pga3d/BivectorBulk.h
        pga3d/BivectorWeight.h
        pga3d/Motor.h
        pga3d/Multivector.h
        pga3d/Plane.h
        pga3d/PlaneIdeal.h
        pga3d/Point.h
        pga3d/PointCenter.h
        pga3d/ProjectivePoint.h
        pga3d/PseudoScalar.h
        pga3d/Quaternion.h
        pga3d/Translator.h
        pga3d/types.h
        pga3d/typesForward.h
        pga3d/Vector.h
        pga3dphysics/InertiaLocal.h
        pga3dphysics/InertiaMovedLocal.h
        pga3dphysics/PhysicsBody.h
        pga3dphysics/pga3dphysics.h
        pga3dphysics/PhysicsSolverRK4.h
        pga3dphysics/BodyState.h
        pga3dphysics/Forque.h
        pga3dphysics/InertiaSummable.h
        pga3dphysics/InertiaLocalSphere.h
        pga3dphysics/Friction.h
        pga3dphysics/Velocity.h
        pga3dphysics/AABB.h
        pga3dphysics/Edge.h
        pga3dphysics/Triangle.h
        pga3dphysics/Sphere.h
        pga3dphysics/Cylinder.h
        pga3dphysics/LinearOperator.h
        pga3dphysics/BodyPoint.h
        pga3dphysics/Spring.h
        pga3dphysics/Gravity.h
        pga3dphysics/Inertia.h
        pga3dphysics/Energy.h
        pga3dphysics/PhysicsBodyConnection.h
        pga3dphysics/PointMapping.h
)

# -----------------------------
# Tests (doctest + CTest)
# -----------------------------
include(CTest)

# Test executable with doctest main and a simple sample test
add_executable(cpp_tests
        test/test_main.cpp
        test/simple_arith_tests.cpp
        test/binary_ops_test.cpp
)

# Ensure the tests can find doctest.h which lives in test/
target_include_directories(cpp_tests PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/test)

# Register the test with CTest
add_test(NAME cpp_unit_tests COMMAND cpp_tests)
