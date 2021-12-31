precision highp float;
uniform sampler2D u_texture;
uniform float u_amount;
uniform float u_angle;
varying vec2 textureCoordinate;
void main() {
    vec2 offset = u_amount * vec2(cos(u_angle), sin(u_angle));
    vec4 cr = texture2D(u_texture, textureCoordinate + offset);
    vec4 cga = texture2D(u_texture, textureCoordinate);
    vec4 cb = texture2D(u_texture, textureCoordinate - offset);
    gl_FragColor = vec4(cr.r, cga.g, cb.b, cga.a);
}