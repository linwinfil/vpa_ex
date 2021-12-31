precision highp float;

//纹理坐标
varying vec2 textureCoordinate;

//progress：过度参数，0.0 -1.0
//ratio：窗口比例，等于w/h
uniform float progress, ratio;


//转换前、后的纹理
uniform sampler2D from_inputImageTexture, to_inputImageTexture;


vec4 getFromColor(vec2 textureCoordinate)
{
    return texture2D(from_inputImageTexture, textureCoordinate);
}
vec4 getToColor(vec2 textureCoordinate)
{
    return texture2D(to_inputImageTexture, textureCoordinate);
}

//以下占位符号不删除
//#transition

void main()
{
    gl_FragColor = transition(textureCoordinate);
}