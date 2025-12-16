// Copyright (c) 2025 Igor Slobodskov
// SPDX-License-Identifier: MIT

#pragma once

#include "pga3d/opsAntiWedge.h"
#include "BodyLine.h"
#include "BodyPoint.h"
#include "PhysicsBodyConnection.h"


namespace pga3d {
    struct SpringToLineConfig {
        double k = 0.0;

        [[nodiscard]] Bivector getForque(const BodyLine &bodyLine, const BodyPoint &bodyPoint) const noexcept {
            const Bivector line = bodyLine.globalLine();
            const Point pos2 = bodyPoint.globalPos();

            const Point posOnLine = pos2.projectOntoLine(line).toPoint();
            return Forque::force(posOnLine, pos2) * k;
        }

        void addForque(const BodyLine &bodyLine, const BodyPoint &bodyPoint) const noexcept {
            const Bivector forque = getForque(bodyLine, bodyPoint);
            bodyLine.body->addGlobalForquePaired(forque, *bodyPoint.body);
        }

        [[nodiscard]] constexpr double getEnergy(const BodyLine &bodyLine, const BodyPoint &bodyPoint) const noexcept {
            const Bivector line = bodyLine.globalLine();
            const Point pos2 = bodyPoint.globalPos();
            const Point posOnLine = pos2.projectOntoLine(line).toPoint();
            const double r2 = (pos2 - posOnLine).normSquare();
            return 0.5 * k * r2;
        }
    };

    struct SpringToLine {
        SpringToLineConfig config;
        BodyLine line;
        BodyPoint point;

        void addForque() const noexcept {
            config.addForque(line, point);
        }

        [[nodiscard]] Bivector getForque() const noexcept {
            return config.getForque(line, point);
        }

        [[nodiscard]] constexpr double energy() const noexcept {
            return config.getEnergy(line, point);
        }
    };

    static_assert(HasEnergy<SpringToLine>);
    static_assert(HasAddForqueMethod<SpringToLine>);
}
