// Copyright (c) 2025 Igor Slobodskov
// SPDX-License-Identifier: MIT

#pragma once

#include "pga3d/Point.h"
#include "pga3d/Vector.h"
#include "pga3d/Plane.h"
#include "pga3d/opsArithmetic.h"
#include "pga3d/opsAntiWedge.h"
#include "PointMapping.h"

namespace pga3d {
    struct Triangle {
        Point a = {};
        Point b = {};
        Point c = {};

        [[nodiscard]] constexpr Vector ab() const noexcept { return b - a; }
        [[nodiscard]] constexpr Vector ac() const noexcept { return c - a; }
        [[nodiscard]] constexpr Vector bc() const noexcept { return c - b; }

        [[nodiscard]] constexpr Point center() const noexcept {
            return Point::mid(a, b, c);
        }

        [[nodiscard]] constexpr Plane plane() const noexcept {
            return a.antiWedge(b).antiWedge(c);
        }

        [[nodiscard]] double perimeter() const noexcept {
            return ab().norm() + ac().norm() + bc().norm();
        }

        [[nodiscard]] double area() const noexcept {
            return 0.5 * ab().antiWedge(ac()).norm();
        }

        [[nodiscard]] Triangle mapPoints(PointMapping auto mapPoint) const noexcept {
            return {
                .a = mapPoint(a),
                .b = mapPoint(b),
                .c = mapPoint(c)
            };
        }
    };
}
