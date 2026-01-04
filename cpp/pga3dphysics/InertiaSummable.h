// Copyright (c) 2025 Igor Slobodskov
// SPDX-License-Identifier: MIT

#pragma once

#include "pga3d/Point.h"
#include "pga3d/ProjectivePoint.h"
#include "pga3d/Quaternion.h"
#include "pga3d/Translator.h"
#include "pga3d/Motor.h"

#include "PointMapping.h"
#include "InertiaLocalSphere.h"
#include "InertiaLocal.h"
#include "InertiaMovedLocal.h"

namespace pga3d {
    struct InertiaSummable {
        double ww = 0.0;
        double wx = 0.0;
        double wy = 0.0;
        double wz = 0.0;
        double xx = 0.0;
        double yy = 0.0;
        double zz = 0.0;
        double xy = 0.0;
        double yz = 0.0;
        double xz = 0.0;

        [[nodiscard]] constexpr double mass() const noexcept {
            return ww;
        }

        [[nodiscard]] constexpr Point centerOfMass() const noexcept {
            const double inv = 1.0 / mass();
            return {.x = wx * inv, .y = wy * inv, .z = wz * inv};
        }

        [[nodiscard]] constexpr ProjectivePoint centerOfMassProjective() const noexcept {
            return {.x = wx , .y = wy , .z = wz, .w = ww};
        }

        [[nodiscard]] constexpr double normSquare() const noexcept {
            return ww * ww + wx * wx + wy * wy + wz * wz + xx * xx + yy * yy + zz * zz + xy * xy + yz * yz + xz * xz;
        }

        [[nodiscard]] constexpr Bivector operator()(const Bivector& b) const noexcept {
            return {
                .wx = +(yy + zz) * b.yz + xy * b.xz - xz * b.xy - wz * b.wy + wy * b.wz,
                .wy = -xy * b.yz - (xx + zz) * b.xz - yz * b.xy - wx * b.wz + wz * b.wx,
                .wz = -xz * b.yz + yz * b.xz + (xx + yy) * b.xy + wx * b.wy - wy * b.wx,
                .xy = ww * b.wz + wx * b.xz + wy * b.yz,
                .xz = -ww * b.wy - wx * b.xy + wz * b.yz,
                .yz = ww * b.wx - wy * b.xy - wz * b.xz,
            };
        }

    private:
        [[nodiscard]] constexpr InertiaSummable movedByImpl(ProjectivePointMapping auto mapProjectivePoint) const noexcept {
            const ProjectivePoint cx = mapProjectivePoint(ProjectivePoint{xx, xy , xz, wx});
            const ProjectivePoint cy = mapProjectivePoint(ProjectivePoint{xy, yy, yz, wy});
            const ProjectivePoint cz = mapProjectivePoint(ProjectivePoint{xz, yz, zz, wz});
            const ProjectivePoint cw = mapProjectivePoint(ProjectivePoint{wx, wy, wz, ww});

            const ProjectivePoint rx = mapProjectivePoint(ProjectivePoint{cx.x, cy.x, cz.x, cw.x});
            const ProjectivePoint ry = mapProjectivePoint(ProjectivePoint{cx.y, cy.y, cz.y, cw.y});
            const ProjectivePoint rz = mapProjectivePoint(ProjectivePoint{cx.z, cy.z, cz.z, cw.z});
            const ProjectivePoint rw = mapProjectivePoint(ProjectivePoint{cx.w, cy.w, cz.w, cw.w});

            return {
                .ww = rw.w,
                .wx = rw.x,
                .wy = rw.y,
                .wz = rw.z,
                .xx = rx.x,
                .yy = ry.y,
                .zz = rz.z,
                .xy = rx.y,
                .yz = ry.z,
                .xz = rx.z
            };
        }
    public:

        [[nodiscard]] constexpr InertiaSummable movedBy(const Motor& m) const noexcept {
            return movedByImpl([&](const ProjectivePoint& p) { return m.sandwich(p); });
        }

        [[nodiscard]] constexpr InertiaSummable movedBy(const Translator& m) const noexcept {
            return movedByImpl([&](const ProjectivePoint& p) { return m.sandwich(p); });
        }

        [[nodiscard]] constexpr InertiaSummable movedBy(const Quaternion& m) const noexcept {
            return movedByImpl([&](const ProjectivePoint& p) { return m.sandwich(p); });
        }

        [[nodiscard]] static constexpr InertiaSummable point(const Point& p, double mass) noexcept {
            // inertia of a mass in a point = symmetricProduct(p.toProjectivePoint, p * mass)
            return {
                .ww = mass,
                .wx = p.x * mass,
                .wy = p.y * mass,
                .wz = p.z * mass,
                .xx = p.x * p.x * mass,
                .yy = p.y * p.y * mass,
                .zz = p.z * p.z * mass,
                .xy = p.x * p.y * mass,
                .yz = p.y * p.z * mass,
                .xz = p.x * p.z * mass,
            };
        }

        [[nodiscard]] static constexpr InertiaSummable from(const InertiaLocalSphere& inertia) noexcept {
            return {
                .ww = inertia.mass,
                .wx = 0.0,
                .wy = 0.0,
                .wz = 0.0,
                .xx = inertia.mr2 * 0.5,
                .yy = inertia.mr2 * 0.5,
                .zz = inertia.mr2 * 0.5,
                .xy = 0.0,
                .yz = 0.0,
                .xz = 0.0,
            };
        }

        [[nodiscard]] static constexpr InertiaSummable from(const InertiaLocal& inertia) noexcept {
            const double mrxyz2 = (inertia.mrxy + inertia.mrxz + inertia.mryz) * 0.5;

            const double mrx2 = mrxyz2 - inertia.mryz;
            const double mry2 = mrxyz2 - inertia.mrxz;
            const double mrz2 = mrxyz2 - inertia.mrxy;

            return {
                .ww = inertia.mass,
                .wx = 0.0,
                .wy = 0.0,
                .wz = 0.0,
                .xx = mrx2,
                .yy = mry2,
                .zz = mrz2,
                .xy = 0.0,
                .yz = 0.0,
                .xz = 0.0
            };
        }

        [[nodiscard]] static constexpr InertiaSummable from(const InertiaMovedLocal& inertia) noexcept {
            return from(inertia.localInertia).movedBy(inertia.localToGlobal);
        }
    };

    [[nodiscard]] constexpr InertiaSummable operator+(const InertiaSummable& a, const InertiaSummable& b) noexcept {
        return {
            .ww = a.ww + b.ww,
            .wx = a.wx + b.wx,
            .wy = a.wy + b.wy,
            .wz = a.wz + b.wz,
            .xx = a.xx + b.xx,
            .yy = a.yy + b.yy,
            .zz = a.zz + b.zz,
            .xy = a.xy + b.xy,
            .yz = a.yz + b.yz,
            .xz = a.xz + b.xz
        };
    }

    [[nodiscard]] constexpr InertiaSummable& operator+=(InertiaSummable& a, const InertiaSummable& b) noexcept {
        a = a + b;
        return a;
    }

    [[nodiscard]] constexpr InertiaSummable operator-(const InertiaSummable& a, const InertiaSummable& b) noexcept {
        return {
            .ww = a.ww - b.ww,
            .wx = a.wx - b.wx,
            .wy = a.wy - b.wy,
            .wz = a.wz - b.wz,
            .xx = a.xx - b.xx,
            .yy = a.yy - b.yy,
            .zz = a.zz - b.zz,
            .xy = a.xy - b.xy,
            .yz = a.yz - b.yz,
            .xz = a.xz - b.xz
        };
    }

    [[nodiscard]] constexpr InertiaSummable& operator-=(InertiaSummable& a, const InertiaSummable& b) noexcept {
        a = a - b;
        return a;
    }

    [[nodiscard]] constexpr InertiaSummable operator*(const InertiaSummable& a, double mult) noexcept {
        return {
            .ww = a.ww * mult,
            .wx = a.wx * mult,
            .wy = a.wy * mult,
            .wz = a.wz * mult,
            .xx = a.xx * mult,
            .yy = a.yy * mult,
            .zz = a.zz * mult,
            .xy = a.xy * mult,
            .yz = a.yz * mult,
            .xz = a.xz * mult
        };
    }

    [[nodiscard]] constexpr InertiaSummable operator*(double mult, const InertiaSummable& a) noexcept {
        return a * mult;
    }

    [[nodiscard]] constexpr InertiaSummable& operator*=(InertiaSummable& a, double mult) noexcept {
        a = a * mult;
        return a;
    }
}
