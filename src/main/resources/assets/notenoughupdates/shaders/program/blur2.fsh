#version 120

uniform sampler2D DiffuseSampler;

varying vec2 texCoord;
varying vec2 oneTexel;

uniform vec2 InSize;

uniform vec2 BlurDir;
uniform float Radius;
uniform float AlphaMult;

void main() {
    vec4 blurred = vec4(0.0);
    float totalStrength = 0.0;
    float totalAlpha = 0.0;
    float totalSamples = 0.0;
    for(float r = -Radius; r <= Radius; r += 1.0) {
        vec4 sample = texture2D(DiffuseSampler, texCoord + oneTexel * r * BlurDir);

		// Accumulate average alpha
        totalAlpha = totalAlpha + sample.a;
        totalSamples = totalSamples + 1.0;

		// Accumulate smoothed blur
        //float strength = (2.0 - abs(r / Radius))*sample.a;
        float strength = sample.a;
        totalStrength = totalStrength + strength;
        blurred = blurred + sample;
    }
    float alpha = totalAlpha/totalSamples*AlphaMult;
    gl_FragColor = vec4(blurred.rgb / totalStrength, alpha);
}
