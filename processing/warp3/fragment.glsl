// Very similar to warp except uses a much simpler noise function that doesn't rely on random
// values as much. Doesn't really need to rely on random values at all!
//
// Play with live version: https://www.shadertoy.com/view/ttlyRs
//
// Modified from: https://www.shadertoy.com/view/4s23zz
// Original author credit:
//   Created by inigo quilez - iq/2013
//   License Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.
//   See http://www.iquilezles.org/www/articles/warp/warp.htm for details
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
uniform sampler2D texture;

varying vec4 vertColor;
varying vec4 vertTexCoord;

// increasing MULTIPASS will decrease aliasing
#define MULTIPASS 1
#define SEED 1146.942
#define BRIGHTNESS 2.729

// high NOISE_SCALE will have very noisey ripples
#define NOISE_SCALE 04.202
#define FREQUENCY 1.150

// higher is more turbulent
#define SCALE1 0.19
#define SCALE2 0.358

// No, this is not precisely contrast, but it's representative
#define CONTRAST 0.506
#define AMPLITUDE 0.425

// low is zoomed in, high is zoomed out
#define ZOOM 0.97

#define HIGHLIGHTS 1.150
#define LOWLIGHTS 0.5

// should range between approx 0.01 and 0.3
#define ROTATION 0.01


/*
 static.frag
 by Spatial
 05 July 2013
 Source: https://stackoverflow.com/a/17479300/3991555
*/
uint hash( uint x ) { x += ( x << 10u ); x ^= ( x >> 6u ); x += ( x << 3u ); x ^= ( x >> 11u ); x += ( x << 15u ); return x; }
float floatConstruct( uint m ) { const uint ieeeMantissa = 0x007FFFFFu; const uint ieeeOne = 0x3F800000u; m &= ieeeMantissa; m |= ieeeOne; float f = uintBitsToFloat( m ); return f - 1.0; }
float random( float x ) { return floatConstruct(hash(floatBitsToUint(x))); }
float randomRange( float x, float min, float max) { return (random(x) * (max - min)) + min; }

// Very simple fBm implementation based on example from
// https://thebookofshaders.com/13/
float noise (in vec2 st) {
    float amplitude = AMPLITUDE;
    // randomize frequency via a uniform to make this interesting
    float frequency = FREQUENCY;
    float y = sin(st.x * frequency);
    float x = st.x;
    float t = 0.01 * (-iTime / 130.0);

    for (float i = 0.0; i < 4.; i++) {
        float randMix = randomRange(SEED - (i + 1.), 0.25, 0.75);
        float randFreq = randomRange(SEED * (i + 1.), 1.0, 3.0);
        float randOffset = randomRange(pow(SEED, (i + 1.)), 0.4, 5.0);
        float randAmp = randomRange(SEED / (i + 1.), 2.5, 5.5);
        float osc = sin(x * frequency * randFreq + t * randOffset) * randAmp;
        y = mix( y, osc, randMix);
    }

    y *= amplitude;
    return y;
}

const mat2 mtx = mat2( SCALE1,  SCALE2, -SCALE2,  SCALE1 );

float fbm6( vec2 p ) {
    float f = 0.0;

    for (int i = 0; i < 6; i++) {
        f += (1. / (pow(2., float(i) + 1.))) * noise( p );
        p = mtx * p * NOISE_SCALE;
    }

    return f * CONTRAST;
}

vec2 fbm6_2( vec2 p ) {
    float val1 = randomRange(SEED / 3., .4, 7.);
    float val2 = randomRange(SEED / 4., .5, 9.);
    return vec2( fbm6(p+vec2(val1)), fbm6(p+vec2(val2)) );
}


float opaqueMagic( vec2 q, out vec2 o, out vec2 n ) {
    // this color comes from the i-love-u-portland image
    vec4 textColor = texture2D(texture, vertTexCoord.st);

    // shift the distortion over time
    q += ROTATION * sin(vec2(0.11,0.13) * iTime + length( q ) * ROTATION * 50.);

    q *= ZOOM + 0.2 * cos(0.05 * iTime);

    o = 0.5 + 0.5 * fbm6_2( q / textColor.xy );

    o += 0.02 * sin(vec2(0.11,0.13)*iTime*length( o ));

    n = fbm6_2( 4.0 * o * textColor.xy );

    vec2 p = (q + 2.0 * n + 1.0) * textColor.xy * 2.;

    float f = 0.5 + 0.5 * fbm6( 2.0 * p );

    f = mix( f, f*f*f*HIGHLIGHTS, f * abs(n.x) );

    const float lighting = 8.0;
    f *= 1.0 - LOWLIGHTS * pow( 0.5 + 0.5 * sin(lighting * p.x) * sin(lighting * p.y), lighting );

    return f;
}

// processing version, and rename FragColor -> gl_FragColor
void main() {
// shadertoy version, and rename gl_FragColor -> FragColor
// void mainImage( out vec4 FragColor, in vec2 doNotUse) {
    vec3 tot = vec3(0.0);
    vec3 color1 = vec3(0.39, 0.41, 0.10);
    vec3 color2 = vec3(0.28, 0.18, 0.40);

    // what an interesting way to code a nested loop!
    // honestly shocked that this syntax compiles and works
    for( int mi=0; mi<MULTIPASS; mi++ )
    for( int ni=0; ni<MULTIPASS; ni++ )
    {
        // pixel coordinates
        vec2 of = vec2(float(mi),float(ni)) / float(MULTIPASS) - 0.5;
        vec2 q = (2.0*(gl_FragCoord.xy+of)-iResolution.xy)/iResolution.y;

        vec2 o, n;
        float f = opaqueMagic(q, o, n);

        vec3 col = vec3(1. / BRIGHTNESS);
        col = mix( col, color1, f );
        col = mix( col, vec3(0.77,0.77,0.77), dot(n,n) );
        col *= f * BRIGHTNESS;

        vec2 ex = vec2( 1.0 / iResolution.x / float(MULTIPASS), 0.0 );
        vec2 ey = vec2( 0.0, 1.0 / iResolution.y / float(MULTIPASS));
        vec3 nor = normalize( vec3( opaqueMagic(q+ex, o, n) - f, ex.x, opaqueMagic(q+ey, o, n) - f ) );

        vec3 lig = normalize( vec3( 0.9, -0.2, -0.4 ) );
        float dif = clamp( 0.3 + 0.7*dot( nor, lig ), 0.0, 1.0 );

        vec3 tint = color2*(nor.y*0.5+0.5);
        // this color comes from the i-love-u-portland image
        vec4 textColor = texture2D(texture, vertTexCoord.st);
        tint += textColor.xyz * 0.25;

        col *= tint;
        col += col*col;
        col *= BRIGHTNESS;

        tot += col;
    }

    tot /= float(MULTIPASS*MULTIPASS);

    gl_FragColor = vec4( tot , 1.0 );
}
