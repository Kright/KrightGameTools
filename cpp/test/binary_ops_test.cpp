#include <tuple>
#include <type_traits>
#include <utility>
#include <array>
#include <concepts>
#include <random>
#include <sstream>
#include <string>

#include "doctest.h"
#include "pga3d/pga3d.h"


namespace pg = pga3d;

using TestTypes = std::tuple<
    pg::Motor,
    pg::Plane,
    pg::Bivector,
    pg::ProjectivePoint,
    pg::Quaternion,
    pg::ProjectiveTranslator,
    pg::Translator,
    pg::Vector,
    pg::Point,
    pg::PlaneIdeal,
    pg::BivectorBulk,
    pg::BivectorWeight,
    pg::PseudoScalar,
    pg::PointCenter,
    pg::Multivector
>;


template<class T>
concept HasToMultivector = requires(const T &t) {
    { t.toMultivector() } -> std::same_as<pg::Multivector>;
};

template<class A, class B>
concept HasGeometric = requires(const A &a, const B &b) {
    { a.geometric(b) };
};

template<class A, class B>
concept HasAntiGeometric = requires(const A &a, const B &b) {
    { a.antiGeometric(b) };
};

template<class A, class B>
concept HasDot = requires(const A &a, const B &b) {
    { a.dot(b) };
};

template<class A, class B>
concept HasAntiDot = requires(const A &a, const B &b) {
    { a.antiDot(b) };
};

template<class A, class B>
concept HasMeet = requires(const A &a, const B &b) {
    { a.meet(b) };
};

template<class A, class B>
concept HasJoin = requires(const A &a, const B &b) {
    { a.join(b) };
};

template<class A, class B>
concept HasSandwich = requires(const A &a, const B &b) {
    { a.sandwich(b) };
};

template<class A, class B>
concept HasCross = requires(const A &a, const B &b) {
    { a.cross(b) };
};


template<class T>
constexpr pga3d::Multivector ToMultivector(const T &v) {
    if constexpr (std::is_same_v<T, pg::Multivector>) {
        return v;
    } else if constexpr (std::is_same_v<T, double>) {
        return pg::Multivector{.s = v};
    } else {
        return v.toMultivector();
    }
}


namespace {
    std::mt19937 &rng() {
        static std::mt19937 engine(0xC0FFEEu); // deterministic seed for reproducible tests
        return engine;
    }

    std::uniform_real_distribution<double> &dist01() {
        static std::uniform_real_distribution<double> dist(-1.0, 1.0);
        return dist;
    }

    template<class T>
    T makeRandom() {
        std::array<double, T::componentsCount> arr{};
        for (auto &v: arr) v = dist01()(rng());
        return T::from(arr);
    }
}


template<std::size_t I, std::size_t J>
struct PairInvoker {
    template<class Tuple, class F>
    static void run(F &&f) {
        using A = std::tuple_element_t<I, Tuple>;
        using B = std::tuple_element_t<J, Tuple>;
        const A a = makeRandom<A>();
        const B b = makeRandom<B>();
        INFO("a = " << a);
        INFO("b = " << b);
        std::forward<F>(f).template operator()<A, B>(a, b);
    }
};


template<std::size_t I, class Tuple, class F, std::size_t... J>
static void for_each_j(F &&f, std::index_sequence<J...>) {
    ( PairInvoker<I, J>::template run<Tuple>(std::forward<F>(f)), ... );
}


template<class Tuple, class F, std::size_t... I, std::size_t... J>
static void for_all_pairs(F &&f,
                          std::index_sequence<I...>,
                          std::index_sequence<J...>) {
    ( for_each_j<I, Tuple>(std::forward<F>(f), std::index_sequence<J...>{}), ... );
}


template<class Tuple, class F>
static void for_all_pairs(F &&f) {
    constexpr std::size_t N = std::tuple_size_v<TestTypes>;
    for_all_pairs<Tuple>(f, std::make_index_sequence<N>{}, std::make_index_sequence<N>{});
}


// Like for_all_pairs but iterates once per type, invoking F with a single value of that type
template<std::size_t I>
struct ValueInvoker {
    template<class Tuple, class F>
    static void run(F &&f) {
        using T = std::tuple_element_t<I, Tuple>;
        const T v = makeRandom<T>();
        std::forward<F>(f).template operator()<T>(v);
    }
};


template<class Tuple, class F, std::size_t... I>
static void for_all_values(F &&f, std::index_sequence<I...>) {
    ( ValueInvoker<I>::template run<Tuple>(std::forward<F>(f)), ... );
}


template<class Tuple, class F>
static void for_all_values(F &&f) {
    constexpr std::size_t N = std::tuple_size_v<TestTypes>;
    for_all_values<Tuple>(f, std::make_index_sequence<N>{});
}


TEST_CASE("v.reversed().reversed() == v") {
    for_all_values<TestTypes>([]<class V>(const V &v) {
        if constexpr (std::is_same_v<V, pg::Multivector>) {
            CHECK(v.reversed().reversed() == v);
        } else {
            CHECK(v.reversed().reversed().toMultivector() == v.toMultivector());
        }
    });
}


TEST_CASE("v.antiReversed().antiReversed() == v") {
    for_all_values<TestTypes>([]<class V>(const V &v) {
        if constexpr (std::is_same_v<V, pg::Multivector>) {
            CHECK(v.antiReversed().antiReversed() == v);
        } else {
            CHECK(v.antiReversed().antiReversed().toMultivector() == v.toMultivector());
        }
    });
}


TEST_CASE("v.dual().dual() == v") {
    for_all_values<TestTypes>([]<class V>(const V &v) {
        if constexpr (std::is_same_v<V, pg::PseudoScalar>) {
            // skip
        } else {
            CHECK(ToMultivector(v.dual().dual()) == ToMultivector(v));
        }
    });
}


TEST_CASE("geometric distributes over toMultivector for all supported pairs") {
    for_all_pairs<TestTypes>([]<class A, class B>(const A &a, const B &b) {
        if constexpr (HasGeometric<A, B>) {
            const pg::Multivector lhs = ToMultivector(a.geometric(b));
            const pg::Multivector rhs = ToMultivector(a).geometric(ToMultivector(b));
            INFO("diff = " << (lhs - rhs));
            CHECK((lhs - rhs).norm() < 3e-16);
            // An addition of doubles is not commutative and order varies between implementation
        }
    });
}


TEST_CASE("antiGeometric distributes over toMultivector for all supported pairs") {
    for_all_pairs<TestTypes>([]<class A, class B>(const A &a, const B &b) {
        if constexpr (HasAntiGeometric<A, B>) {
            const pg::Multivector lhs = ToMultivector(a.antiGeometric(b));
            const pg::Multivector rhs = ToMultivector(a).antiGeometric(ToMultivector(b));
            INFO("diff = " << (lhs - rhs));
            CHECK((lhs - rhs).norm() < 4e-16);
            // An addition of doubles is not commutative and order varies between implementation
        }
    });
}


TEST_CASE("dot distributes over toMultivector for all supported pairs") {
    for_all_pairs<TestTypes>([]<class A, class B>(const A &a, const B &b) {
        if constexpr (HasDot<A, B>) {
            const pg::Multivector lhs = ToMultivector(a.dot(b));
            const pg::Multivector rhs = ToMultivector(a).dot(ToMultivector(b));
            INFO("diff = " << (lhs - rhs));
            CHECK((lhs - rhs).norm() < 3e-16);
            // An addition of doubles is not commutative and order varies between implementation
        }
    });
}


TEST_CASE("antiDot distributes over toMultivector for all supported pairs") {
    for_all_pairs<TestTypes>([]<class A, class B>(const A &a, const B &b) {
        if constexpr (HasAntiDot<A, B>) {
            const pg::Multivector lhs = ToMultivector(a.antiDot(b));
            const pg::Multivector rhs = ToMultivector(a).antiDot(ToMultivector(b));
            INFO("diff = " << (lhs - rhs));
            CHECK((lhs - rhs).norm() < 3e-16);
            // An addition of doubles is not commutative and order varies between implementation
        }
    });
}


TEST_CASE("meet distributes over toMultivector for all supported pairs") {
    for_all_pairs<TestTypes>([]<class A, class B>(const A &a, const B &b) {
        if constexpr (HasMeet<A, B>) {
            const pg::Multivector lhs = ToMultivector(a.meet(b));
            const pg::Multivector rhs = ToMultivector(a).meet(ToMultivector(b));
            INFO("diff = " << (lhs - rhs));
            CHECK((lhs - rhs).norm() < 3e-16);
            // An addition of doubles is not commutative and order varies between implementation
        }
    });
}


TEST_CASE("join distributes over toMultivector for all supported pairs") {
    for_all_pairs<TestTypes>([]<class A, class B>(const A &a, const B &b) {
        if constexpr (HasJoin<A, B>) {
            const pg::Multivector lhs = ToMultivector(a.join(b));
            const pg::Multivector rhs = ToMultivector(a).join(ToMultivector(b));
            INFO("diff = " << (lhs - rhs));
            CHECK((lhs - rhs).norm() < 3e-16);
            // An addition of doubles is not commutative and order varies between implementation
        }
    });
}


TEST_CASE("sandwich distributes over toMultivector for all supported pairs") {
    for_all_pairs<TestTypes>([]<class A, class B>(const A &a, const B &b) {
        if constexpr (HasSandwich<A, B>) {
            const pg::Multivector lhs = ToMultivector(a.sandwich(b));
            const pg::Multivector rhs = ToMultivector(a).sandwich(ToMultivector(b));
            INFO("diff = " << (lhs - rhs));
            CHECK((lhs - rhs).norm() < 1e-15);
            // An addition of doubles is not commutative and order varies between implementation
        }
    });
}


TEST_CASE("cross distributes over toMultivector for all supported pairs") {
    for_all_pairs<TestTypes>([]<class A, class B>(const A &a, const B &b) {
        if constexpr (HasCross<A, B>) {
            const pg::Multivector lhs = ToMultivector(a.cross(b));
            const pg::Multivector rhs = ToMultivector(a).cross(ToMultivector(b));
            INFO("diff = " << (lhs - rhs));
            CHECK((lhs - rhs).norm() < 3e-16);
            // An addition of doubles is not commutative and order varies between implementation
        }
    });
}


namespace {
    auto naiveSandwich(const pga3d::Motor &motor, const auto &v) {
        if constexpr (std::is_same_v<decltype(motor.geometric(v)), pga3d::Multivector>) {
            return motor.geometric(v).geometric(motor.reversed().toMultivector());
        } else {
            return motor.geometric(v).geometric(motor.reversed());
        }
    }
}

TEST_CASE("sandwich product is correct for motor") {
    const double eps = 3e-15;
    for (int i = 0; i < 1000; ++i) {
        const pga3d::Motor motor = makeRandom<pga3d::Motor>();

        {
            const pga3d::ProjectivePoint point = makeRandom<pga3d::ProjectivePoint>();
            const pga3d::ProjectivePoint naive = naiveSandwich(motor, point).toProjectivePointUnsafe();
            const pga3d::ProjectivePoint moved = motor.sandwich(point);

            CHECK((moved - naive).norm() < eps);
        }
        {
            const pga3d::Plane plane = makeRandom<pga3d::Plane>();
            const pga3d::Plane naive = naiveSandwich(motor, plane).toPlaneUnsafe();
            const pga3d::Plane moved = motor.sandwich(plane);

            CHECK((moved - naive).norm() < eps);
        }
        {
            const pga3d::Bivector bivec = makeRandom<pga3d::Bivector>();
            const pga3d::Bivector naive = naiveSandwich(motor, bivec).toBivectorUnsafe();
            const pga3d::Bivector moved = motor.sandwich(bivec);

            CHECK((moved - naive).norm() < eps);
        }

        {
            const pga3d::Motor m2 = makeRandom<pga3d::Motor>();
            const pga3d::Motor naive = naiveSandwich(motor, m2);
            const pga3d::Motor moved = motor.sandwich(m2);

            CHECK((moved - naive).norm() < eps);
        }
    }
};
