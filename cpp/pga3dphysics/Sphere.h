// Copyright (c) 2025 Igor Slobodskov
// SPDX-License-Identifier: MIT

#pragma once
#include "pga3d/Point.h"
#include "PointMapping.h"


namespace pga3d {
    struct Sphere {
        Point center = {};
        double radius = 0.0;

        [[nodiscard]] constexpr bool contains(const Point& point) const noexcept {
            const Vector dr = point - center;
            const double drNormSquare = dr.normSquare();
            return drNormSquare <= radius * radius;
        }

        [[nodiscard]] Point clamp(const Point& point) const noexcept {
            const Vector dr = point - center;
            const double drNormSquare = dr.normSquare();
            if (drNormSquare <= radius * radius) {
                return point;
            }

            const double drNorm = std::sqrt(drNormSquare);
            return center + dr * (radius / (drNorm + 1e-100));
        }

        [[nodiscard]] constexpr Vector clampDR(const Point& point) const noexcept {
            const Vector dr = point - center;
            const double drNormSquare = dr.normSquare();
            if (drNormSquare <= radius * radius) {
                return dr;
            }

            const double drNorm = std::sqrt(drNormSquare);
            return dr * (radius / (drNorm + 1e-100));
        }

        [[nodiscard]] Sphere mapPoints(PointMapping auto mapPoint) const noexcept {
            return {
                .center = mapPoint(center),
                .radius = radius
            };
        }
    };
}
