// Copyright (c) 2025 Igor Slobodskov
// SPDX-License-Identifier: MIT

#pragma once

#include "pga3d/Point.h"
#include "pga3d/Vector.h"
#include "pga3d/Bivector.h"
#include "pga3d/opsArithmetic.h"
#include "pga3d/opsAntiWedge.h"
#include "PointMapping.h"

namespace pga3d {
    struct Edge {
        Point a = {};
        Point b = {};

        [[nodiscard]] constexpr Point center() const noexcept {
            return Point::mid(a, b);
        }

        [[nodiscard]] constexpr Vector ab() const noexcept {
            return b - a;
        }

        [[nodiscard]] constexpr Bivector line() const noexcept {
            return a.antiWedge(b);
        }

        [[nodiscard]] constexpr Point interpolatedPoint(const double t) const noexcept {
          return Point::interpolate(a, b, t);
        }

        [[nodiscard]] constexpr Edge reversed() const noexcept {
            return {b, a};
        }

        constexpr void reverse() noexcept {
            *this = reversed();
        }

        [[nodiscard]] double magnitude() const noexcept {
            return ab().norm();
        }

        [[nodiscard]] constexpr double magnitudeSquare() const noexcept {
            return ab().normSquare();
        }

        [[nodiscard]] Edge mapPoints(PointMapping auto mapPoint) const noexcept {
            return {
                .a = mapPoint(a),
                .b = mapPoint(b),
            };
        }
    };
}
