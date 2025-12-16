// Copyright (c) 2025 Igor Slobodskov
// SPDX-License-Identifier: MIT

#pragma once

#include "pga3d/opsGeometric.h"
#include "pga3d/opsAntiWedge.h"
#include "BodyLine.h"
#include "BodyPoint.h"
#include "Forque.h"
#include "PhysicsBodyConnection.h"


namespace pga3d {
    struct TorsionConfig {
        double kNperRad = 0.0;

        void calculateForque(const BodyPoint &first,
                             const BodyPoint &second,
                             const BodyLine &axis,
                             auto onResult) const noexcept {
            const Point firstGlobalPos = first.globalPos();
            const Point secondGlobalPos = second.globalPos();

            const Bivector axisGlobalLine = axis.globalLine();

            const Point firstProjectedToLine = firstGlobalPos.projectOntoLine(axisGlobalLine).toPoint();
            const Point secondProjectedToLine = secondGlobalPos.projectOntoLine(axisGlobalLine).toPoint();

            const Vector dr1 = firstGlobalPos - firstProjectedToLine;
            const Vector dr2 = secondGlobalPos - secondProjectedToLine;

            const double dr1NormSquare = dr1.normSquare();
            const double dr2NormSquare = dr2.normSquare();

            const double dr1Dr2Norm = std::sqrt(dr1NormSquare * dr2NormSquare);

            if (dr1Dr2Norm < 1e-100) return;

            const BivectorBulk momentum = (dr1.dual().geometric(dr2.dual()) / dr1Dr2Norm).log() * kNperRad;

            const BivectorWeight f1 = Translator::addVector(dr1).sandwich(-momentum).weight() / dr1NormSquare;
            const Vector force1 = Vector(f1.wx, f1.wy, f1.wz);

            const BivectorWeight f2 = Translator::addVector(dr2).sandwich(momentum).weight() / dr2NormSquare;
            const Vector force2 = Vector(f2.wx, f2.wy, f2.wz);

            const Bivector mf1 = Forque::force(firstGlobalPos, force1);
            const Bivector mf2 = Forque::force(secondGlobalPos, force2);

            onResult(mf1, mf2);
        }

        [[nodiscard]] double getEnergy(const BodyPoint &first,
                                       const BodyPoint &second,
                                       const BodyLine &axis) const noexcept {
            const Point firstGlobalPos = first.globalPos();
            const Point secondGlobalPos = second.globalPos();

            const Bivector axisGlobalLine = axis.globalLine();

            const Point firstProjectedToLine = firstGlobalPos.projectOntoLine(axisGlobalLine).toPoint();
            const Point secondProjectedToLine = secondGlobalPos.projectOntoLine(axisGlobalLine).toPoint();

            const Vector dr1 = firstGlobalPos - firstProjectedToLine;
            const Vector dr2 = secondGlobalPos - secondProjectedToLine;

            const double dr1NormSquare = dr1.normSquare();
            const double dr2NormSquare = dr2.normSquare();

            const double dr1Dr2Norm = std::sqrt(dr1NormSquare * dr2NormSquare);

            if (dr1Dr2Norm < 1e-100) return 0.0;

            const BivectorBulk rotationAngle = (dr1.dual().geometric(dr2.dual()) / dr1Dr2Norm).log();
            return 0.5 * kNperRad * rotationAngle.normSquare();
        }

        void addForque(const BodyPoint &first, const BodyPoint &second, const BodyLine &axis) const noexcept {
            calculateForque(first, second, axis, [&](const Bivector &mf1, const Bivector &mf2) {
                first.body->addGlobalForquePaired(mf1, *axis.body);
                second.body->addGlobalForquePaired(mf2, *axis.body);
            });
        }

        [[nodiscard]] std::pair<Bivector, Bivector> getTorque(const BodyPoint &first, const BodyPoint &second,
                                                              const BodyLine &axis) const noexcept {
            std::pair<Bivector, Bivector> result = {};

            calculateForque(first, second, axis, [&](const Bivector &mf1, const Bivector &mf2) {
                result.first = mf1;
                result.second = mf2;
            });

            return result;
        }
    };


    struct Torsion {
        TorsionConfig config = {};

        BodyPoint first = {};
        BodyPoint second = {};
        BodyLine axis = {};

        void addForque() const noexcept {
            config.addForque(first, second, axis);
        }

        [[nodiscard]] std::pair<Bivector, Bivector> getForque() const noexcept {
            return config.getTorque(first, second, axis);
        }

        [[nodiscard]] double energy() const noexcept {
            return config.getEnergy(first, second, axis);
        }
    };

    static_assert(HasEnergy<Torsion>);
    static_assert(HasAddForqueMethod<Torsion>);
}
