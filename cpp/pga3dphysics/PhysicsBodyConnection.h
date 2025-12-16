// Copyright (c) 2025 Igor Slobodskov
// SPDX-License-Identifier: MIT

#pragma once
#include <concepts>

namespace pga3d {
    template<class T>
    concept HasAddForqueMethod =
            requires(T &obj) {
                { obj.addForque() } -> std::same_as<void>;
            };

    template<class T>
    concept HasBeforeStepMethod =
            requires(T &obj) {
                { obj.beforeStep() } -> std::same_as<void>;
            };

    template<class T>
    concept HasAfterStepMethod =
            requires(T &obj) {
                { obj.afterStep() } -> std::same_as<void>;
            };
}
