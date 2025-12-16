// Copyright (c) 2025 Igor Slobodskov
// SPDX-License-Identifier: MIT

#pragma once

#include "pga3d/Motor.h"
#include "pga3d/Bivector.h"
#include "pga3d/opsArithmetic.h"
#include "pga3d/opsWedge.h"
#include "pga3d/opsAntiWedge.h"
#include "pga3d/opsSandwich.h"
#include "InertiaMovedLocal.h"
#include "BodyState.h"
#include "Energy.h"


namespace pga3d {
    struct PhysicsBody {
        InertiaMovedLocal inertia = {};
        BodyState state = {};
        Bivector globalForqueAccumulator = {};

        [[nodiscard]] constexpr Bivector globalForque() const noexcept {
            return globalForqueAccumulator;
        }

        [[nodiscard]] constexpr Bivector localForque() const noexcept {
            return state.motor.reversed().sandwich(globalForque());
        }

        constexpr void resetForqueAccum() noexcept {
            globalForqueAccumulator = {};
        }

        constexpr void addGlobalForque(const Bivector& globalForque) noexcept {
            globalForqueAccumulator += globalForque;
        }

        /** Otherwise it's too easy to forget adding paired force */
        constexpr void addGlobalForquePaired(const Bivector &globalForque, PhysicsBody &other) noexcept {
            globalForqueAccumulator += globalForque;
            other.globalForqueAccumulator -= globalForque;
        }

        [[nodiscard]] constexpr BodyState stateDerivative() const noexcept {
            return BodyState{
                .motor = state.motor.geometric(state.localB) * -0.5,
                .localB = inertia.getAcceleration(state.localB, localForque())
            };
        }

        [[nodiscard]] constexpr double kineticEnergy() const noexcept {
            return state.localB.antiWedge(inertia(state.localB)) * 0.5;
        }

        [[nodiscard]] constexpr Bivector impulse() const noexcept {
            return state.motor.sandwich(inertia(state.localB));
        }

        [[nodiscard]] constexpr Point localPosToGlobal(const Point& localPos) const noexcept {
            return state.localPosToGlobal(localPos);
        }

        [[nodiscard]] constexpr Point globalPosToLocal(const Point& localPos) const noexcept {
            return state.globalPosToLocal(localPos);
        }

        [[nodiscard]] constexpr Point globalCenterOfMass() const noexcept {
            return localPosToGlobal(inertia.centerOfMassPoint());
        }

        [[nodiscard]] constexpr Vector globalVelocityForLocalPos(const Point& localPos) const noexcept {
            return state.globalVelocityForLocalPos(localPos);
        }
    };

    [[nodiscard]] constexpr double energy(const PhysicsBody &body) noexcept {
        return body.kineticEnergy();
    }
    static_assert(HasEnergy<PhysicsBody>);
}
