// Copyright (c) 2025 Igor Slobodskov
// SPDX-License-Identifier: MIT

#pragma once
#include <span>

#include "Forque.h"
#include "pga3d/pga3d.h"
#include "PhysicsBody.h"

namespace pga3d {
    struct GravitySystem {
        Vector gravity{};

        void addForques(std::span<PhysicsBody> bodies) const noexcept {
            if (gravity == Vector{}) return;

            for (auto &body: bodies) {
                body.addGlobalForque(Forque::force(body.globalCenterOfMass(), body.inertia.mass() * gravity));
            }
        }

        [[nodiscard]] constexpr double energy(std::span<const PhysicsBody> bodies) const noexcept {
            if (gravity == Vector{}) return 0.0;
            double totalEnergy = 0.0;
            for (auto &body: bodies) {
                totalEnergy -= gravity.antiDot(body.globalCenterOfMass()).i;
            }
            return totalEnergy;
        }
    };
}
