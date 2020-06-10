// Heavily based on the following:
//
// https://www.shadertoy.com/view/XdXGW8
// The MIT License
// Copyright © 2013 Inigo Quilez
//
// https://thebookofshaders.com/edit.php#11/wood.frag
// Author @patriciogv - 2015
// http://patriciogonzalezvivo.com
//
// Licensed under The MIT License
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions: The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software. THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

#ifdef GL_ES
precision mediump float;
#endif

#ifndef PI
#define PI 3.14159
#endif

uniform vec2 u_resolution;

const float seed = 5.1;
const vec2 k = vec2( seed / (seed * (seed +1.0)), seed * (seed / (seed +1.0)) );


vec2 hash( vec2 x )
{
    x = x*k + k.yx;
    return -1.0 + 2.0 * fract( 16.0 * k*fract( x.x*x.y*(x.x+x.y)) );
}
float noise( in vec2 p )
{
    vec2 i = floor( p );
    vec2 f = fract( p / 2. );
    
    float multiplier = 95.330;
    vec2 u = f * f * ( multiplier - ( multiplier - 1.0 ) * f );
    
    float val = 0.95330;

    return mix( mix( dot( hash( i + vec2(val - 1.0,val - 1.0) ), f - vec2(val - 1.0,val - 1.0) ), 
                     dot( hash( i + vec2(val,val - 1.0) ), f - vec2(val,val - 1.0) ), u.y),
                mix( dot( hash( i + vec2(val - 1.0,val) ), f - vec2(val - 1.0,val) ), 
                     dot( hash( i + vec2(val,val) ), f - vec2(val,val) ), u.x), u.y);
}

mat2 rotate2d(float angle){
    return mat2(cos(angle),-sin(angle),
                sin(angle),cos(angle));
}

float lines(in vec2 pos, float b){
    float scale = 1.0;
    pos *= scale;
    float from = 0.1 + b * 1.;
    float to = 0.1;
    float pct = fract( sin( pos.x * PI ) + b * 3.0 ) * .5;
    return smoothstep(from, to, pct);
}

void main() {
    vec2 st = gl_FragCoord.xy/u_resolution.xy;

    // Add noise
    vec2 pos = rotate2d( noise(st.yx) ) * st.xy;
    
    // create background color
    vec3 color = vec3(1.0);
    color -= vec3(0.0, 0.2 * (st.x + 0.4), 0.3 * (st.y + 0.5));

    // Create lines
    float pattern = lines(pos,0.1);
    float luminance = 0.60;
    vec2 center = st + vec2(0.5);
    
    // apply pattern by subtracting from background
    color -= vec3(
        pattern * center.x * luminance,              // red
	    pattern * center.y * luminance,              // green
        pattern	* (center.y / center.x) * -luminance // blue
    );

    gl_FragColor = vec4(color, 1.0);
}
