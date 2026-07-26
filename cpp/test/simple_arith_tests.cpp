#include "doctest.h"
#include "pga3d/pga3d.h"

TEST_CASE("convert to multivector and back") {
    std::array<double, 16> arr{};
    for (int i = 0; i < 16; ++i) {
        arr[i] = i;
    }

    const pga3d::Multivector mv = pga3d::Multivector::from(arr);
    CHECK(mv.toArray() == arr);

    CHECK(mv.toMotorUnsafe().toMultivector().toMotorUnsafe() == mv.toMotorUnsafe());
    CHECK(mv.toPlaneUnsafe().toMultivector().toPlaneUnsafe() == mv.toPlaneUnsafe());
    CHECK(mv.toBivectorUnsafe().toMultivector().toBivectorUnsafe() == mv.toBivectorUnsafe());
    CHECK(mv.toProjectivePointUnsafe().toMultivector().toProjectivePointUnsafe() == mv.toProjectivePointUnsafe());
    CHECK(mv.toRotorUnsafe().toMultivector().toRotorUnsafe() == mv.toRotorUnsafe());
    CHECK(mv.toProjectiveTranslatorUnsafe().toMultivector().toProjectiveTranslatorUnsafe() == mv.toProjectiveTranslatorUnsafe());
    CHECK(mv.toTranslatorUnsafe().toMultivector().toTranslatorUnsafe() == mv.toTranslatorUnsafe());
    CHECK(mv.toVectorUnsafe().toMultivector().toVectorUnsafe() == mv.toVectorUnsafe());
    CHECK(mv.toPointUnsafe().toMultivector().toPointUnsafe() == mv.toPointUnsafe());
    CHECK(mv.toPlaneCentralUnsafe().toMultivector().toPlaneCentralUnsafe() == mv.toPlaneCentralUnsafe());
    CHECK(mv.toBivectorBulkUnsafe().toMultivector().toBivectorBulkUnsafe() == mv.toBivectorBulkUnsafe());
    CHECK(mv.toBivectorWeightUnsafe().toMultivector().toBivectorWeightUnsafe() == mv.toBivectorWeightUnsafe());
}


TEST_CASE("cross product right- and left-handed") {
    constexpr pga3d::Vector x{.x = 1.0};
    constexpr pga3d::Vector y{.y = 1.0};
    constexpr pga3d::Vector z{.z = 1.0};

    static_assert(pga3d::Vector::crossRightHanded(x, y) == z);
    static_assert(pga3d::Vector::crossRightHanded(y, z) == x);
    static_assert(pga3d::Vector::crossRightHanded(z, x) == y);

    static_assert(pga3d::Vector::crossLeftHanded(x, y) == -z);
    static_assert(pga3d::Vector::crossLeftHanded(y, z) == -x);
    static_assert(pga3d::Vector::crossLeftHanded(z, x) == -y);

    const pga3d::Vector a{.x = 0.5, .y = -1.25, .z = 2.0};
    const pga3d::Vector b{.x = -3.0, .y = 0.75, .z = 1.5};
    const pga3d::BivectorWeight m = a.antiWedge(b);
    const pga3d::Vector cross = pga3d::Vector::crossRightHanded(a, b);

    CHECK(cross == pga3d::Vector{.x = m.wx, .y = m.wy, .z = m.wz});
    CHECK(pga3d::Vector::crossLeftHanded(a, b) == -cross);
}
