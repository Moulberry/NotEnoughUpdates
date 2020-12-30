#version 130

varying vec4 passColour;
varying vec3 passNormal;
uniform sampler2D textureIn;
varying vec4 passPosition;
uniform vec2 screensize;

int a(float f) {
	return int(round(f*1999));
}

void main() {
    vec2 viewportCoord = gl_FragCoord.xy/screensize.xy;
    vec4 texture = texture2D(textureIn, viewportCoord);

    int offsetFactor = a(viewportCoord.x)^a(viewportCoord.y);
    float dist = 1+mod(offsetFactor/4, 2);
    float xOffset = dist*mod(offsetFactor-1, 2);
    float yOffset = dist*mod(offsetFactor-2, 2);

    texture = texture2D(textureIn, vec2(viewportCoord.x+xOffset/screensize.x, viewportCoord.y+yOffset/screensize.y));
    texture = vec4(texture.rgb*texture.a, 1);
    texture.rgb = 1 - texture.rgb;

    gl_FragColor = texture * passColour;

    vec3 fakeSunNormal = normalize(vec3(0.2f,1.0f,-0.2f));
    vec3 normNormal = normalize(passNormal);
    float shading = max(0.6f, dot(fakeSunNormal, normNormal));

    gl_FragColor = vec4(gl_FragColor.rgb*shading, 1);
}