package com.trilateration.wifi;

public class Trilateration {

    public static final double TX_POWER_DBM = -40.0;
    public static final double PATH_LOSS_N  =  2.7;

    public static class Point {
        public final double x, y;
        public Point(double x, double y) { this.x = x; this.y = y; }

        @Override
        public String toString() {
            return String.format("(%.2f m,  %.2f m)", x, y);
        }
    }

    public static double rssiToDistance(double rssi) {
        return rssiToDistance(rssi, TX_POWER_DBM, PATH_LOSS_N);
    }

    public static double rssiToDistance(double rssi, double txPower, double n) {
        return Math.pow(10.0, (txPower - rssi) / (10.0 * n));
    }

    public static Point compute(
            double x1, double y1, double r1,
            double x2, double y2, double r2,
            double x3, double y3, double r3)
    {
        double A = 2.0 * (x2 - x1);
        double B = 2.0 * (y2 - y1);
        double C = r1*r1 - r2*r2 - x1*x1 + x2*x2 - y1*y1 + y2*y2;

        double D = 2.0 * (x3 - x1);
        double E = 2.0 * (y3 - y1);
        double F = r1*r1 - r3*r3 - x1*x1 + x3*x3 - y1*y1 + y3*y3;

        double det = A * E - B * D;
        if (Math.abs(det) < 1e-9) return null;

        return new Point(
                (C * E - B * F) / det,
                (A * F - C * D) / det
        );
    }
}
