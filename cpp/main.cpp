#include <iostream>
#include <chrono>
#include <random>

#include "pga3d/pga3d.h"
#include "pga3dphysics/pga3dphysics.h"


class PhysicsSystem {
public:
    double dt = 0.01;
    double time = 0.0;
    std::vector<pga3d::PhysicsBody> bodies;
    pga3d::PhysicsSolverRK4 solver;

    pga3d::GravitySystem gravity;
    pga3d::PhysicsBodyConnections<pga3d::Spring> springs;

    void doStep() {
        solver.doStepRk4(bodies, dt, [&](double _) {
            gravity.addForques(bodies);
            springs.addForque();
        });

        time += dt;

        springs.afterStep();
    }

    [[nodiscard]] constexpr double getKineticEnergy() const {
        return pga3d::energy(bodies);
    }

    [[nodiscard]] double getTotalEnergy() const {
        return getKineticEnergy() + pga3d::energy(springs) + gravity.energy(bodies);
    }

    [[nodiscard]] constexpr pga3d::Bivector getTotalImpulse() const {
        pga3d::Bivector sum = {};
        for (const auto &body: bodies) {
            sum += body.impulse();
        }
        return sum;
    }
};


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

void measurePerf() {
    const int size = 100000;

    std::vector<pga3d::Bivector> bivecs;
    for (int i = 0; i < size; ++i) {
        bivecs.push_back(makeRandom<pga3d::Bivector>());
    }

    std::vector<pga3d::Motor> motors;
    for (int i = 0; i < size; ++i) {
        motors.push_back(makeRandom<pga3d::Motor>());
    }

    for (int j = 0; j < 10; ++j) {
        pga3d::Bivector sum = {};

        const auto start = std::chrono::high_resolution_clock::now();

        for (int i = 0; i < size; ++i) {
            const auto &bivec = bivecs[i];
            // const auto &motor = motors[i];
            const auto &motor = motors[0];

            sum += motor.sandwich(bivec);
            // sum += motor.geometric(bivec).geometric(motor.reversed()).toBivectorUnsafe();
        }

        const auto end = std::chrono::high_resolution_clock::now();
        const auto duration = std::chrono::duration_cast<std::chrono::nanoseconds>(end - start);

        std::cout << "Motor.sandwich(Bivector) " << duration.count() << "ns"
                  << ", " << duration.count() / size << " ns/step"
                  << ", result = " << sum
                  << std::endl;
    }
}


int main() {
    measurePerf();

    for (int i = 0; i < 10; ++i) {
        PhysicsSystem system;

        for (double shift: {0.0, 1e-2, 1e-1, 1.0, 1e1, 1e2, 1e3, 1e4, 1e5, 1e6}) {
            const pga3d::Motor motor = pga3d::Motor::addVector({shift, shift, shift});

            system.bodies.push_back(pga3d::PhysicsBody{
                .inertia = pga3d::InertiaMovedLocal{
                    .localToGlobal = motor.reversed(),
                    .localInertia = pga3d::InertiaLocal(1.0, 3.0, 2.0, 1.0),
                },
                .state = pga3d::BodyState{
                    .motor = motor,
                    .localB = motor.reversed().sandwich(pga3d::Bivector{0, 0, 0, 1.0, 1.0, -1.0}),
                },
            });
        }

        for (int body_no = 0; body_no < system.bodies.size(); ++body_no) {
            for (int j = 0; j < 3; ++j) {
                system.springs.elems.push_back(pga3d::Spring{
                    .config = pga3d::SpringConfig{
                        .velocityFriction = {.linearK = 2.0, .quadraticK = 1.0, .maxForce = 1000.0},
                        .positionFriction = {.stiffness = {.linearK = 2.0, .maxDelta = 0.001}, .boundPosition = 0.0},
                        .k = 2.0, .targetR = 2.0, .noPush = false, .noPull = false,
                    },
                    .first = {.body = &system.bodies[body_no], .localPoint = {0.0, 0.0, 0.0}},
                    .second = {.body = &system.bodies[body_no], .localPoint = {0.0, 0.0, 1.0}},
                });
                // dummy spring connects еру body to itself
            }
        }

        // system.gravity.gravity = pga3d::Vector{0.0, -9.81, 0.0};

        const double initialEnergy = system.getKineticEnergy();
        const double initialTotalEnergy = system.getTotalEnergy();
        const pga3d::Bivector initialImpulse = system.getTotalImpulse();

        constexpr int stepsCount = 2000;
        auto start = std::chrono::high_resolution_clock::now();

        for (int j = 0; j < stepsCount; ++j) {
            system.doStep();
        }

        auto end = std::chrono::high_resolution_clock::now();
        auto duration = std::chrono::duration_cast<std::chrono::nanoseconds>(end - start);

        std::cout
                << duration.count() << " ns for " << stepsCount << " steps"
                << ", dE = " << system.getKineticEnergy() - initialEnergy
                << ", total dE = " << system.getTotalEnergy() - initialTotalEnergy
                << ", total dImpulse = " << (system.getTotalImpulse() - initialImpulse).norm()
                << ", " << duration.count() / stepsCount << " ns per step"
                << std::endl;
    }

    const pga3d::Motor motor = pga3d::Translator::addVector({1.0, 2.0, 3.0}).geometric(
        pga3d::Quaternion::rotation(pga3d::Vector{0, 0, 1}, pga3d::Vector{1, 1, 0.0}));
    const auto linear = pga3d::LinearOperator<pga3d::ProjectivePoint>::create([&](const pga3d::ProjectivePoint &p) {
        return motor.sandwich(p);
    });

    std::cout << linear << std::endl;
    std::cout << pga3d::linearOperatorForSandwichForProjectivePoint(motor) << std::endl;

    return 0;
}
