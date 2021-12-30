precision highp float;
uniform sampler2D inputImageTexture;
uniform float amount;
uniform float position;
varying vec2 textureCoordinate;
void main() {
    vec4 sum = vec4(0.0);
    float vv = amount * abs(position - textureCoordinate.y);
    sum += texture2D(inputImageTexture, vec2(textureCoordinate.x, textureCoordinate.y - 4.0 * vv)) * 0.051;
    sum += texture2D(inputImageTexture, vec2(textureCoordinate.x, textureCoordinate.y - 3.0 * vv)) * 0.0918;
    sum += texture2D(inputImageTexture, vec2(textureCoordinate.x, textureCoordinate.y - 2.0 * vv)) * 0.12245;
    sum += texture2D(inputImageTexture, vec2(textureCoordinate.x, textureCoordinate.y - 1.0 * vv)) * 0.1531;
    sum += texture2D(inputImageTexture, vec2(textureCoordinate.x, textureCoordinate.y)) * 0.1633;
    sum += texture2D(inputImageTexture, vec2(textureCoordinate.x, textureCoordinate.y + 1.0 * vv)) * 0.1531;
    sum += texture2D(inputImageTexture, vec2(textureCoordinate.x, textureCoordinate.y + 2.0 * vv)) * 0.12245;
    sum += texture2D(inputImageTexture, vec2(textureCoordinate.x, textureCoordinate.y + 3.0 * vv)) * 0.0918;
    sum += texture2D(inputImageTexture, vec2(textureCoordinate.x, textureCoordinate.y + 4.0 * vv)) * 0.051;
    gl_FragColor = sum;
}