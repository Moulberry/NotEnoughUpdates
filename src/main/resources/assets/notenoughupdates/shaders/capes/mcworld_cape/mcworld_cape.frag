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

vec3 rgb2hsv(vec3 c) {
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));

    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

void main() {
    vec4 texture = texture2D(textureIn, gl_TexCoord[0].st);

    float t = gl_TexCoord[0].t;
    t = clamp(t, 10.0f/1024.0f, 410.0f/1024.0f);

    float start = 56.0f - 44.0f*t/(420.0f/1024.0f);
    float width = 182.0f + 90.0f*t/(420.0f/1024.0f);
    float scaleFactor = 1.0f/(width/1024.0f)*112.0f;

    float index = mod(millis/20.0f+t*scaleFactor, 2820.0f);
    float yIndex = floor(index/1024.0f)*112.0f-(gl_TexCoord[0].s-start/1024.0f)*scaleFactor+532.0f;
    float xIndex = mod(index, 1024.0f);
    vec3 world = texture2D(textureIn, vec2(xIndex/1024.0f, yIndex/1024.0f)).xyz;

    float hue = 0.0f;
    float saturation = 1.0f;
    float value = 1.0f;
    if(index < 208) { //sky
        float blurFactor = clamp((index-158)/100.0f, 0.0f, 0.5f);

        hue = 200.0f;
        saturation = 1.0f - blurFactor;
        value = 0.9f - 0.6f*blurFactor;
    } else if(index < 800) { //underground
        if(index < 400) {
            float blurFactor = clamp((258-index)/100.0f, 0.0f, 0.5f);

            hue = 200.0f;
            saturation = 0.0f + blurFactor;
            value = 0.3f + 0.6f*blurFactor;
        } else {
            float blurFactor = clamp((index-750)/100.0f, 0.0f, 0.5f);

            hue = 350.0f;
            saturation = 0.0f + 0.5f*blurFactor;
            value = 0.3f - 0.1f*blurFactor;
        }
    } else if(index < 1762) { //nether
        if(index < 1200) {
            float blurFactor = clamp((850-index)/100.0f, 0.0f, 0.5f);

            hue = 350.0f;
            saturation = 0.5f - 0.5f*blurFactor;
            value = 0.2f + 0.1f*blurFactor;
        } else {
            float blurFactor = clamp((index-1712)/100.0f, 0.0f, 0.5f);

            hue = 350.0f;
            saturation = 0.5f - 0.5f*blurFactor;
            value = 0.2f + 0.1f*blurFactor;
        }
    } else if(index < 2200) { //underground
        if(index < 1900) {
            float blurFactor = clamp((1812-index)/100.0f, 0.0f, 0.5f);

            hue = 350.0f;
            saturation = 0.0f + 0.5f*blurFactor;
            value = 0.3f - 0.1f*blurFactor;
        } else {
            float blurFactor = clamp((index-2150)/100.0f, 0.0f, 0.5f);

            hue = 0.0f;
            saturation = 0.0f;
            value = 0.3f - 0.3f*blurFactor;
        }
    } else if(index < 2600) { //end
        if(index < 2400) {
            float blurFactor = clamp((2250-index)/100.0f, 0.0f, 0.5f);

            hue = 0.0f;
            saturation = 0.0f;
            value = 0.0f + 0.3f*blurFactor;
        } else {
            float blurFactor = clamp((index-2550)/100.0f, 0.0f, 0.5f);

            hue = 200.0f;
            saturation = 0.0f + blurFactor;
            value = 0.0f + 0.9f*blurFactor;
        }
    } else { //sky
        float blurFactor = clamp((2650-index)/100.0f, 0.0f, 0.5f);

        hue = 200.0f;
        saturation = 1.0f - blurFactor;
        value = 0.9f - 0.9f*blurFactor;
    }
    hue = mod(hue, 360.0f);
    saturation = max(0.0f, min(1.0f, saturation));
    value = max(0.0f, min(1.0f, value));

    vec3 hsv = rgb2hsv(texture.rgb);
    hsv.x = hue/360.0f;
    hsv.y *= saturation;
    hsv.z *= value;

    gl_FragColor = vec4(hsv2rgb(hsv)*texture.a + world*(1.0f-texture.a), 1.0f) * passColour;

    vec3 fakeSunNormal = normalize(vec3(0.2f,1.0f,-0.2f));
    vec3 normNormal = normalize(passNormal);
    float shading = max(0.6f, dot(fakeSunNormal, normNormal));

    gl_FragColor = vec4(gl_FragColor.rgb*shading, gl_FragColor.a);
}
