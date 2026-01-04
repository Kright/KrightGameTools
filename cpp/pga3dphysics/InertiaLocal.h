// Copyright (c) 2025 Igor Slobodskov
// SPDX-License-Identifier: MIT

#pragma once

#include "pga3d/Bivector.h"
#include "InertiaLocalSphere.h"
#include "Inertia.h"

namespace pga3d {
    struct InertiaLocal {
        double mass = 0.0;
        double mryz = 0.0; // moment of inertia in plane yz (including both integral for (dm * y^2 and dm * z^2)
        double mrxz = 0.0;
        double mrxy = 0.0;

        [[nodiscard]] constexpr Bivector operator()(const Bivector& velocity) const noexcept {
            return Bivector {
              .wx = velocity.yz * mryz,
              .wy = -velocity.xz * mrxz,
              .wz = velocity.xy * mrxy,
              .xy = velocity.wz * mass,
              .xz = -velocity.wy * mass,
              .yz = velocity.wx * mass,
            };
        }

        [[nodiscard]] constexpr Bivector invert(const Bivector& localInertia) const noexcept {
            const double massInv = 1.0 / mass;

            return Bivector {
              .wx = localInertia.yz * massInv,
              .wy = -localInertia.xz * massInv,
              .wz = localInertia.xy * massInv,
              .xy = localInertia.wz / mrxy,
              .xz = -localInertia.wy / mrxz,
              .yz = localInertia.wx / mryz,
            };
        }

        /** invert(localB.cross(apply(localB)) + localForque) */
        [[nodiscard]] constexpr Bivector getAcceleration(const Bivector& localB, const Bivector& localForque) const noexcept {
            return Bivector {
              .wx = localForque.yz / mass + localB.wy * localB.xy + localB.wz * localB.xz,
              .wy = -localForque.xz / mass + localB.wz * localB.yz - localB.wx * localB.xy,
              .wz = localForque.xy / mass - localB.wx * localB.xz - localB.wy * localB.yz,
              .xy = (localForque.wz + localB.xz * localB.yz * (mrxz - mryz)) / mrxy,
              .xz = (-localForque.wy + localB.xy * localB.yz * (mryz - mrxy)) / mrxz,
              .yz = (localForque.wx + localB.xy * localB.xz * (mrxy - mrxz)) / mryz,
            };
        }

        [[nodiscard]] constexpr double getKineticEnergy(const Bivector& velocity) const noexcept {
            return inertia::getKineticEnergy(velocity, operator()(velocity));
        }


        // constructors
        [[nodiscard]] static constexpr InertiaLocal from(const InertiaLocalSphere& i) noexcept {
            return {
                .mass = i.mass,
                .mryz = i.mryz(),
                .mrxz = i.mrxz(),
                .mrxy = i.mrxy(),
            };
        }

        [[nodiscard]] static constexpr InertiaLocal symmetric(const double mass, const double mr2) noexcept {
            return {.mass = mass, .mryz = mr2, .mrxz = mr2, .mrxy = mr2};
        }

        [[nodiscard]] static constexpr InertiaLocal fromXXYYZZ(const double mass, const double xx, const double yy, const double zz) noexcept {
            return {
                .mass = mass,
                .mryz = (yy + zz) * mass,
                .mrxz = (xx + zz) * mass,
                .mrxy = (xx + yy) * mass,
            };
        }

        [[nodiscard]] static constexpr InertiaLocal point(const double mass) noexcept {
            return from(InertiaLocalSphere::point(mass));
        }

        [[nodiscard]] static constexpr InertiaLocal cube(const double mass, const double rx) noexcept {
            return from(InertiaLocalSphere::cube(mass, rx));
        }

        [[nodiscard]] static constexpr InertiaLocal cube(const double mass, const double rx, const double ry, const double rz) noexcept {
            constexpr double mult = 1.0 / 3.0;
            return fromXXYYZZ(mass, rx * rx * mult, ry * ry * mult, rz * rz * mult);
        }

        [[nodiscard]] static constexpr InertiaLocal hollowSphere(const double mass, const double r) noexcept {
            return from(InertiaLocalSphere::hollowSphere(mass, r));
        }

        [[nodiscard]] static constexpr InertiaLocal solidSphere(const double mass, const double r) noexcept {
            return from( InertiaLocalSphere::solidSphere(mass, r));
        }

        [[nodiscard]] static constexpr InertiaLocal solidEllipsoid(const double mass, const double rx, const double ry, const double rz) noexcept {
            constexpr double mult = 1.0 / 5.0;
            return fromXXYYZZ(mass, rx * rx * mult, ry * ry * mult, rz * rz * mult);
        }

        // rod along OX
        [[nodiscard]] static constexpr InertiaLocal rodX(const double mass, const double length) noexcept {
            const double rod = inertia::rodAlongAxis(length);
            return fromXXYYZZ(mass, rod, 0.0, 0.0);
        }

        // disk in plane YZ
        [[nodiscard]] static constexpr InertiaLocal diskYZ(const double mass, const double r, const double innerR = 0.0) noexcept {
            const double disk = inertia::diskXX(r, innerR);
            return fromXXYYZZ(mass, 0.0, disk, disk);
        }

        // cylinder with axis along OX
        [[nodiscard]] static constexpr InertiaLocal cylinderX(const double mass, const double length, const double r, const double innerR = 0.0) noexcept {
            const double disk = inertia::diskXX(r, 0.0);
            const double rod = inertia::rodAlongAxis(length);
            return fromXXYYZZ(mass, rod, disk, disk);
        }
    };

    static_assert(Inertia<InertiaLocal>);
}
