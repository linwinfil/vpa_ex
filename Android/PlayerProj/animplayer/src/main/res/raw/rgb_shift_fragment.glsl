precision highp float;
uniform sampler2D vTexture;
uniform float u_amount;
uniform float u_angle;
varying vec2 aCoordinate;
void main()
{
    vec2 offset = u_amount * vec2(cos(u_angle), sin(u_angle));
    vec4 cr = texture2D(vTexture, aCoordinate + offset);
    vec4 cga = texture2D(vTexture, aCoordinate);
    vec4 cb = texture2D(vTexture, aCoordinate - offset);
    gl_FragColor = vec4(cr.r, cga.g, cb.b, cga.a);
}