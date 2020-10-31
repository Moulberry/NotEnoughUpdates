#version 120

uniform sampler2D DiffuseSampler;

uniform float radiusSq = 0.5f;

uniform vec2 InSize;
uniform vec2 OutSize;

varying vec2 texCoord;

void main() {
    if(radiusSq < 0.5 && (texCoord.s-0.5)*(texCoord.s-0.5)+(texCoord.t-0.5)*(texCoord.t-0.5) > radiusSq) {
        discard;
    }

    /*float totalAlpha = 0.0f;
    vec3 accum = vec3(0.0);

    for(int x = -1; x<3; x++) {
        for(int y = -1; y<3; y++) {
            vec4 pixel = texture2D(DiffuseSampler, texCoord+vec2(x, y)/InSize);

            accum += pixel.rgb * pixel.a;
            totalAlpha += pixel.a;
        }
    }
    gl_FragColor.a = totalAlpha/4*4;
    gl_FragColor.rgb = accum/4*4;*/

    gl_FragColor = texture2D(DiffuseSampler, texCoord);
}