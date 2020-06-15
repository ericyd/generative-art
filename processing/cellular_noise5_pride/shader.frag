// Voronoi logic taken from
//   https://thebookofshaders.com/edit.php#12/vorono-01.frag
//   Author: @patriciogv
//   Title: Simple Voronoi
//
// This 

#ifdef GL_ES
precision mediump float;
#endif

uniform vec2 u_resolution;
uniform float u_time;

vec2 random2( vec2 p ) {
    return fract(sin(vec2(dot(p,vec2(127.1,311.7)),dot(p,vec2(269.5,183.3))))*43758.5453);
}

//  Function from Iñigo Quiles
//  www.iquilezles.org/www/articles/functions/functions.htm
float cubicPulse( float c, float w, float x ){
    x = abs(x - c);
    if( x>w ) return 0.0;
    x /= w;
    return 1.0 - x*x*(3.0-2.0*x);
}

// credit: https://gist.github.com/yiwenl/745bfea7f04c456e0101#file-hsv2rgb
vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void main() {
    vec2 st = gl_FragCoord.xy/u_resolution.xy;
    vec2 toCenter = st - vec2(.5,.5);
    
    // set up grid: convert cartesian to polar coordinates
    st -= vec2(0.5);
    float r = sqrt(st.x * st.x + st.y * st.y);
    float theta = atan(st.y, st.x);
    st = vec2(r, theta);
    
    // reshape slightly
    // comment this out for a more "hyperspace" vibe
    st.x += cubicPulse(st.x, 0.5, 0.5);
    
    // scale up
    st *= 9.;
    
    // Tile the space
    vec2 i_st = floor(st);
    vec2 f_st = fract(st);

    // Get minimum distance and point to create voronoi pattern
    float minDist = 10.;  // minimum distance
    vec2 minPoint;        // minimum point

    for (int j=-1; j<=1; j++ ) {
        for (int i=-1; i<=1; i++ ) {
            vec2 neighbor = vec2(float(i),float(j));
            vec2 point = random2(i_st + neighbor);
            point = 0.5 + 0.5*sin(u_time + 6.2831*point);
            vec2 diff = neighbor + point - f_st;
            float dist = length(diff);

            if( dist < minDist ) {
                minDist = dist;
                minPoint = point;
            }
        }
    }

    // apply min dist/point to color
    vec3 color = vec3(0.0);
    
    // Assign a color using the closest point position
    color += dot(minPoint,vec2(.3,.6)) * 0.25;

    // Add distance field to closest point center
    color += minDist * 0.6;
    
    // rainbow-ize
    color += hsv2rgb(vec3(length(toCenter*1.6), 0.7, 0.67));

    gl_FragColor = vec4(color,1.0);
}
