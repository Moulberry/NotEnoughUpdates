#version 120

varying vec4 passColour;
varying vec3 passNormal;
uniform sampler2D textureIn;

uniform int millis;
uniform int eventMillis;
uniform float eventRand; //0-1

void main() {
    vec4 texture = texture2D(textureIn, gl_TexCoord[0].st);

    vec2 texCoord = gl_TexCoord[0].st + vec2(mod(millis/100.0f,1024.0f)/1024.0f, 421.0f/1024.0f);

    vec4 extra = vec4(0,0,0,0);
    if(eventRand < 0.4f) {
        vec2 extraPos = vec2(45.0f+eventRand/0.4f*250.0f, eventMillis/300.0f*550.0f)/1024.0f;
        vec2 extraTexCoord = vec2(48.0f/1024.0f-(gl_TexCoord[0].t-extraPos.y), gl_TexCoord[0].s-extraPos.x+894.0f/1024.0f);
        if(extraTexCoord.x > 0.0f && extraTexCoord.x < 200.0f/1024.0f) {
            if(extraTexCoord.y > 843.0f/1024.0f && extraTexCoord.y < 943.0f/1024.0f) {
                extra = texture2D(textureIn, extraTexCoord);
            }
        }
    } else if(eventRand < 0.45f) {
        vec2 extraPos = vec2(-30.0f+eventMillis/2000.0f*370.0f, 50.0f+(eventRand-0.4f)/0.05f*300.0f)/1024.0f;
        vec2 extraTexCoord = vec2(gl_TexCoord[0].s-extraPos.x+248.0f/1024.0f, gl_TexCoord[0].t-extraPos.y+894.0f/1024.0f);
        if(extraTexCoord.x > 200.0f/1024.0f && extraTexCoord.x < 300.0f/1024.0f) {
            if(extraTexCoord.y > 843.0f/1024.0f && extraTexCoord.y < 943.0f/1024.0f) {
                extra = texture2D(textureIn, extraTexCoord);
            }
        }
    } else if(eventRand < 0.47f) {
        vec2 extraPos = vec2(-30.0f+eventMillis/2000.0f*370.0f, 50.0f+(eventRand-0.45f)/0.02f*300.0f)/1024.0f;
        vec2 extraTexCoord = vec2(gl_TexCoord[0].s-extraPos.x+348.0f/1024.0f, gl_TexCoord[0].t-extraPos.y+894.0f/1024.0f);
        if(extraTexCoord.x > 300.0f/1024.0f && extraTexCoord.x < 400.0f/1024.0f) {
            if(extraTexCoord.y > 843.0f/1024.0f && extraTexCoord.y < 943.0f/1024.0f) {
                extra = texture2D(textureIn, extraTexCoord);
            }
        }
    } else if(eventRand < 0.48f) {
        vec2 extraPos = vec2(-30.0f+eventMillis/2000.0f*370.0f, 50.0f+(eventRand-0.47f)/0.01f*300.0f)/1024.0f;
        vec2 extraTexCoord = vec2(gl_TexCoord[0].s-extraPos.x+448.0f/1024.0f, gl_TexCoord[0].t-extraPos.y+894.0f/1024.0f);
        if(extraTexCoord.x > 400.0f/1024.0f && extraTexCoord.x < 500.0f/1024.0f) {
            if(extraTexCoord.y > 843.0f/1024.0f && extraTexCoord.y < 943.0f/1024.0f) {
                extra = texture2D(textureIn, extraTexCoord);
            }
        }
    }

    vec3 space = texture2D(textureIn, texCoord).rgb;

    gl_FragColor = vec4(texture.rgb*texture.a + extra.rgb*extra.a*(1.0f-texture.a) + space*(1.0f-texture.a)*(1.0f-extra.a), 1.0f) * passColour;

    vec3 fakeSunNormal = normalize(vec3(0.2f,1.0f,-0.2f));
    vec3 normNormal = normalize(passNormal);
    float shading = max(0.6f, dot(fakeSunNormal, normNormal));

    gl_FragColor = vec4(gl_FragColor.rgb*shading, gl_FragColor.a);
}
