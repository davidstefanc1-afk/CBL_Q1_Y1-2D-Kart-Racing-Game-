/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
import java.util.ArrayList;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.*;
/**
 *
 * @author roire
 */
public class RaceTrack{
    private int[][] hullPoints;
    private Path2D.Double path;
    private Shape roadShape;

    private static final float ROAD_WIDTH = 100f;
    
    public RaceTrack(int width, int height){
    
    }
    
    public int[][] generateRandomPoints(int width, int height){
        int[][] hullPoints;
    do {
            int [][] randomPoints = new int[10][2];
            int margin = 50;

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
    
    public int[][] convexHull(int[][] points) {
        int n = points.length;

        ArrayList<int[]> hull = new ArrayList<>();

        // Find leftmost point
        int leftmost = 0;

        for (int i = 1; i < n; i++) {
            if (points[i][0] < points[leftmost][0]) {
                leftmost = i;
            }
        }

        int current = leftmost;

        do {
            hull.add(points[current]);

            // Pick any other point as initial candidate
            int next = (current + 1) % n;

            for (int i = 0; i < n; i++) {

                if (i == current) {
                    continue;
                }

                int cross = (points[next][0] - points[current][0]) * (points[i][1] - points[current][1])
                        - (points[next][1] - points[current][1]) * (points[i][0] - points[current][0]);

                if (cross < 0) {
                    next = i;
                }
            }

            current = next;

        } while (current != leftmost);

        // Convert ArrayList to int[][]
        int[][] result = new int[hull.size()][2];

        for (int i = 0; i < hull.size(); i++) {
            result[i][0] = hull.get(i)[0];
            result[i][1] = hull.get(i)[1];
        }

        return result;
    } 
    public boolean isTrackLargeEnough(int[][] hullPoints, int width, int height) {

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

        return trackWidth >= width * 0.8 && trackHeight >= height * 0.8;
    }
    
    public Path2D.Double drawSmoothHull(int[][] points) {

        Path2D.Double path = new Path2D.Double();

        int n = points.length;

        // Midpoint between last point and first point
        double startX = (points[n - 1][0] + points[0][0]) / 2.0;
        double startY = (points[n - 1][1] + points[0][1]) / 2.0;

        path.moveTo(startX, startY);

        for (int i = 0; i < n; i++) {
            int next = (i + 1) % n;

            double midX = (points[i][0] + points[next][0]) / 2.0;
            double midY = (points[i][1] + points[next][1]) / 2.0;

            path.quadTo(points[i][0], points[i][1], midX, midY);
        }

        path.closePath();
        return path;
    }
    
}
