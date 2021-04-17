#version 120

varying vec4 passColour;
varying vec3 passNormal;
uniform sampler2D textureIn;

uniform int millis;

//Algorithm by hughsk
vec3 hsv2rgb(vec3 c) {
  vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
  vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
  return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void main()
{
    float num = 5.0;

    vec2 uv = vec2(gl_TexCoord[0].s*1024.0/300.0, gl_TexCoord[0].t*1024.0/420.0);

    float timeMod = mod(millis/1000.0/num, 1.0/num);

    if(uv.x > 1.5 || uv.y > 1) {
        vec3 rgb = hsv2rgb(vec3((-1.0/num-millis/1000.0/num+timeMod)/2.0, 0.6, 0.8)) * 1.4;
        gl_FragColor = vec4(rgb, 1.0);
        return;
    }

    float xDist = 1.0 - min(uv.x, 1.0 - uv.x)*2.0;
    float yDist = 1.0 - min(uv.y, 1.0 - uv.y)*2.0;
    //float a = 20.0;
    //float edgeDist = 1.0 - pow(pow(xDist, a) + pow(yDist, a), 1.0/a);
    float edgeDist = 1.0 - max(xDist, yDist);

    edgeDist += 0.2;
    edgeDist *= edgeDist;
    edgeDist -= timeMod;

    float factor = 1.0 - mod(edgeDist*num, 1.0);
    factor *= factor*factor;
    factor = 1.4 - factor;
    factor = max(0.8, factor);

    edgeDist = floor(edgeDist*num)/num;

    vec3 rgb = hsv2rgb(vec3((edgeDist-millis/1000.0/num+timeMod)/2.0, 0.6, 0.8));

    gl_FragColor = vec4(rgb*factor, 1.0);
}
