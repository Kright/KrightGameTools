// Copyright (c) 2025 Igor Slobodskov
// SPDX-License-Identifier: MIT

#pragma once

#include <concepts>
#include "pga3d/Point.h"

namespace pga3d {
    template<typename F>
    concept PointMapping = requires(F f, const Point& p){
        { f(p) } -> std::same_as<Point>;
    };

    template<typename F>
    concept ProjectivePointMapping = requires(F f, const ProjectivePoint& p){
        { f(p) } -> std::same_as<ProjectivePoint>;
    };
}
