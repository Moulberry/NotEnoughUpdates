#version 120

uniform sampler2D DiffuseSampler;

varying vec2 texCoord;

void main(){
    vec3 Gray = vec3(0.3, 0.59, 0.11);
    vec4 diffuseColor = texture2D(DiffuseSampler, texCoord);

    float Luma = dot(diffuseColor.rgb, Gray);
    diffuseColor.rgb = vec3(Luma, Luma, Luma);

    gl_FragColor = diffuseColor;
}
