attribute vec4 vPosition;
attribute vec4 vTexCoordinateAlpha;
attribute vec4 vTexCoordinateRgb;
varying vec2 v_TexCoordinateAlpha;
varying vec2 v_TexCoordinateRgb;

void main() {
    v_TexCoordinateAlpha = vec2(vTexCoordinateAlpha.x, vTexCoordinateAlpha.y);
    v_TexCoordinateRgb = vec2(vTexCoordinateRgb.x, vTexCoordinateRgb.y);
    gl_Position = vPosition;
}