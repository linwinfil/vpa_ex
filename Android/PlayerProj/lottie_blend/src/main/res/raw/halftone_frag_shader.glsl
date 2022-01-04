precision highp float;
uniform vec2 center;
uniform float angle;
uniform float scale;
uniform vec2 tSize;
uniform sampler2D inputImageTexture;
varying vec2 textureCoordinate;
float pattern() {
    float s = sin(angle);
    float c = cos(angle);
    vec2 tex = textureCoordinate * tSize - center;
    vec2 point = vec2(c * tex.x - s * tex.y, s * tex.x + c * tex.y) * scale;
    return (sin(point.x) * sin(point.y)) * 4.0;
}
void main() {
    vec4 color = texture2D(inputImageTexture, textureCoordinate);
    if (scale == 0.0) {
        gl_FragColor = color;
        return;
    }
    float average = (color.r + color.g + color.b) / 3.0;
    gl_FragColor = vec4(vec3(average * 10.0 - 5.0 + pattern()), color.a);
}