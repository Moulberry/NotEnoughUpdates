#version 120

varying vec4 passColour;
varying vec3 passNormal;
uniform sampler2D textureIn;

uniform int millis;

//Algorithm by hughsk
vec3 hsv2rgb(vec3 c) {
  vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
  vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
  return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void main() {
    vec4 texture = texture2D(textureIn, gl_TexCoord[0].st);

    float hue = mod(millis/10000.0f+gl_TexCoord[0].t, 1.0f);
    float sat = 0.5f;
    float val = 0.5f;
    vec3 fade = hsv2rgb(vec3(hue, sat, val));

    gl_FragColor = vec4(texture.rgb*texture.a + fade*(1.0f-texture.a), 1.0f) * passColour;

    vec3 fakeSunNormal = normalize(vec3(0.2f,1.0f,-0.2f));
    vec3 normNormal = normalize(passNormal);
    float shading = max(0.6f, dot(fakeSunNormal, normNormal));

    gl_FragColor = vec4(gl_FragColor.rgb*shading, gl_FragColor.a);
}
