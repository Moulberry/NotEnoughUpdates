#version 120

varying vec4 passColour;
varying vec3 passNormal;
uniform sampler2D textureIn;

void main() {
    vec4 texture = texture2D(textureIn, gl_TexCoord[0].st);
    gl_FragColor = texture * passColour;

    vec3 fakeSunNormal = normalize(vec3(0.2f,1.0f,-0.2f));
    vec3 normNormal = normalize(passNormal);
    float shading = max(0.6f, dot(fakeSunNormal, normNormal));

    gl_FragColor = vec4(gl_FragColor.rgb*shading, gl_FragColor.a);
}