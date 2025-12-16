// Copyright (c) 2025 Igor Slobodskov
// SPDX-License-Identifier: MIT

#pragma once
#include <vector>
#include "Energy.h"
#include "Spring.h"
#include "PhysicsBodyConnection.h"

namespace pga3d {
    template<class Connection> requires HasAddForqueMethod<Connection>
    struct PhysicsBodyConnections {
        std::vector<Connection> elems;

        void beforeStep() {
            if constexpr (HasBeforeStepMethod<Connection>) {
                for (auto &elem: elems) {
                    elem.beforeStep();
                }
            }
        }

        void addForque() const {
            for (const auto &elem: elems) {
                elem.addForque();
            }
        }

        void afterStep() {
            if constexpr (HasAfterStepMethod<Connection>) {
                for (auto &elem: elems) {
                    elem.afterStep();
                }
            }
        }
    };

    template<HasEnergy Connection>
    [[nodiscard]] double energy(const PhysicsBodyConnections<Connection> &connections) {
        return energy(connections.elems);
    }

    static_assert(HasEnergy<Spring>);
    static_assert(HasEnergy<PhysicsBodyConnections<Spring> >);
}
