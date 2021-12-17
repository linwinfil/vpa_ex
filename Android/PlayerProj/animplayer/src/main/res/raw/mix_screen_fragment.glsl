#extension GL_OES_EGL_image_external : require
//fragment中没有默认的浮点数精度修饰符。因此，对于浮点数，浮点数向量和矩阵变量声明，必须声明包含一个精度修饰符。
precision mediump float;

uniform sampler2D u_TextureSrc;//2d纹理采样
uniform samplerExternalOES u_TextureOes;//oes纹理采样

varying vec2 v_TextureOesCoordionates;//oes纹理坐标
varying vec2 v_TextureSrcCoordinates;//2d纹理坐标

void main()
{
    vec4 oesRbga = vec4(texture2D(u_TextureOes, v_TextureOesCoordionates));
    vec4 srcRgba = vec4(texture2D(u_TextureSrc, v_TextureSrcCoordinates));
    gl_FragColor = mix(srcRgba, oesRbga);
}