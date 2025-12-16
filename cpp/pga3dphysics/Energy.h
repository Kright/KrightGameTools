// Copyright (c) 2025 Igor Slobodskov
// SPDX-License-Identifier: MIT

#pragma once

#include <concepts>
#include <vector>

namespace pga3d {
    template<class T>
    concept HasEnergy =
            requires(const T &obj) {
                { energy(obj) } -> std::same_as<double>;
            };


    template<class T>
    concept HasEnergyMethod =
            requires(const T &obj) {
                { obj.energy() } -> std::same_as<double>;
            };

    template<HasEnergyMethod T>
    auto energy(const T &obj) {
        return obj.energy();
    }


    template<HasEnergy T>
    double energy(const std::span<const T>& elems) {
        double totalEnergy = 0.0;
        for (const auto &elem: elems) {
            totalEnergy += energy(elem);
        }
        return totalEnergy;
    }

    template<HasEnergy T>
    double energy(const std::vector<T>& elems) {
        double totalEnergy = 0.0;
        for (const auto &elem: elems) {
            totalEnergy += energy(elem);
        }
        return totalEnergy;
    }
}
