#extension GL_OES_EGL_image_external : require
//fragment中没有默认的浮点数精度修饰符。因此，对于浮点数，浮点数向量和矩阵变量声明，必须声明包含一个精度修饰符。
precision mediump float;

varying vec2 v_TextureFgCoordinates;//oes纹理坐标
varying vec2 v_TextureBgCoordinates;//2d纹理坐标

uniform sampler2D u_TextureBg;//2d纹理采样
uniform sampler2D/*samplerExternalOES*/ u_TextureFg;//oes纹理采样

void main()
{
    vec4 fgRgba = vec4(texture2D(u_TextureFg, v_TextureFgCoordinates));
    vec4 bgRgba = vec4(texture2D(u_TextureBg, v_TextureBgCoordinates));
    //https://learnopengl-cn.github.io/04%20Advanced%20OpenGL/03%20Blending/#_3
    //混合 Src+Dst，Src=fgRgba Dst=bgRgba
    gl_FragColor = fgRgba.rgba * fgRgba.a + bgRgba * (1.0 - fgRgba.a);
}