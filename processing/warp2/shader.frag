// Very similar to warp except uses a much simpler noise function that doesn't rely on random
// values as much. Doesn't actually need to rely on random values at all!
//
// Modified from: https://www.shadertoy.com/view/4s23zz
// Original author credit:
//   Created by inigo quilez - iq/2013
//   License Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.
//   See http://www.iquilezles.org/www/articles/warp/warp.htm for details
//
//
// The article linked above describes it much better than I ever could.
// All I really did was take the original example and refactor certain parts so I
// could understand what various magic numbers did.

// uncomment for Processing
#ifdef GL_ES
precision mediump float;
#endif
uniform vec2 iResolution;
uniform float iTime;
uniform float iSeed;

// increasing OVERSAMPLE will decrease aliasing
#define OVERSAMPLE 2
#define SEED 12146.942
#define BRIGHTNESS 2.55950

// high NOISE_SCALE will have very noisey ripples
#define NOISE_SCALE 1.810

// low COLOR_VARIABILITY will have more of the same tones, less highlights
#define COLOR_VARIABILITY 0.609803

// No, this is not precisely contrast, but it's representative
#define CONTRAST 01.2509

// higher is more turbulent
#define SCALE1 0.43
#define SCALE2 0.872

// low is zoomed in, high is zoomed out
#define ZOOM 0.97

#define HIGHLIGHTS 2.005
#define LOWLIGHTS 0.5


//uint(iTime)
#define MY_VAL random2(SEED)



/*
 static.frag
 by Spatial
 05 July 2013
 Source: https://stackoverflow.com/a/17479300/3991555
*/
uint hash( uint x ) { x += ( x << 10u ); x ^= ( x >> 6u ); x += ( x << 3u ); x ^= ( x >> 11u ); x += ( x << 15u ); return x; }
uint hash( uvec2 v ) { return hash( v.x ^ hash(v.y) ); }
uint hash( uvec3 v ) { return hash( v.x ^ hash(v.y) ^ hash(v.z) ); }
uint hash( uvec4 v ) { return hash( v.x ^ hash(v.y) ^ hash(v.z) ^ hash(v.w) ); }
float floatConstruct( uint m ) { const uint ieeeMantissa = 0x007FFFFFu; const uint ieeeOne = 0x3F800000u; m &= ieeeMantissa; m |= ieeeOne; float f = uintBitsToFloat( m ); return f - 1.0; }
float random( float x ) { return floatConstruct(hash(floatBitsToUint(x))); }
float random( vec2 v ) { return floatConstruct(hash(floatBitsToUint(v))); }
float random( vec3 v ) { return floatConstruct(hash(floatBitsToUint(v))); }
float random( vec4 v ) { return floatConstruct(hash(floatBitsToUint(v))); }
float randomRange( float x, float min, float max) { return (random(x) * (max - min)) + min; }

// Very simple fBm implementation based on example from
// https://thebookofshaders.com/13/
float noise (in vec2 st) {
    float amplitude = 0.5;
    float frequency = randomRange(iSeed, 1.5, 3.0);
    float y = sin(st.x * frequency);
    float x = st.x;
    float t = 0.01 * (-iTime / 130.0);
    y += sin(x * frequency*2.1 + t) * 4.5;
    y += sin(x * frequency*1.72 + t * 1.121) * 4.0;
    y += sin(x * frequency*2.221 + t * 0.437) * 5.0;
    y += sin(x * frequency*3.1122+ t * 4.269) * 2.5;
    y *= amplitude * 0.06;
    return y;
}



const mat2 mtx = mat2( SCALE1,  SCALE2, -SCALE2,  SCALE1 );

float fbm4( vec2 p )
{
    float f = 0.0;

    for (int i = 0; i < 4; i++) {
        f += (1. / (pow(2., float(i) + 1.))) * (noise( p ) * 2. - 1.);
        p = mtx * p * NOISE_SCALE;
    }

    return f * COLOR_VARIABILITY;
}

float fbm6( vec2 p )
{
    float f = 0.0;
    
    for (int i = 0; i < 6; i++) {
        f += (1. / (pow(2., float(i) + 1.))) * noise( p );
        p = mtx * p * NOISE_SCALE;
    }

    return f * CONTRAST;
}

//const float one = random(1.1);
//const float two = random(1.2);
const float one = 0.19;  
const float two = 1.2;
vec2 fbm4_2( vec2 p )
{
    return vec2( fbm4(p+vec2(one)), fbm4(p+vec2(one)) );
}

//const float three = randomRange(2.4, 03.1, 8.0);
//const float four = randomRange(4.6, 3., 8.0);
const float three = 5.4;
const float four = 7.8;
vec2 fbm6_2( vec2 p )
{
    return vec2( fbm6(p+vec2(three)), fbm6(p+vec2(four)) );
}


float func( vec2 q, out vec2 o, out vec2 n )
{
    q += 0.05*sin(vec2(0.11,0.13) * iTime + length( q ) * 4.0);
    
    q *= ZOOM + 0.2 * cos(0.05 * iTime);

    o = 0.5 + 0.5 * fbm4_2( q );
    
    o += 0.02 * sin(vec2(0.11,0.13)*iTime*length( o ));

    n = fbm6_2( 4.0 * o );

    vec2 p = q + 2.0 * n + 1.0;

    float f = 0.5 + 0.5 * fbm4( 2.0 * p );

    f = mix( f, f*f*f*HIGHLIGHTS, f * abs(n.x) );

    const float lighting = 8.0;
    f *= 1.0 - LOWLIGHTS * pow( 0.5 + 0.5 * sin(lighting * p.x) * sin(lighting * p.y), lighting );

    return f;
}

float funcs( in vec2 q )
{
    vec2 t1, t2;
    return func(q,t1,t2);
}

void main() // processing version, and rename Frag.* -> gl_Frag.*
// void mainImage( out vec4 gl_FragColor, in vec2 gl_FragCoord) // shadertoy version
{
    vec3 tot = vec3(0.0);
    
    // what an interesting way to code a nested loop!
    // honestly shocked that this syntax compiles and works
    for( int mi=0; mi<OVERSAMPLE; mi++ )
    for( int ni=0; ni<OVERSAMPLE; ni++ )
    {
        // pixel coordinates
        vec2 of = vec2(float(mi),float(ni)) / float(OVERSAMPLE) - 0.5;
        vec2 q = (2.0*(gl_FragCoord.xy+of)-iResolution.xy)/iResolution.y;

        vec2 o, n;
        float f = func(q, o, n);
        
        vec3 col = vec3(1. / BRIGHTNESS);
        col = mix( col, vec3(0.53,0.15,0.05), f );
        col = mix( col, vec3(0.77,0.77,0.77), dot(n,n) );
        col *= f * BRIGHTNESS;

        vec2 ex = vec2( 1.0 / iResolution.x / float(OVERSAMPLE), 0.0 );
        vec2 ey = vec2( 0.0, 1.0 / iResolution.y / float(OVERSAMPLE));
        vec3 nor = normalize( vec3( funcs(q+ex) - f, ex.x, funcs(q+ey) - f ) );
        
        vec3 lig = normalize( vec3( 0.9, -0.2, -0.4 ) );
        float dif = clamp( 0.3 + 0.7*dot( nor, lig ), 0.0, 1.0 );

        vec3 tint = vec3(0.835,0.80,0.855)*(nor.y*0.5+0.5);

        col *= tint;
        col += col*col;
        col *= BRIGHTNESS * 0.8;
        
        tot += col;
    }
    
    tot /= float(OVERSAMPLE*OVERSAMPLE);

    
    vec2 p = gl_FragCoord.xy / iResolution.xy;
    tot *= BRIGHTNESS - (BRIGHTNESS / 2.) * sqrt(16.0*p.x*p.y*(1.0-p.x)*(1.0-p.y));
    
    gl_FragColor = vec4( tot, 1.0 );
}
