#version 120

varying vec4 passColour;
varying vec3 passNormal;
uniform sampler2D textureIn;

uniform int millis;
uniform int eventMillis;

void main() {
    vec4 texture = texture2D(textureIn, gl_TexCoord[0].st);

    float t = gl_TexCoord[0].t;
    t = clamp(t, 10.0f/1024.0f, 410.0f/1024.0f);

    float index = mod(millis/30.0f-t*1024.0f, 1024.0f);
    float xIndex = mod(index, 1024.0f);
    float yIndex = mod(gl_TexCoord[0].s*1024.0f+millis/500.0f, 421.0f)+421.0f;
    vec3 lava = texture2D(textureIn, vec2(xIndex/1024.0f, yIndex/1024.0f)).xyz;

    if(eventMillis < 350) {
        float scroll = (gl_TexCoord[0].s*1024.0f+gl_TexCoord[0].t*1024.0f);
        float factor = (10 - abs(scroll - eventMillis*2))/20.0f;
        if(factor > 0) {
            lava += vec3(factor, factor, factor);
        }
    }

    gl_FragColor = vec4(texture.rgb*texture.a + lava*(1.0f-texture.a), 1.0f) * passColour;

    vec3 fakeSunNormal = normalize(vec3(0.2f,1.0f,-0.2f));
    vec3 normNormal = normalize(passNormal);
    float shading = max(0.6f, dot(fakeSunNormal, normNormal));

    gl_FragColor = vec4(gl_FragColor.rgb*shading, gl_FragColor.a);
}
