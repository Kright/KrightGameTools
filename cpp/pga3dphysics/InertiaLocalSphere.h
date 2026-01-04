// Copyright (c) 2025 Igor Slobodskov
// SPDX-License-Identifier: MIT

#pragma once

#include "pga3d/Bivector.h"
#include "Inertia.h"

namespace pga3d {
    /**
     * Fully symmetrical tensor of inertia (for example, for cube or sphere).
     * It has no precession, and computations are simpler and faster
     *
     * @param mass - mass
     * @param mr2  - square of effective radius multiplied by mass
     */
    struct InertiaLocalSphere {
        double mass = 0.0;
        double mr2 = 0.0;

        [[nodiscard]] constexpr double mryz() const noexcept { return mr2; }
        [[nodiscard]] constexpr double mrxz() const noexcept { return mr2; }
        [[nodiscard]] constexpr double mrxy() const noexcept { return mr2; }

        [[nodiscard]] constexpr Bivector operator()(const Bivector& velocity) const noexcept {
            return Bivector {
                .wx = velocity.yz * mr2,
                .wy = -velocity.xz * mr2,
                .wz = velocity.xy * mr2,
                .xy = velocity.wz * mass,
                .xz = -velocity.wy * mass,
                .yz = velocity.wx * mass,
              };
        }

        [[nodiscard]] constexpr Bivector invert(const Bivector& localInertia) const noexcept {
            const double massInv = 1.0 / mass;
            const double mr2Inv = 1.0 / mr2;

            return Bivector {
                .wx = localInertia.yz * massInv,
                .wy = -localInertia.xz * massInv,
                .wz = localInertia.xy * massInv,
                .xy = localInertia.wz * mr2Inv,
                .xz = -localInertia.wy * mr2Inv,
                .yz = localInertia.wx * mr2Inv,
            };
        }

        /** invert(localB.cross(apply(localB)) + localForque) */
        [[nodiscard]] constexpr Bivector getAcceleration(const Bivector& localB, const Bivector& localForque) const noexcept {
            const double massInv = 1.0 / mass;
            const double mr2Inv = 1.0 / mr2;

            return {
                .wx = localForque.yz * massInv + localB.wy * localB.xy + localB.wz * localB.xz,
                .wy = -localForque.xz * massInv + localB.wz * localB.yz - localB.wx * localB.xy,
                .wz = localForque.xy * massInv - localB.wx * localB.xz - localB.wy * localB.yz,
                .xy = localForque.wz * mr2Inv,
                .xz = -localForque.wy * mr2Inv,
                .yz = localForque.wx * mr2Inv,
            };
        }

        [[nodiscard]] constexpr double getKineticEnergy(const Bivector& velocity) const noexcept {
            return inertia::getKineticEnergy(velocity, operator()(velocity));
        }

        [[nodiscard]] static constexpr InertiaLocalSphere fromXX(const double mass, const double xx) noexcept {
            return {
                .mass = mass,
                .mr2 = mass * xx * 2.0 ,
            };
        }

        [[nodiscard]] static constexpr InertiaLocalSphere fromR2(const double mass, const double r2) noexcept {
            return {
                .mass = mass,
                .mr2 = mass * r2,
            };
        }

        [[nodiscard]] static constexpr InertiaLocalSphere point(const double mass) noexcept {
            return {mass, 0.0};
        }

        [[nodiscard]] static constexpr InertiaLocalSphere cube(const double mass, const double rx) noexcept {
            return fromR2(mass, rx * rx * (2.0 / 3.0));
        }

        [[nodiscard]] static constexpr InertiaLocalSphere hollowSphere(const double mass, const double r) noexcept {
            return fromR2(mass, r * r * (2.0 / 3.0));
        }

        [[nodiscard]] static constexpr InertiaLocalSphere solidSphere(const double mass, const double r) noexcept {
            return fromR2(mass, r * r * (2.0 / 5.0));
        }
    };

    static_assert(Inertia<InertiaLocalSphere>);
}