#version 120

varying vec4 passColour;
varying vec3 passNormal;
varying vec4 passPosition;
uniform vec2 screensize;
uniform int millis;

void main() {
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
    gl_TexCoord[0] = gl_MultiTexCoord0;

    passColour = gl_Color;
    passNormal = normalize(gl_Normal);
    passPosition = gl_Position;
}