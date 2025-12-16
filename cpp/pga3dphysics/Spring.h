// Copyright (c) 2025 Igor Slobodskov
// SPDX-License-Identifier: MIT

#pragma once
#include <vector>
#include "BodyPoint.h"
#include "Friction.h"
#include "PhysicsBodyConnection.h"

namespace pga3d {
    struct SpringConfig {
        VelocityFriction velocityFriction{};
        Position1dFriction positionFriction{};
        double k = 0.0;
        double targetR = 0.0;
        bool noPush = false;
        bool noPull = false;

        void addForque(const BodyPoint &first, const BodyPoint &second, auto processResult) const noexcept {
            const Point pos1 = first.globalPos();
            const Point pos2 = second.globalPos();
            const Vector dPos = pos2 - pos1;
            const double r2 = dPos.normSquare();
            if (r2 <= targetR * targetR) {
                if (noPush) return;
            } else {
                if (noPull) return;
            }

            const double r = std::sqrt(r2);

            double frictionForce = 0.0;
            if (!velocityFriction.isZero()) {
                const Vector velocity1 = first.globalPosVelocity();
                const Vector velocity2 = second.globalPosVelocity();
                const Vector dv = velocity2 - velocity1;
                const double sign = dPos.antiDot(dv).i;
                frictionForce += velocityFriction(sign * dv.norm());
            }
            frictionForce += positionFriction(r);

            const double mult = (k * (r - targetR) - frictionForce) / (r + 1e-100);
            const Bivector forque = pos1.antiWedge(dPos) * mult;

            processResult(forque);
        }

        void addForque(const BodyPoint &first, const BodyPoint &second) const noexcept {
            addForque(first, second, [&](const Bivector& forque) {
                first.body->addGlobalForquePaired(forque, *second.body);
            });
        }

        [[nodiscard]] Bivector getForque(const BodyPoint &first, const BodyPoint &second) const noexcept {
            Bivector result = {};
            addForque(first, second, [&](const Bivector& forque) {
                result = forque;
            });
            return result;
        }

        [[nodiscard]] double getEnergy(const BodyPoint &first, const BodyPoint &second) const noexcept {
            const Point pos1 = first.globalPos();
            const Point pos2 = second.globalPos();
            const Vector dPos = pos2 - pos1;
            const double r2 = dPos.normSquare();
            if (r2 <= targetR * targetR) {
                if (noPush) return 0.0;
            } else {
                if (noPull) return 0.0;
            }
            const double r = std::sqrt(r2);
            const double dr = r - targetR;
            return 0.5 * k * dr * dr;
        }

        void afterStep(const BodyPoint& first, const BodyPoint& second) noexcept {
            const Point pos1 = first.globalPos();
            const Point pos2 = second.globalPos();
            const Vector dPos = pos2 - pos1;
            const double r = dPos.normSquare();
            positionFriction.correctBoundPosition(r);
        }
    };

    struct Spring {
        SpringConfig config;
        BodyPoint first;
        BodyPoint second;

        void addForque() const noexcept {
            config.addForque(first, second);
        }

        [[nodiscard]] Bivector getForque() const noexcept {
            return config.getForque(first, second);
        }

        void afterStep() noexcept {
            config.afterStep(first, second);
        }

        [[nodiscard]] double energy() const noexcept {
            return config.getEnergy(first, second);
        }
    };

    static_assert(HasEnergy<Spring>);
    static_assert(HasAddForqueMethod<Spring>);
    static_assert(HasAfterStepMethod<Spring>);
}
