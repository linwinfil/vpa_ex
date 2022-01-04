precision highp float;
uniform sampler2D inputImageTexture;
uniform float time;
uniform float amount;
varying vec2 textureCoordinate;
#define Radom_Args 43758.5453
float random1d(float n){
    return fract(sin(n) * Radom_Args);
}
void main() {
    vec2 p = textureCoordinate;
    vec2 offset = (vec2(random1d(time), random1d(time + 999.99)) - 0.5) * amount;
    p += offset;
    gl_FragColor = texture2D(inputImageTexture, p);
}