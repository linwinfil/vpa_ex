#extension GL_OES_EGL_image_external : require
precision mediump float;
uniform samplerExternalOES texture;
varying vec2 v_TexCoordinateAlpha;
varying vec2 v_TexCoordinateRgb;

void main () {
    vec4 alphaColor = texture2D(texture, v_TexCoordinateAlpha);
    vec4 rgbColor = texture2D(texture, v_TexCoordinateRgb);
    gl_FragColor = vec4(rgbColor.r, rgbColor.g, rgbColor.b, alphaColor.r);//使用R通道系数作为alpha系数
}