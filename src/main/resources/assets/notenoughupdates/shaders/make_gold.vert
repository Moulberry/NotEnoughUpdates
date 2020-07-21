#version 120

varying vec4 passColour;
varying vec3 passPosition;

void main() {
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
    gl_TexCoord[0] = gl_MultiTexCoord0;

    passPosition = gl_Position.xyz;

    passColour = gl_Color;
}