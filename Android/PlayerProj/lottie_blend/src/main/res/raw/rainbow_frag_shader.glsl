precision highp float;
uniform sampler2D inputImageTexture;
uniform float amount;
uniform float offset;
uniform float time;
varying vec2 textureCoordinate;
vec3 rainbow2(in float t){
    vec3 d = vec3(0.0, 0.33, 0.67);
    return 0.5 + 0.5*cos(6.28318*(t+d));
}
void main() {
    vec2 p = textureCoordinate;
    vec3 origCol = texture2D(inputImageTexture, p).rgb;
    vec2 off = texture2D(inputImageTexture, p).rg - 0.5;p += off * offset;
    vec3 rb = rainbow2((p.x + p.y + time * 2.0) * 0.5);
    vec3 col = mix(origCol, rb, amount);
    gl_FragColor = vec4(col, 1.0);
}