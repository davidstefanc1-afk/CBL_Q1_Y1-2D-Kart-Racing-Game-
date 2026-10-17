import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.dyn4j.dynamics.Body;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.World;

/**
 * The main game surface. It owns the physics world, runs the simulation loop, handles
 * keyboard input and renders the track and the karts.
 *
 * <p>Physics operates in meters and rendering in pixels, related by {@code SCALE}. The
 * simulation advances with a fixed timestep, independent of the rendering rate. The track
 * boundary is enforced geometrically: the road is the set of all points within
 * {@code HALF_ROAD} of the track centerline.</p>
 */
public class GamePanel extends JPanel implements KeyListener {

    /** Rendering scale, in pixels per meter. */
    private static final double SCALE = 10.0;

    /** Fixed simulation timestep, in seconds (60 Hz, matching dyn4j's default step frequency). */
    private static final double DT = 1.0 / 60.0;

    /** Road width in pixels, used for both rendering and collision. */
    private static final float ROAD_WIDTH = 100f;

    /** Half the road width in meters: the maximum distance from the centerline to any point of a kart. */
    private static final double HALF_ROAD = ROAD_WIDTH / 2.0 / SCALE;

    /** Coefficient of restitution at the track edge: 0 is fully inelastic, 1 is fully elastic. */
    private static final double WALL_BOUNCE = 0.3;

    /** Generator for the random track layout. */
    private final RaceTrack track = new RaceTrack();

    /** Smooth closed curve describing the track centerline, in pixels. Used for rendering. */
    private Path2D.Double raceTrack;

    /**
     * Track centerline as a closed polyline, in meters. Used for collision. The last point
     * connects back to the first.
     */
    private double[][] center;

    /** The physics world containing both karts. */
    private final World<Body> world = new World<>();

    /** The player-controlled kart. */
    private Car car;

    /** The second kart. It does not yet receive driver input. */
    private Car bot;

    /** Current state of the driving keys. */
    private boolean upPressed, downPressed, leftPressed, rightPressed, driftPressed;

    /** Timer that drives the update and repaint cycle. */
    private final Timer timer;

    /** Timestamp of the previous update, in nanoseconds. */
    private long lastTime = System.nanoTime();

    /** Elapsed real time that has not yet been simulated, in seconds. */
    private double accumulator = 0;

    /**
     * Creates the game panel, configures the physics world and keyboard input, and starts
     * the game loop.
     */
    public GamePanel() {
        // Top-down view: no gravity.
        world.setGravity(new Vector2(0, 0));

        // Register for keyboard input; focus is requested once the component is displayed.
        setFocusable(true);
        addKeyListener(this);
        SwingUtilities.invokeLater(this::requestFocusInWindow);

        // Update and repaint approximately every 16 ms.
        timer = new Timer(16, e -> {
            updateGame();
            repaint();
        });
        timer.start();
    }

    /**
     * Generates the track and creates the karts.
     *
     * <p>The track is sized to the panel, so initialization is deferred until the panel has
     * been laid out. The method runs once; subsequent calls return immediately.</p>
     */
    private void initTrack() {
        // Already initialized, or the panel has not been laid out yet.
        if (raceTrack != null || getWidth() == 0) {
            return;
        }
        int w = getWidth();
        int h = getHeight();

        // Generate the track outline and derive its centerline polyline.
        int[][] pts = track.generateRandomPoints(w, h);
        raceTrack = track.drawSmoothHull(pts);
        buildCenterline();

        // Start position: the point where the track path begins, heading along the track.
        int n = pts.length;
        double sx = (pts[n - 1][0] + pts[0][0]) / 2.0;
        double sy = (pts[n - 1][1] + pts[0][1]) / 2.0;
        double angle = Math.atan2(pts[0][1] - sy, pts[0][0] - sx);

        // Unit vector perpendicular to the heading, used to place the karts side by side.
        Vector2 side = new Vector2(angle).getLeftHandOrthogonalVector();

        // Convert the start position from pixels to meters.
        double mx = sx / SCALE;
        double my = sy / SCALE;

        // Place the karts 1.4 m to either side of the centerline.
        car = new Car(mx - side.x * 1.4, my - side.y * 1.4, angle, Color.RED);
        bot = new Car(mx + side.x * 1.4, my + side.y * 1.4, angle, Color.BLUE);
        world.addBody(car.body);
        world.addBody(bot.body);

        // Reset the time reference so the first step does not include setup time.
        lastTime = System.nanoTime();
    }

    /**
     * Converts the track curve into a closed polyline of straight segments, in meters, and
     * stores it in {@code center}. Curved segments are flattened to within 0.5 px of the
     * true curve.
     */
    private void buildCenterline() {
        List<double[]> list = new ArrayList<>();

        // Flatten the curves; the maximum deviation from the true curve is 0.5 px.
        PathIterator it = raceTrack.getPathIterator(null, 0.5);
        double[] c = new double[6];
        while (!it.isDone()) {
            int type = it.currentSegment(c);
            if (type == PathIterator.SEG_MOVETO || type == PathIterator.SEG_LINETO) {
                // Convert from pixels to meters.
                double x = c[0] / SCALE;
                double y = c[1] / SCALE;

                // Skip consecutive duplicate points to avoid zero-length segments.
                if (list.isEmpty()
                        || Math.hypot(x - list.get(list.size() - 1)[0], y - list.get(list.size() - 1)[1]) > 0.01) {
                    list.add(new double[]{x, y});
                }
            }
            it.next();
        }
        center = list.toArray(new double[0][]);
    }

    /**
     * Finds the point on the track centerline that is closest to a given position.
     *
     * @param px x coordinate of the query position, in meters
     * @param py y coordinate of the query position, in meters
     * @return an array {distance, x, y}: the distance from the query position to the
     *         centerline and the coordinates of the closest centerline point, all in meters
     */
    private double[] nearestOnCenter(double px, double py) {
        // Squared distances are compared to avoid square roots inside the loop.
        double best = Double.MAX_VALUE;
        double bx = 0;
        double by = 0;
        int n = center.length;
        for (int i = 0; i < n; i++) {
            // Segment from a to b; the last segment connects back to the first point.
            double[] a = center[i];
            double[] b = center[(i + 1) % n];
            double abx = b[0] - a[0];
            double aby = b[1] - a[1];
            double len2 = abx * abx + aby * aby;

            // Projection of the query point onto the segment: 0 at a, 1 at b.
            double t = len2 == 0 ? 0 : ((px - a[0]) * abx + (py - a[1]) * aby) / len2;

            // Clamp to the segment so the result lies between its end points.
            t = Math.max(0, Math.min(1, t));

            // Closest point on this segment and its squared distance to the query point.
            double qx = a[0] + t * abx;
            double qy = a[1] + t * aby;
            double d2 = (px - qx) * (px - qx) + (py - qy) * (py - qy);

            // Keep the nearest candidate found so far.
            if (d2 < best) {
                best = d2;
                bx = qx;
                by = qy;
            }
        }
        return new double[]{Math.sqrt(best), bx, by};
    }

    /**
     * Keeps a kart within the road boundary.
     *
     * <p>Each of the four body corners is tested against the road. A corner outside the
     * road is first moved back onto it, then handled as a contact with an immovable
     * surface: an impulse is applied at the corner that reverses its outward motion
     * according to {@code WALL_BOUNCE}. Because the impulse acts at the corner rather than
     * the center of mass, it also induces rotation.</p>
     *
     * <p>Call once per physics step, after {@code World.step()}.</p>
     *
     * @param c the kart to constrain
     */
    private void keepOnTrack(Car c) {
        Body b = c.body;

        // Kart corners in body-local coordinates.
        double hl = Car.LENGTH / 2;
        double hw = Car.WIDTH / 2;
        double[][] corners = {{hl, hw}, {hl, -hw}, {-hl, hw}, {-hl, -hw}};
        for (double[] k : corners) {
            // Corner position in world coordinates, in meters.
            Vector2 w = b.getTransform().getTransformed(new Vector2(k[0], k[1]));

            // Penetration depth: how far the corner is beyond the road boundary.
            double[] near = nearestOnCenter(w.x, w.y);
            double excess = near[0] - HALF_ROAD;
            if (excess <= 0) {
                // The corner is on the road; no contact.
                continue;
            }

            // Contact normal: unit vector from the corner back toward the road.
            Vector2 n = new Vector2(near[1] - w.x, near[2] - w.y);
            n.normalize();

            // Positional correction: remove the penetration.
            b.translate(n.x * excess, n.y * excess);

            // Lever arm from the center of mass to the corner (rotation only, no translation).
            Vector2 r = b.getTransform().getTransformedR(new Vector2(k[0], k[1]));

            // Velocity of the corner = linear velocity + angular velocity x lever arm.
            Vector2 v = b.getLinearVelocity();
            double om = b.getAngularVelocity();

            // Corner speed along the contact normal; negative means it is moving off the road.
            double vnc = (v.x - om * r.y) * n.x + (v.y + om * r.x) * n.y;
            if (vnc < 0) {
                // Effective inverse mass for an impulse applied at an offset from the center of mass.
                double rn = r.cross(n);
                double invEff = 1.0 / b.getMass().getMass() + rn * rn / b.getMass().getInertia();

                // Impulse magnitude from Newton's law of restitution.
                double j = -(1 + WALL_BOUNCE) * vnc / invEff;

                // Apply the impulse at the corner, which changes both velocity and rotation.
                b.applyImpulse(new Vector2(n.x * j, n.y * j), b.getWorldCenter().sum(r));
            }
        }
    }

    /**
     * Advances the simulation using a fixed timestep.
     *
     * <p>Elapsed real time is accumulated and consumed in steps of {@code DT}, so the
     * simulation behaves identically regardless of the frame rate. The elapsed time per
     * update is capped to avoid a burst of catch-up steps after a stall.</p>
     */
    private void updateGame() {
        // Create the track once the panel has been laid out.
        initTrack();

        // Nothing to simulate until the karts exist.
        if (car == null) {
            return;
        }

        // Accumulate elapsed time, capped at 0.1 s.
        long now = System.nanoTime();
        accumulator += Math.min((now - lastTime) / 1e9, 0.1);
        lastTime = now;

        // Consume the accumulated time in fixed-size steps.
        while (accumulator >= DT) {
            physicsStep();
            accumulator -= DT;
        }
    }

    /**
     * Performs one fixed-duration simulation step: applies the player's input, advances the
     * physics world, then enforces the road boundary.
     */
    private void physicsStep() {
        // Translate the key state into driver inputs for the player's kart.
        car.throttle = (upPressed ? 1 : 0) - (downPressed ? 1 : 0);
        car.steer = (rightPressed ? 1 : 0) - (leftPressed ? 1 : 0);
        car.drifting = driftPressed;

        // Apply control forces. The second kart receives no driver input and is moved
        // only by the physics engine.
        car.applyControls();
        bot.applyControls();

        // Advance the world by one step: integration and kart-to-kart collisions.
        world.step(1);

        // Enforce the road boundary after the engine has moved the bodies.
        keepOnTrack(car);
        keepOnTrack(bot);
    }

    /**
     * Renders the grass background, the road, both karts and the speed readout.
     *
     * @param g the graphics context supplied by Swing
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Background (grass).
        g.setColor(new Color(70, 110, 70));
        g.fillRect(0, 0, getWidth(), getHeight());

        // Initialize here as well in case painting occurs before the first update.
        initTrack();
        if (raceTrack == null) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Road: the track centerline stroked with the road width.
        g2.setStroke(new BasicStroke(ROAD_WIDTH));
        g2.setColor(Color.GRAY);
        g2.draw(raceTrack);

        // Karts.
        car.draw(g2, SCALE);
        bot.draw(g2, SCALE);

        // Speed readout, converted from m/s to km/h.
        g2.setColor(Color.WHITE);
        g2.drawString(String.format("Speed: %.0f km/h", car.getSpeed() * 3.6), 10, 20);
    }

    /**
     * Records that a key has been pressed.
     *
     * @param e the key event
     */
    @Override
    public void keyPressed(KeyEvent e) {
        setKey(e.getKeyCode(), true);
    }

    /**
     * Records that a key has been released.
     *
     * @param e the key event
     */
    @Override
    public void keyReleased(KeyEvent e) {
        setKey(e.getKeyCode(), false);
    }

    /**
     * Updates the state flag associated with a driving key.
     *
     * @param code the key code
     * @param down {@code true} if the key is pressed, {@code false} if it is released
     */
    private void setKey(int code, boolean down) {
        switch (code) {
            case KeyEvent.VK_W -> upPressed = down;
            case KeyEvent.VK_S -> downPressed = down;
            case KeyEvent.VK_A -> leftPressed = down;
            case KeyEvent.VK_D -> rightPressed = down;
            case KeyEvent.VK_SPACE -> driftPressed = down;
            default -> {
                // Other keys are ignored.
            }
        }
    }

    /**
     * Not used; input is handled through {@link #keyPressed} and {@link #keyReleased}.
     *
     * @param e the key event
     */
    @Override
    public void keyTyped(KeyEvent e) {
        // Intentionally empty.
    }
}
