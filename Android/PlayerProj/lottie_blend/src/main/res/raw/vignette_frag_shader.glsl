precision highp float;
uniform float offset;//晕影范围 逐步扩大
uniform float darkness;//晕影力度
uniform sampler2D inputImageTexture;
varying vec2 textureCoordinate;
void main() {
    vec4 texel = texture2D(inputImageTexture, textureCoordinate);
    vec2 uv = (textureCoordinate - vec2(0.5)) * vec2(offset);
    gl_FragColor = vec4(mix(texel.rgb, vec3(1.0 - darkness), dot(uv, uv)), texel.a);
}