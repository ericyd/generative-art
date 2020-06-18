// Author: @patriciogv
// Title: 4 cells DF

#ifdef GL_ES
precision mediump float;
#endif

#ifndef PI
#define PI 3.141596
#endif

uniform vec2 u_resolution;
uniform vec2 u_mouse;
uniform float u_time;

const int nPoints = 60;

float map(float val, float inMin, float inMax, float outMin, float outMax) {
    return ((val - inMin) / (inMax - inMin) * (outMax - outMin) + outMin);
}

float map(int val, int inMin, int inMax, float outMin, float outMax) {
    return ((float(val) - float(inMin)) / (float(inMax) - float(inMin)) * (outMax - outMin) + outMin);
}

float deg2rad(float degrees) {
    return degrees / 180. * PI;
}

// credit: https://gist.github.com/yiwenl/745bfea7f04c456e0101#file-hsv2rgb
vec3 hsv2rgb(vec3 c)
{
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void main() {
    vec2 st = gl_FragCoord.xy/u_resolution.xy;
    st.x *= u_resolution.x/u_resolution.y;

    vec3 color = vec3(.0);

    // Cell positions
    vec2 point[nPoints];
    int nSections = 5;
    for (int i = 0; i < nPoints; i++) {
        float radius = .1;
	    float offsetDegrees = 0.;
        if (i < nPoints / nSections) {
            radius = 0.8;
            offsetDegrees = 2.;
        } else if (i < nPoints * 2 / nSections) {
            radius = 0.9;
            offsetDegrees = 10.;
        } else if (i < nPoints * 3 / nSections) {
            radius = 0.8;
            offsetDegrees = 18.;
        } else if (i < nPoints * 4 / nSections) {
            radius = 0.4;
            offsetDegrees = 15.;
        }
        float pct = map(i, 0, nPoints / nSections - 1, 0. + offsetDegrees, 360. + offsetDegrees);
        float angle = deg2rad(pct);
        float x = map(sin(angle), -1., 1., 0., 1.) * radius + ((1.-radius) / 2.);
        float y = map(cos(angle), -1., 1., 0., 1.) * radius + ((1.-radius) / 2.);
        point[i] = vec2(x, y);
    }

    float minDist = 1.;  // minimum distance

    // Iterate through the points positions
    for (int i = 0; i < nPoints; i++) {
        float dist = distance(st, point[i]);

        // Keep the closer distance
        minDist = min(minDist, dist);
    }

    // Draw the min distance (distance field)
    color += hsv2rgb(vec3(minDist*2., 0.5, 0.7));
    
    // draw points
    color += 1.-step(.003, minDist);

    // Show isolines
    color -= step(.26997,abs(sin(140.0*minDist)))*.13;

    gl_FragColor = vec4(color,1.0);
}