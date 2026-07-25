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
    CHECK(mv.toPlaneIdealUnsafe().toMultivector().toPlaneIdealUnsafe() == mv.toPlaneIdealUnsafe());
    CHECK(mv.toBivectorBulkUnsafe().toMultivector().toBivectorBulkUnsafe() == mv.toBivectorBulkUnsafe());
    CHECK(mv.toBivectorWeightUnsafe().toMultivector().toBivectorWeightUnsafe() == mv.toBivectorWeightUnsafe());
}

