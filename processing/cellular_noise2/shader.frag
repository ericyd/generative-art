// Author: @patriciogv
// Title: Simple Voronoi

#ifdef GL_ES
precision mediump float;
#endif

uniform vec2 u_resolution;

const float u_time = 5.;

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

void main() {
    vec2 st = gl_FragCoord.xy/u_resolution.xy;
    st.x *= u_resolution.x/u_resolution.y;
    vec3 color = vec3(.0);

    // warp domain
    vec2 copy = st;
    st.x = cubicPulse(st.x, 0.95, 0.665);
    st.x -= cubicPulse(cos(st.x), sin(st.x), 0.9);
    
    st.y += sin(st.x*.5)*.35;
    st.y -= cubicPulse(cos((copy.x+0.25) * 5. + 1.30), st.y * 5. , .95);
    st.y += cubicPulse(sin(st.x), cos(st.y), 0.85);
    st.y += cubicPulse(cos(st.x)*.5, cos(st.y*10.), 0.885);
    
    st.y *= 4.;
    
    st.x += sin(st.y * 7.) * 2. + 2.;
    st.y += 4.0+cos(6.0-st.y*4.) * 4.;
    
    st *= 3.5;

    // Tile the space
    vec2 i_st = floor(st);
    vec2 f_st = fract(st);

    float m_dist = 10.;  // minimum distance
    vec2 m_point;        // minimum point

    for (int j=-1; j<=1; j++ ) {
        for (int i=-1; i<=1; i++ ) {
            vec2 neighbor = vec2(float(i),float(j));
            vec2 point = random2(i_st + neighbor);
            point = 0.5 + 0.5*sin(u_time/10. + 6.2831*point);
            vec2 diff = neighbor + point - f_st;
            float dist = length(diff);

            if( dist < m_dist ) {
                m_dist = dist;
                m_point = point;
            }
        }
    }

    // Assign a color using the closest point position
    color += dot(m_point,vec2(.4,.7)) * 0.995;
    color.b += m_dist*0.4;
    color.g += m_dist*0.2;

    gl_FragColor = vec4(color,1.0);
}
