package com.github.kright;

import com.badlogic.gdx.math.Vector2;
import me.kright.gametools.pga3d.*;
import me.kright.gametools.pga3d.physics.*;
import scala.runtime.BoxedUnit;

import java.util.ArrayList;
import java.util.Random;

public class BodySystem {

    private final int nBodies;
    private final double gravity;
    private final Pga3dPhysicsSystem bodySystem;

    public BodySystem(int nBodies, double gravity) {
        this.nBodies = nBodies;
        this.gravity = gravity;
        var bodies = new ArrayList<Pga3dPhysicsBody>();

        double centerMass = 100.0;

        if (nBodies > 0) {
            bodies.add(Pga3dPhysicsBody.motionless(new Pga3dInertiaSimple(centerMass, 1000.0), Pga3dMotor.id()));
        }

        var rnd = new Random(0xdeadbeef);

        while (bodies.size() < nBodies) {
            var mass = rnd.nextDouble() + 0.1;

            Pga3dVector position = new Pga3dVector(rnd.nextDouble() - 0.5, rnd.nextDouble() - 0.5, 0.0).times(600.0);

            var dist = position.norm();

            var vy = position.x();
            var vx = -position.y();

            Pga3dBivectorWeight velocity = new Pga3dBivectorWeight(vx, vy, 0.0).normalizedByWeight().times(centerMass * 0.004 * gravity / (Math.pow(dist + 10, 0.5)));

            bodies.add(new Pga3dPhysicsBody(new Pga3dInertiaSimple(mass, mass), Pga3dTranslator.addVector(position).toMotor(), velocity.toBivector()));
        }

        Pga3dPhysicsSolver<Pga3dPhysicsBody> solver = Pga3dPhysicsSolverRK4$.MODULE$;

        bodySystem = new Pga3dPhysicsSystem(bodies.toArray(new Pga3dPhysicsBody[0]), solver, 0.0);
    }

    public void step(double dt) {
        bodySystem.doStep(dt, (unused) -> {
            for (int i = 0; i < nBodies; i++) {
                var body1 = bodySystem.state()[i];
                var pos1 = body1.globalCenter();
                var m1 = body1.inertia().mass();

                for (int j = i + 1; j < nBodies; j++) {
                    var body2 = bodySystem.state()[j];
                    var pos2 = body2.globalCenter();
                    var m2 = body2.inertia().mass();

                    Pga3dVector r = pos2.minus(pos1);

                    double dist = r.norm();

                    var f = r.times(gravity * m1 * m2 / (dist * dist * dist + 1.0));

                    body1.addGlobalForquePaired(Pga3dForque.force(pos1, f), body2);
                }
            }
            return BoxedUnit.UNIT;
        });
    }

    public Pga3dPoint[] positions() {
        Pga3dPoint[] positions = new Pga3dPoint[nBodies];

        var bodies = bodySystem.state();
        for (int i = 0; i < nBodies; i++) {
            positions[i] = bodies[i].globalCenter();
        }

        return positions;
    }
}
