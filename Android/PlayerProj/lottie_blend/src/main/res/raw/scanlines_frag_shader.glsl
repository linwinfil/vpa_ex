precision highp float;
uniform sampler2D inputImageTexture;
uniform float time;
uniform float count;
uniform float noiseAmount;
uniform float linesAmount;
uniform float height;
varying vec2 textureCoordinate;
#define PI 3.14159265359
highp float rand(const in vec2 uv) {
    const highp float a = 12.9898;
    const highp float b = 78.233;
    const highp float c = 43758.5453;
    highp float dt = dot(uv.xy, vec2(a, b));
    highp float sn = mod(dt, PI);
    return fract(sin(sn) * c);
}
void main() {
    // sample the source
    vec4 cTextureScreen = texture2D(inputImageTexture, textureCoordinate);

    // add noise
    float dx = rand(textureCoordinate + time);
    vec3 cResult = cTextureScreen.rgb * dx * noiseAmount;

    // add scanlines
    float lineAmount = height * 1.8 * count;
    vec2 sc = vec2(sin(textureCoordinate.y * lineAmount), cos(textureCoordinate.y * lineAmount));
    cResult += cTextureScreen.rgb * vec3(sc.x, sc.y, sc.x) * linesAmount;
    cResult = cTextureScreen.rgb + (cResult);

    // interpolate between source and result by intensity
    gl_FragColor =  vec4(cResult, cTextureScreen.a);
}