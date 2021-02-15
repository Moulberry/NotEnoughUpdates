#version 120

varying vec4 passColour;
varying vec3 passNormal;
uniform sampler2D textureIn;

uniform int millis;
uniform int eventMillis;

void main() {
    gl_FragColor = texture2D(textureIn, gl_TexCoord[0].st) * passColour;

    if(eventMillis < 350 && gl_TexCoord[0].s < 512 && gl_TexCoord[0].t < 512) {
        float scroll = (gl_TexCoord[0].s*1024.0f+gl_TexCoord[0].t*1024.0f);
        float factor = (10 - abs(scroll - eventMillis*2))/40.0f;
        if(factor > 0) {
            gl_FragColor.rgb += vec3(factor+0.25f, factor+0.25f, factor+0.25f);
        }
    }

    vec3 fakeSunNormal = normalize(vec3(0.2f,1.0f,-0.2f));
    vec3 normNormal = normalize(passNormal);
    float shading = max(0.6f, dot(fakeSunNormal, normNormal));

    gl_FragColor = vec4(gl_FragColor.rgb*shading, gl_FragColor.a);
}