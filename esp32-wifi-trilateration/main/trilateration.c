typedef struct {
    double x;
    double y;
    double r;
} pos_vec;


/*
The following function calculates the position of a point
given its distance from three points p1, p2 and p3, where the distances
to the three points are r1, r2 and r3 respectively.

Construct a circle at each point p_i with radius r_i.
Any two circles will intersect at two distinct points.
A line passes through these two intersection points.
Now, using the centre and radius of the third circle, a unique point
on the line could be determined.
*/
pos_vec trilaterate(pos_vec p1, pos_vec p2, pos_vec p3) {
    double u = (p2.r * p2.r);
    double v = (p2.x * p2.x);

    double a1 = 2.0 * (p2.x - p1.x);
    double b1 = 2.0 * (p2.y - p1.y);
    double c1 =
        (p1.r * p1.r) - u - (p1.x * p1.x) + v - (p1.y * p1.y) + (p2.y * p2.y);
    double a2 = 2.0 * (p3.x - p2.x);
    double b2 = 2.0 * (p3.y - p2.y);
    double c2 =
        u - (p3.r * p3.r) - v + (p3.x * p3.x) - (p2.y * p2.y) + (p3.y * p3.y);

    double d = (a1 * b2 - a2 * b1);

    double x = (c1 * b2 - b1 * c2) / d;
    double y = (a1 * c2 - a2 * c1) / d;

    return (pos_vec){.x = x, .y = y, .r = 0.0f};
}