attribute vec4 position;//顶点
attribute vec4 inputTextureCoordinate;//纹理坐标
varying vec2 textureCoordinate;
void main()
{
    gl_Position = position;
    textureCoordinate = inputTextureCoordinate.xy;
}