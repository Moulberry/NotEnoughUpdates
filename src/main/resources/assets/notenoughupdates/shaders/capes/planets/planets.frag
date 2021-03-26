#version 120

varying vec4 passColour;
varying vec3 passNormal;
varying vec3 passVertex;

uniform sampler2D textureIn;
uniform int millis;
uniform int planetType;
uniform vec3 sunVec;

vec4 permute(vec4 x){return mod(((x*34.0)+1.0)*x, 289.0);}
float permute(float x){return floor(mod(((x*34.0)+1.0)*x, 289.0));}
vec4 taylorInvSqrt(vec4 r){return 1.79284291400159 - 0.85373472095314 * r;}
float taylorInvSqrt(float r){return 1.79284291400159 - 0.85373472095314 * r;}

vec4 grad4(float j, vec4 ip){
  const vec4 ones = vec4(1.0, 1.0, 1.0, -1.0);
  vec4 p,s;

  p.xyz = floor( fract (vec3(j) * ip.xyz) * 7.0) * ip.z - 1.0;
  p.w = 1.5 - dot(abs(p.xyz), ones.xyz);
  s = vec4(lessThan(p, vec4(0.0)));
  p.xyz = p.xyz + (s.xyz*2.0 - 1.0) * s.www;

  return p;
}

float snoise(vec4 v){
  const vec2  C = vec2( 0.138196601125010504,  // (5 - sqrt(5))/20  G4
                        0.309016994374947451); // (sqrt(5) - 1)/4   F4
// First corner
  vec4 i  = floor(v + dot(v, C.yyyy) );
  vec4 x0 = v -   i + dot(i, C.xxxx);

// Other corners

// Rank sorting originally contributed by Bill Licea-Kane, AMD (formerly ATI)
  vec4 i0;

  vec3 isX = step( x0.yzw, x0.xxx );
  vec3 isYZ = step( x0.zww, x0.yyz );
//  i0.x = dot( isX, vec3( 1.0 ) );
  i0.x = isX.x + isX.y + isX.z;
  i0.yzw = 1.0 - isX;

//  i0.y += dot( isYZ.xy, vec2( 1.0 ) );
  i0.y += isYZ.x + isYZ.y;
  i0.zw += 1.0 - isYZ.xy;

  i0.z += isYZ.z;
  i0.w += 1.0 - isYZ.z;

  // i0 now contains the unique values 0,1,2,3 in each channel
  vec4 i3 = clamp( i0, 0.0, 1.0 );
  vec4 i2 = clamp( i0-1.0, 0.0, 1.0 );
  vec4 i1 = clamp( i0-2.0, 0.0, 1.0 );

  //  x0 = x0 - 0.0 + 0.0 * C
  vec4 x1 = x0 - i1 + 1.0 * C.xxxx;
  vec4 x2 = x0 - i2 + 2.0 * C.xxxx;
  vec4 x3 = x0 - i3 + 3.0 * C.xxxx;
  vec4 x4 = x0 - 1.0 + 4.0 * C.xxxx;

// Permutations
  i = mod(i, 289.0);
  float j0 = permute( permute( permute( permute(i.w) + i.z) + i.y) + i.x);
  vec4 j1 = permute( permute( permute( permute (
             i.w + vec4(i1.w, i2.w, i3.w, 1.0 ))
           + i.z + vec4(i1.z, i2.z, i3.z, 1.0 ))
           + i.y + vec4(i1.y, i2.y, i3.y, 1.0 ))
           + i.x + vec4(i1.x, i2.x, i3.x, 1.0 ));
// Gradients
// ( 7*7*6 points uniformly over a cube, mapped onto a 4-octahedron.)
// 7*7*6 = 294, which is close to the ring size 17*17 = 289.

  vec4 ip = vec4(1.0/294.0, 1.0/49.0, 1.0/7.0, 0.0) ;

  vec4 p0 = grad4(j0,   ip);
  vec4 p1 = grad4(j1.x, ip);
  vec4 p2 = grad4(j1.y, ip);
  vec4 p3 = grad4(j1.z, ip);
  vec4 p4 = grad4(j1.w, ip);

// Normalise gradients
  vec4 norm = taylorInvSqrt(vec4(dot(p0,p0), dot(p1,p1), dot(p2, p2), dot(p3,p3)));
  p0 *= norm.x;
  p1 *= norm.y;
  p2 *= norm.z;
  p3 *= norm.w;
  p4 *= taylorInvSqrt(dot(p4,p4));

// Mix contributions from the five corners
  vec3 m0 = max(0.6 - vec3(dot(x0,x0), dot(x1,x1), dot(x2,x2)), 0.0);
  vec2 m1 = max(0.6 - vec2(dot(x3,x3), dot(x4,x4)            ), 0.0);
  m0 = m0 * m0;
  m1 = m1 * m1;
  return 49.0 * ( dot(m0*m0, vec3( dot( p0, x0 ), dot( p1, x1 ), dot( p2, x2 )))
               + dot(m1*m1, vec2( dot( p3, x3 ), dot( p4, x4 ) ) ) ) ;

}

vec3 interp_hsv(vec3 one, vec3 two, float amount) {
  return one + (two - one) * amount;
}

vec3 hsv2rgb(vec3 c) {
  vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
  vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
  return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

vec3 ocean_deep = vec3(245.0/365.0, 82.0/100.0, 67.0/100.0);
vec3 ocean_shallow = vec3(220.0/365.0, 75.0/100.0, 67.0/100.0);
vec3 land_low = vec3(93.0/365.0, 87.0/100.0, 86.0/100.0);
vec3 land_med = vec3(135.0/365.0, 84.0/100.0, 82.0/100.0);
vec3 land_high = vec3(40.0/365.0, 52.0/100.0, 82.0/100.0);

vec3 gray_light = vec3(31.0/365.0, 38.0/100.0, 80.0/100.0);
vec3 gray_dark = vec3(15.0/365.0, 50.0/100.0, 24.0/100.0);

vec3 jupiter_light = vec3(31.0/365.0, 50.0/100.0, 80.0/100.0);
vec3 jupiter_dark = vec3(15.0/365.0, 70.0/100.0, 24.0/100.0);

void main() {
    if(gl_TexCoord[0].s > 0.5f || gl_TexCoord[0].t > 0.5f) {
        gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    }
	vec2 uv = gl_TexCoord[0].st;

    if(planetType == 0) {
        float perlin = (snoise(vec4(passVertex.x*10.0, passVertex.y*10.0, passVertex.z*10.0, millis/1000.0))+1.0)/2.0;
        float perlin2 = snoise(vec4(passVertex.x*2.0, passVertex.y*2.0, passVertex.z*2.0, millis/10000.0));

        vec3 col1 = vec3(1, 0.1+perlin*0.6, 0);
        vec3 col2 = vec3(perlin2*0.7);
        vec3 colF = max(col1, col2);
        gl_FragColor = vec4(colF,1.0);
        return;
    } else if(planetType == 1) {
        float perlin = (snoise(vec4(passVertex.x*2.0, passVertex.y*2.0, passVertex.z*2.0, 0.0))+1.0)/2.0;
        float perlin2 = snoise(vec4(passVertex.x*1.0, passVertex.y*1.0, passVertex.z*1.0, millis/10000.0));

        vec3 col1 = vec3(0, 0, 1);
        if(perlin < 0.55) {
            col1 = hsv2rgb(interp_hsv(ocean_deep, ocean_shallow, perlin*perlin/0.55/0.55));
        } else if(perlin < 0.9) {
            col1 = hsv2rgb(interp_hsv(land_low, land_med, (perlin-0.55)/0.35));
        } else {
            col1 = hsv2rgb(interp_hsv(land_med, land_high, (perlin-0.9)/0.1));
        }
        vec3 col2 = vec3(perlin2*0.7);
        vec3 colF = max(col1, col2);

        gl_FragColor = vec4(colF,1.0);

        gl_FragColor.rgb *= 1.2*max(0.35, dot(normalize(sunVec), normalize(passNormal)));

        return;
    } else if(planetType == 2) {
       float perlin = (snoise(vec4(passVertex.x*10.0, passVertex.y*10.0, passVertex.z*10.0, 0.0))+1.0)/2.0;
       gl_FragColor = vec4(hsv2rgb(interp_hsv(gray_light, gray_dark, perlin)), 1.0);

       gl_FragColor.rgb *= 1.2*max(0.35, dot(normalize(sunVec), normalize(passNormal)));

       return;
    } else if(planetType == 3) {
       float perlin = (snoise(vec4(passVertex.x*0.2, passVertex.y*10.0, passVertex.z*0.2, 0.0))+1.0)/2.0;
       gl_FragColor = vec4(hsv2rgb(interp_hsv(jupiter_light, jupiter_dark, perlin)), 1.0);

       gl_FragColor.rgb *= 1.2*max(0.35, dot(normalize(sunVec), normalize(passNormal)));

       return;
    } else if(planetType == 4) {
        float perlin = (snoise(vec4(passVertex.x*0.2, passVertex.y*10.0, passVertex.z*0.2, 0.0))+1.0)/2.0;
       gl_FragColor = vec4(hsv2rgb(interp_hsv(ocean_deep, ocean_shallow, perlin)), 1.0);

       gl_FragColor.rgb *= 1.2*max(0.35, dot(normalize(sunVec), normalize(passNormal)));

       return;
    }

    gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);
}