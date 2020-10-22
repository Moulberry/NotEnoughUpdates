#version 120

varying vec4 passColour;
varying vec3 passNormal;

void main() {
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
    gl_TexCoord[0] = gl_MultiTexCoord0;

    passColour = gl_Color;
    passNormal = normalize(gl_Normal);
}