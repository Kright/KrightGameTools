// Copyright (c) 2025 Igor Slobodskov
// SPDX-License-Identifier: MIT

#pragma once
#include <array>
#include <span>
#include <concepts>


namespace pga3d {
    template<typename T>
    concept PgaElem = requires(T obj)
    {
        { T::componentsCount } -> std::convertible_to<std::size_t>;
        { obj.toArray() } -> std::same_as<std::array<double, T::componentsCount>>;
        { T::from(std::array<double, T::componentsCount>{}) } -> std::same_as<T>;
    };

    template<PgaElem T>
    struct LinearOperator {
        static constexpr size_t size = T::componentsCount;
        static constexpr size_t squaredSize = size * size;

        std::array<double, T::componentsCount * T::componentsCount> matrix;

        constexpr T operator()(const T& a) const noexcept {
            const auto& aArray = a.toArray();
            std::array<double, T::componentsCount> result;

            for (int i = 0; i < T::componentsCount; ++i) {
                double elem = 0.0;
                for (int j = 0; j < T::componentsCount; ++j) {
                    elem += matrix[i * size + j] * aArray[j];
                }
                result[i] = elem;
            }

            return T::from(result);
        }

        template<class MapFunc>
        static constexpr LinearOperator create(MapFunc func) noexcept
            requires std::regular_invocable<MapFunc&, const T&> && std::same_as<std::invoke_result_t<MapFunc, const T&>, T> {

            LinearOperator result;

            for (size_t i = 0; i < size; ++i) {
                std::array<double, size> arr = {};
                arr[i] = 1.0;

                const T probe = T::from(arr);
                std::array<double, size> map = func(probe).toArray();

                for (size_t j = 0; j < size; ++j) {
                    result.matrix[i + j * size] = map[j];
                }
            }

            return result;
        }

        friend std::ostream& operator<<(std::ostream& os, const LinearOperator<T>& op) {
            os << "LinearOperator([";
            for (size_t i = 0; i < size; ++i) {
                os << "[";
                for (size_t j = 0; j < size; ++j) {
                    os << op.matrix[i * size + j];
                    if (j < T::componentsCount - 1) os << ", ";
                }
                os << "]";
                if (i < size - 1) os << ", ";
            }
            os << "])";
            return os;
        }
    };

    constexpr  LinearOperator<ProjectivePoint> linearOperatorForSandwichForProjectivePoint(const Motor& motor) {
        return LinearOperator<ProjectivePoint>{ motor.sandwichAsMatrixForProjectivePoint() };
    }

    constexpr  LinearOperator<ProjectivePoint> linearOperatorForSandwichForProjectivePoint(const Rotor& q) {
        return LinearOperator<ProjectivePoint>{ q.sandwichAsMatrixForProjectivePoint() };
    }
}