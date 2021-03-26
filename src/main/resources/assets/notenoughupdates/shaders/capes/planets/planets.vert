#version 120

varying vec4 passColour;
varying vec3 passNormal;
varying vec3 passVertex;

void main() {
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
    gl_TexCoord[0] = gl_MultiTexCoord0;

    passColour = gl_Color;
    passNormal = normalize(gl_Normal);
    passVertex = vec3(gl_Vertex.x, gl_Vertex.y, gl_Vertex.z);
}