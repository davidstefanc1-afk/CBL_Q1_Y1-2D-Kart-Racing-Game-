import java.awt.Color;
import java.awt.Graphics2D;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Vector2;

/**
 * A kart simulated as a rigid body by the dyn4j physics engine.
 *
 * <p>All physical quantities use SI units (meters, seconds, radians). The conversion to
 * screen pixels is applied only when the kart is drawn. The class serves both the
 * player-controlled kart and the computer-controlled kart: the owner writes the driver
 * inputs ({@link #throttle}, {@link #steer}, {@link #drifting}) and calls
 * {@link #applyControls()} once per physics step.</p>
 */
public class Car {

    // ---- Dimensions ----

    /** Length of the kart body along its direction of travel, in meters. */
    public static final double LENGTH = 3.0;

    /** Width of the kart body, in meters. */
    public static final double WIDTH = 1.5;

    // ---- Handling parameters ----

    /** Forward engine acceleration, in m/s^2. */
    private static final double ACCEL = 18.0;

    /** Braking deceleration while the kart is moving forward, in m/s^2. */
    private static final double BRAKE = 30.0;

    /** Acceleration applied while the kart is reversing, in m/s^2. */
    private static final double REVERSE_ACCEL = 10.0;

    /** Forward speed above which the engine force is no longer applied, in m/s. */
    private static final double MAX_SPEED = 20.0;

    /** Reverse speed above which the reverse force is no longer applied, in m/s. */
    private static final double MAX_REVERSE = 8.0;

    /** Linear damping coefficient representing rolling resistance, in 1/s. */
    private static final double DRAG_ROAD = 0.6;

    /** Maximum yaw rate at full steering input, in rad/s. */
    private static final double STEER_RATE = 2.8;

    /**
     * Forward speed at which steering reaches full authority, in m/s. Below this speed the
     * authority decreases linearly to zero.
     */
    private static final double STEER_REF_SPEED = 6.0;

    /** The rigid body that represents this kart in the physics world. */
    public final Body body;

    /** Color used when rendering the kart. */
    public final Color color;

    // ---- Driver inputs (written by the owner before each physics step) ----

    /** Longitudinal input from -1 (brake or reverse) to 1 (accelerate). */
    public double throttle;

    /** Steering input from -1 (left) to 1 (right). */
    public double steer;

    /** True while the drift input is held; reduces lateral grip. */
    public boolean drifting;

    /**
     * Creates a kart at the given pose and adds its collision shape to a new physics body.
     * The body is not added to a world by this constructor.
     *
     * @param x     initial x position of the kart's center, in meters
     * @param y     initial y position of the kart's center, in meters
     * @param angle initial heading, in radians
     * @param color color used to render the kart
     */
    public Car(double x, double y, double angle, Color color) {
        this.color = color;

        body = new Body();

        // Rectangular collision shape; its long side lies along the body's local x axis.
        BodyFixture fixture = body.addFixture(Geometry.createRectangle(LENGTH, WIDTH));
        fixture.setDensity(1.0);

        // Contact material properties used for kart-to-kart collisions.
        fixture.setFriction(0.2);
        fixture.setRestitution(0.4);

        // Derive mass and rotational inertia from the shape and its density.
        body.setMass(MassType.NORMAL);

        // Continuous collision detection prevents tunnelling at high speed.
        body.setBullet(true);

        // Damping applied to rotational motion.
        body.setAngularDamping(0.8);

        // Rotate about the body's origin first, then move it to the start position.
        body.rotate(angle);
        body.translate(x, y);
    }

    /**
     * Converts the current driver inputs into forces and torque on the kart's body.
     *
     * <p>Must be called once per physics step, before {@code World.step()}. The method
     * applies, in order: longitudinal drive (engine, brake, reverse), linear damping, a
     * lateral grip force that opposes sideways sliding, and a steering torque that drives
     * the yaw rate toward a speed-dependent target.</p>
     */
    public void applyControls() {
        // Keep the body awake so that control forces are always processed.
        body.setAtRest(false);

        double mass = body.getMass().getMass();

        // Kart-local axes expressed in world coordinates.
        Vector2 forward = new Vector2(body.getTransform().getRotationAngle());
        Vector2 side = forward.getLeftHandOrthogonalVector();
        Vector2 vel = body.getLinearVelocity();

        // Signed speed along the heading; negative while reversing.
        double forwardSpeed = vel.dot(forward);

        // --- Longitudinal drive ---
        if (throttle > 0) {
            // Accelerate until the forward speed limit is reached.
            if (forwardSpeed < MAX_SPEED) {
                body.applyForce(forward.product(ACCEL * mass));
            }
        } else if (throttle < 0) {
            if (forwardSpeed > 0.5) {
                // Moving forward: apply the brake force.
                body.applyForce(forward.product(throttle * BRAKE * mass));
            } else if (forwardSpeed > -MAX_REVERSE) {
                // Stationary or reversing: apply reverse drive up to the reverse speed limit.
                body.applyForce(forward.product(throttle * REVERSE_ACCEL * mass));
            }
        }

        // --- Rolling resistance ---
        body.setLinearDamping(DRAG_ROAD);

        // --- Lateral tyre grip ---
        // Velocity component perpendicular to the heading (sideways sliding speed).
        double lateralSpeed = vel.dot(side);

        // Grip response rate in 1/s; a lower rate allows the kart to slide while drifting.
        double gripRate = drifting ? 3.0 : 20.0;

        // Lateral acceleration that would cancel the sideways velocity.
        double lateralAccel = -lateralSpeed * gripRate;

        // Upper bound on the lateral acceleration the tyres can generate, in m/s^2.
        double maxLateralAccel = drifting ? 8.0 : 25.0;

        lateralAccel = Math.max(
                -maxLateralAccel,
                Math.min(maxLateralAccel, lateralAccel)
        );

        // Convert acceleration to force (F = m * a) and apply it along the lateral axis.
        Vector2 lateralForce = side.product(lateralAccel * mass);
        body.applyForce(lateralForce);

        // --- Steering ---
        // Steering authority scales with forward speed and reverses when driving backwards,
        // so the kart cannot rotate on the spot.
        double speedFactor = Math.max(
                -1.0,
                Math.min(1.0, forwardSpeed / STEER_REF_SPEED)
        );

        // Target yaw rate in rad/s; drifting increases steering authority.
        double targetTurn = steer * STEER_RATE * speedFactor;

        if (drifting) {
            targetTurn *= 1.4;
        }

        // Current yaw rate in rad/s and the body's rotational inertia.
        double current = body.getAngularVelocity();
        double inertia = body.getMass().getInertia();

        // Proportional control: angular acceleration is proportional to the yaw-rate error.
        double angularError = targetTurn - current;

        double angularAccel = angularError * 6.0;

        // Limit the angular acceleration, in rad/s^2, to keep the steering response plausible.
        double maxAngularAccel = 8.0;

        angularAccel = Math.max(-maxAngularAccel, Math.min(maxAngularAccel, angularAccel));

        // Apply as torque (torque = inertia * angular acceleration).
        body.applyTorque(angularAccel * inertia);
    }

    /**
     * Renders the kart as a rotated rectangle with a marker on its front edge.
     *
     * @param g2    graphics context to draw on; its state is not modified
     * @param scale rendering scale, in pixels per meter
     */
    public void draw(Graphics2D g2, double scale) {
        // Draw on a copy so the caller's transform and settings are left untouched.
        Graphics2D g = (Graphics2D) g2.create();

        // Move the origin to the kart's center and rotate to its heading.
        Vector2 c = body.getWorldCenter();
        g.translate(c.x * scale, c.y * scale);
        g.rotate(body.getTransform().getRotationAngle());

        // Kart dimensions in pixels.
        int l = (int) (LENGTH * scale);
        int w = (int) (WIDTH * scale);

        // Kart body.
        g.setColor(color);
        g.fillRect(-l / 2, -w / 2, l, w);

        // Marker on the front edge indicating the direction of travel.
        g.setColor(Color.WHITE);
        g.fillRect(l / 2 - 8, -w / 2 + 2, 4, w - 4);

        g.dispose();
    }

    /**
     * Returns the kart's current speed.
     *
     * @return magnitude of the linear velocity, in m/s
     */
    public double getSpeed() {
        return body.getLinearVelocity().getMagnitude();
    }
}
