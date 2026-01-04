// Copyright (c) 2025 Igor Slobodskov
// SPDX-License-Identifier: MIT

#pragma once

#include "pga3d/Motor.h"
#include "pga3d/opsSandwich.h"

#include "Inertia.h"
#include "InertiaLocal.h"

namespace pga3d {
    struct InertiaMovedLocal {
        Motor localToGlobal = {};
        InertiaLocal localInertia = {};

        [[nodiscard]] constexpr double mass() const noexcept {
            return localInertia.mass;
        }

        [[nodiscard]] constexpr Point centerOfMassPoint() const noexcept {
            return localToGlobal.sandwich(PointCenter{}).toPointUnsafe();
        }

        [[nodiscard]] constexpr Bivector operator ()(const Bivector& globalB) const noexcept {
            const Bivector localB = localToGlobal.reversed().sandwich(globalB);
            const Bivector localI = localInertia(localB);
            return localToGlobal.sandwich(localI);
        }

        [[nodiscard]] constexpr Bivector invert(const Bivector& globalI) const noexcept {
            const Bivector localI = localToGlobal.reversed().sandwich(globalI);
            const Bivector localB = localInertia.invert(localI);
            return localToGlobal.sandwich(localB);
        }

        [[nodiscard]] constexpr Bivector getLocalAcceleration(const Bivector &globalVelocity,
                                                              const Bivector &globalForque) const noexcept {
            const Bivector localB = localToGlobal.reversed().sandwich(globalVelocity);
            const Bivector localF = localToGlobal.reversed().sandwich(globalForque);
            return localInertia.getAcceleration(localB, localF);
        }

        [[nodiscard]] constexpr Bivector getAcceleration(const Bivector &globalVelocity,
                                                         const Bivector &globalForque) const noexcept {
            const Bivector localA = getLocalAcceleration(globalVelocity, globalForque);
            return localToGlobal.sandwich(localA);
        }

        [[nodiscard]] constexpr double getKineticEnergy(const Bivector &globalVelocity) const noexcept {
            const Bivector localB = localToGlobal.reversed().sandwich(globalVelocity);
            return localInertia.getKineticEnergy(localB);
        }

        [[nodiscard]] constexpr InertiaMovedLocal movedBy(const Motor& m) const noexcept {
            return {
                .localToGlobal = m.geometric(localToGlobal),
                .localInertia = localInertia,
            };
        }

        [[nodiscard]] constexpr InertiaMovedLocal movedBy(const Translator& m) const noexcept {
            return {
                .localToGlobal = m.geometric(localToGlobal),
                .localInertia = localInertia,
            };
        }

        [[nodiscard]] constexpr InertiaMovedLocal movedBy(const Quaternion& m) const noexcept {
            return {
                .localToGlobal = m.geometric(localToGlobal),
                .localInertia = localInertia,
            };
        }
    };

    static_assert(Inertia<InertiaMovedLocal>);
}
