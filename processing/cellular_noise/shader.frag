// Heavily based on the example from Book of Shaders chapter 12
// https://thebookofshaders.com/12/
//
// A million thanks to @patriciogv and the book of shaders editor
// https://thebookofshaders.com/edit.php
//
// Technically I edited it a bit but I'm still gonna give credit to Patricio because
// I didn't do most of the work on this, I just edited a few params.
//
// Author: @patriciogv
// Title: CellularNoise

#ifdef GL_ES
precision mediump float;
#endif

uniform vec2 u_resolution;

vec2 random2( vec2 p ) {
    return fract(sin(vec2(dot(p,vec2(127.1,311.7)),dot(p,vec2(269.5,183.3))))*43758.5453);
}

void main() {
    vec2 st = gl_FragCoord.xy/u_resolution.xy;
    st.x *= u_resolution.x/u_resolution.y;
    vec3 color = vec3(.0);

    // Scale
    st *= 10.;
    float scale = pow(0.26,
        (0.1/atan(st.x, st.y)) - sin(pow(st.y,(7.+st.x)/8.)) - cos(pow(st.x, (30.+st.x)/21.))
    );
    st += pow(1.2, scale);
    

    // Tile the space
    vec2 i_st = floor(st);
    vec2 f_st = fract(st);

    float m_dist = 1.;  // minimum distance

    for (int y= -1; y <= 1; y++) {
        for (int x= -1; x <= 1; x++) {
            // Neighbor place in the grid
            vec2 neighbor = vec2(float(x),float(y));

            // Random position from current + neighbor place in the grid
            vec2 point = random2(i_st + neighbor);

            // Vector between the pixel and the point
            vec2 diff = neighbor + point - f_st;

            // Distance to the point
            float dist = length(diff);

            // Keep the closer distance
            m_dist = min(m_dist, dist);
        }
    }

    // Draw the min distance (distance field)
    color += vec3(m_dist * 1.32, m_dist * 1.3, m_dist * 1.28);

    // Show isolines
    // This is a pretty interesting addition in some cases but it can also be really noisy
    // color -= step(.7,abs(sin(27.0*m_dist)))*.6;

    gl_FragColor = vec4(color,1.0);
}
