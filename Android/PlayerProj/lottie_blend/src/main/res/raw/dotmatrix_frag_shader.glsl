precision highp float;
uniform sampler2D inputImageTexture;
uniform float dots;
uniform float size;
uniform float blur;
varying vec2 textureCoordinate;
void main() {
    if (dots == 0.0) {
        gl_FragColor = texture2D(inputImageTexture, textureCoordinate);
        return;
    }
    float dotSize = 1.0/dots;
    vec2 samplePos = textureCoordinate - mod(textureCoordinate, dotSize) + 0.5 * dotSize;
    float distanceFromSamplePoint = distance(samplePos, textureCoordinate);
    vec4 col = texture2D(inputImageTexture, samplePos);
    gl_FragColor = mix(col, vec4(0.0), smoothstep(dotSize * size, dotSize *(size + blur), distanceFromSamplePoint));
}