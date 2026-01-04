// Copyright (c) 2025 Igor Slobodskov
// SPDX-License-Identifier: MIT

#pragma once

#include "pga3d/Bivector.h"
#include "pga3d/opsArithmetic.h"

//
// Helper formulas for inertia construction
//
namespace pga3d {
    // todo add concept for Inertia
    template<typename T>
    concept Inertia = requires(const T& t, const Bivector& velocity, const Bivector& impulse, const Bivector& forque) {
        { t(velocity) } -> std::convertible_to<Bivector>; // impulse
        { t.invert(impulse) } -> std::convertible_to<Bivector>; // velocity
        { t.getAcceleration(velocity, forque) } -> std::convertible_to<Bivector>; // invert(velocity.cross(apply(velocity)) + forque)
        { t.getKineticEnergy(velocity) } -> std::convertible_to<double>;
    };


    namespace inertia {
        [[nodiscard]] static constexpr double diskInPlane(const double r, const double innerR = 0.0) noexcept {
            return 0.5 * (r * r + innerR * innerR);
        }

        [[nodiscard]] static constexpr double diskXX(const double r, const double innerR = 0.0) noexcept {
            return 0.25 * (r * r + innerR * innerR);
        }

        [[nodiscard]] static constexpr double rodAlongAxis(const double length) noexcept {
            return length * length / 12;
        }

        [[nodiscard]] constexpr double getKineticEnergy(const Bivector& velocity, const Bivector& impulse) noexcept {
            return velocity.antiWedge(impulse) * 0.5;
        }
    }
}
