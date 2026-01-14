// Copyright (c) 2025 Igor Slobodskov
// SPDX-License-Identifier: MIT

#pragma once

#include "pga3d/Motor.h"
#include "pga3d/Bivector.h"
#include "pga3d/opsArithmetic.h"

namespace pga3d {
    struct BodyState {
        Motor motor = {};
        Bivector localB = {};

        static constexpr BodyState id() noexcept {
            return {
                .motor = Motor::id(),
                .localB = {},
            };
        }

        static constexpr BodyState zero() noexcept {
            return {
                .motor = {},
                .localB = {},
            };
        }

        [[nodiscard]] BodyState madd(const BodyState& other, double t) const noexcept {
            return {
                .motor = motor.madd(other.motor, t),
                .localB = localB.madd(other.localB, t),
            };
        }

        [[nodiscard]] BodyState renormalized() const noexcept {
            return {
                .motor = motor.renormalized(),
                .localB = localB,
            };
        }

        [[nodiscard]] constexpr Point localPosToGlobal(const Point& localPos) const noexcept {
            return motor.sandwich(localPos).toPointUnsafe();
        }

        [[nodiscard]] constexpr Point globalPosToLocal(const Point& globalPos) const noexcept {
            return motor.reversed().sandwich(globalPos).toPointUnsafe();
        }

        [[nodiscard]] constexpr Vector globalVelocityForLocalPos(const Point& localPos) const noexcept {
            return motor.sandwich(localPos.cross(localB));
        }

        [[nodiscard]] constexpr Point globalCenter() const noexcept {
            return motor.sandwich(PointCenter{}).toPointUnsafe();
        }
    };

    [[nodiscard]] constexpr BodyState operator+(const BodyState& a, const BodyState& b) noexcept {
        return {
            .motor = a.motor + b.motor,
            .localB = a.localB + b.localB,
        };
    }

    constexpr BodyState& operator+=(BodyState& a, const BodyState& b) noexcept {
        a = a + b;
        return a;
    }

    [[nodiscard]] constexpr BodyState operator-(const BodyState& a, const BodyState& b) noexcept {
        return {
            .motor = a.motor - b.motor,
            .localB = a.localB - b.localB,
        };
    }

    constexpr BodyState& operator-=(BodyState& a, const BodyState& b) noexcept {
        a = a - b;
        return a;
    }

    [[nodiscard]] constexpr BodyState operator*(const BodyState& a, double mult) noexcept {
        return {
            .motor = a.motor * mult,
            .localB = a.localB * mult,
        };
    }

    constexpr BodyState& operator*=(BodyState& a, double mult) noexcept {
        a = a * mult;
        return a;
    }

    [[nodiscard]] constexpr BodyState operator*(double mult, const BodyState& a) noexcept {
        return a * mult;
    }
}
