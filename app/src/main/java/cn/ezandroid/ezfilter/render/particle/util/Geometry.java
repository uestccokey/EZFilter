package cn.ezandroid.ezfilter.render.particle.util;

public class Geometry {

    public static class Point {
        public final float x, y, z;

        public Point(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Point translate(Vector vector) {
            return new Point(x + vector.x,
                    y + vector.y,
                    z + vector.z);
        }
    }

    public static class Vector {
        public float x, y, z;

        public Vector(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public float length() {
            return (float) Math.sqrt(x * x + y * y + z * z);
        }

        public Vector crossProduct(Vector other) {
            return new Vector((y * other.z) - (z * other.y),
                    (z * other.x) - (x * other.z),
                    (x * other.y) - (y * other.x));
        }

        public float dotProduct(Vector other) {
            return x * other.x +
                    y * other.y +
                    z * other.z;
        }

        public Vector scale(float f) {
            return new Vector(x * f,
                    y * f,
                    z * f);
        }
    }

    public static Vector vectorBetween(Point from, Point to) {
        return new Vector(to.x - from.x,
                to.y - from.y,
                to.z - from.z);
    }
}
