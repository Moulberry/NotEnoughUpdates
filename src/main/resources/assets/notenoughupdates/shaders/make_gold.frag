#version 120

varying vec4 passColour;
varying vec3 passPosition;
uniform sampler2D textureIn;

uniform float amount;

//Algorithm by sam hocevar
vec3 rgb2hsv(vec3 c) {
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));

    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

//Algorithm by hughsk
vec3 hsv2rgb(vec3 c) {
  vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
  vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
  return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void main() {
    vec4 texture = texture2D(textureIn, gl_TexCoord[0].st);

    vec3 hsv = rgb2hsv(texture.rgb);

    float hue = mod(hsv.x + amount + passPosition.x*4.0f, 1.0f);
    float sat = hsv.y*0.7f;
    float val = hsv.z;
    vec3 fade = hsv2rgb(vec3(hue, sat, val));

    gl_FragColor = vec4(fade.rgb, texture.a);
}
