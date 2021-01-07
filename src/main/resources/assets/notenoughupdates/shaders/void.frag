#version 130

varying vec4 passColour;
varying vec3 passNormal;
uniform sampler2D textureIn;
varying vec4 passPosition;
uniform vec2 screensize;
uniform int millis;

//Algorithm by hughsk
vec3 hsv2rgb(vec3 c) {
  vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
  vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
  return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void main() {
    vec2 viewportCoord1 = (gl_FragCoord.xy+vec2(-millis/100.0f, 70)/5.0f)/screensize.yy;
    vec4 texture1 = texture2D(textureIn, viewportCoord1);

    vec2 viewportCoord2 = (gl_FragCoord.xy/2.0f+vec2(50, millis/100.0f)/3.0f)/screensize.yy;
    vec4 texture2 = texture2D(textureIn, viewportCoord2);

    vec2 viewportCoord3 = (gl_FragCoord.xy/3.0f+vec2(-millis/100.0f+30, millis/100.0f+90)/2.0f)/screensize.yy;
    vec4 texture3 = texture2D(textureIn, viewportCoord3);

    vec2 viewportCoord4 = (gl_FragCoord.xy/3.0f+vec2(-millis/100.0f+50, -millis/100.0f+10)/2.0f)/screensize.yy;
    vec4 texture4 = texture2D(textureIn, viewportCoord4);

    float r = texture1.r + texture2.r + texture3.r + texture4.r;
    float g = texture1.g + texture2.g + texture3.g + texture4.g;
    float b = texture1.b + texture2.b + texture3.b + texture4.b;

    if(r > 1) r = 1;
    if(g > 1) g = 1;
    if(b > 1) b = 1;

    vec4 colour = vec4(hsv2rgb(vec3(mod(millis/50000.0f+gl_FragCoord.x/screensize.x/3.0f, 1.0f), 0.35f, 1.0f)), 1.0f);

    gl_FragColor = vec4(r, g, b, 1) * passColour * colour;
}