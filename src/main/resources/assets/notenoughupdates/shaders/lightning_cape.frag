#version 120

varying vec4 passColour;
varying vec3 passNormal;
uniform sampler2D textureIn;

uniform int millis;

void main() {
    vec4 texture = texture2D(textureIn, gl_TexCoord[0].st);

    gl_FragColor = vec4(texture.rgb*shading, gl_FragColor.a);
}
