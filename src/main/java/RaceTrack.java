import java.util.ArrayList;
import java.awt.geom.Path2D;

/**
 * Generates random closed race tracks.
 *
 * <p>A track is built by sampling random points, taking their convex hull, and smoothing
 * the hull into a closed curve made of quadratic Bezier segments. This produces rounded
 * corners and a convex overall shape.</p>
 */
public class RaceTrack {

    /**
     * Generates the control points of a new track.
     *
     * <p>Random points are sampled inside the given area, leaving a margin at the edges, and
     * reduced to their convex hull. The process repeats until the hull covers enough of the
     * area to produce a usable track.</p>
     *
     * @param width  available width, in pixels
     * @param height available height, in pixels
     * @return the hull vertices in order, as {x, y} pixel coordinates
     */
    public int[][] generateRandomPoints(int width, int height) {

        int[][] hullPoints;

        // Resample until the hull is large enough.
        do {
            int[][] randomPoints = new int[10][2];

            // Keep the points away from the edges of the available area.
            int margin = 50;

            // Sample 10 uniformly distributed points.
            for (int i = 0; i < 10; i++) {

                int x = margin + (int) (Math.random() * (width - 2 * margin));
                int y = margin + (int) (Math.random() * (height - 2 * margin));

                randomPoints[i][0] = x;
                randomPoints[i][1] = y;
            }

            hullPoints = convexHull(randomPoints);

        } while (!isTrackLargeEnough(hullPoints, width, height));
        return hullPoints;
    }

    /**
     * Computes the convex hull of a set of points using the gift-wrapping (Jarvis march)
     * algorithm.
     *
     * @param points the input points, as {x, y} pixel coordinates
     * @return the hull vertices in order, as {x, y} pixel coordinates
     */
    public int[][] convexHull(int[][] points) {
        int n = points.length;

        ArrayList<int[]> hull = new ArrayList<>();

        // Start from the leftmost point, which always lies on the hull.
        int leftmost = 0;

        for (int i = 1; i < n; i++) {
            if (points[i][0] < points[leftmost][0]) {
                leftmost = i;
            }
        }

        int current = leftmost;

        do {
            hull.add(points[current]);

            // Initial candidate for the next hull vertex.
            int next = (current + 1) % n;

            for (int i = 0; i < n; i++) {

                if (i == current) {
                    continue;
                }

                // Cross product of (next - current) and (i - current). Its sign shows on which
                // side of the line current-next point i lies; a negative value makes point i
                // the new candidate.
                int cross = (points[next][0] - points[current][0]) * (points[i][1] - points[current][1])
                        - (points[next][1] - points[current][1]) * (points[i][0] - points[current][0]);

                if (cross < 0) {
                    next = i;
                }
            }

            current = next;

        } while (current != leftmost);

        // Copy the hull vertices into an array.
        int[][] result = new int[hull.size()][2];

        for (int i = 0; i < hull.size(); i++) {
            result[i][0] = hull.get(i)[0];
            result[i][1] = hull.get(i)[1];
        }

        return result;
    }

    /**
     * Checks whether the hull is large enough to make a usable track. The bounding box of
     * the hull must cover at least 80% of the available width and height.
     *
     * @param hullPoints the hull vertices, as {x, y} pixel coordinates
     * @param width      available width, in pixels
     * @param height     available height, in pixels
     * @return {@code true} if the hull is large enough
     */
    public boolean isTrackLargeEnough(int[][] hullPoints, int width, int height) {

        // Compute the bounding box of the hull.
        int minX = hullPoints[0][0];
        int maxX = hullPoints[0][0];

        int minY = hullPoints[0][1];
        int maxY = hullPoints[0][1];

        for (int i = 1; i < hullPoints.length; i++) {

            if (hullPoints[i][0] < minX) {
                minX = hullPoints[i][0];
            }

            if (hullPoints[i][0] > maxX) {
                maxX = hullPoints[i][0];
            }

            if (hullPoints[i][1] < minY) {
                minY = hullPoints[i][1];
            }

            if (hullPoints[i][1] > maxY) {
                maxY = hullPoints[i][1];
            }
        }

        int trackWidth = maxX - minX;
        int trackHeight = maxY - minY;

        // Require the bounding box to cover at least 80% of each dimension.
        return trackWidth >= width * 0.8 && trackHeight >= height * 0.8;
    }

    /**
     * Builds a smooth closed path from the hull vertices.
     *
     * <p>Each vertex is used as the control point of a quadratic Bezier segment, and the
     * segment end points are the midpoints between consecutive vertices. The resulting curve
     * has a continuous tangent and does not pass through the vertices themselves, which
     * rounds the corners.</p>
     *
     * @param points the hull vertices in order, as {x, y} pixel coordinates
     * @return the closed path, in pixel coordinates
     */
    public Path2D.Double drawSmoothHull(int[][] points) {

        Path2D.Double path = new Path2D.Double();

        int n = points.length;

        // Start at the midpoint between the last and first vertices so the loop closes smoothly.
        double startX = (points[n - 1][0] + points[0][0]) / 2.0;
        double startY = (points[n - 1][1] + points[0][1]) / 2.0;

        path.moveTo(startX, startY);

        for (int i = 0; i < n; i++) {
            int next = (i + 1) % n;

            // Midpoint between this vertex and the next one.
            double midX = (points[i][0] + points[next][0]) / 2.0;
            double midY = (points[i][1] + points[next][1]) / 2.0;

            // Curve toward the midpoint, using the current vertex as the control point.
            path.quadTo(points[i][0], points[i][1], midX, midY);
        }

        // Close the loop.
        path.closePath();
        return path;
    }
}
