//2d纹理 + oes纹理
attribute vec4 a_Position;//顶点坐标
attribute vec2 a_TextureFgCoordinates;
attribute vec2 a_TextureBgCoordinates;
varying vec2 v_TextureFgCoordinates;//oes纹理坐标
varying vec2 v_TextureBgCoordinates;//2d纹理坐标
void main()
{
    v_TextureFgCoordinates = a_TextureFgCoordinates;
    v_TextureBgCoordinates = a_TextureBgCoordinates;
    gl_Position = a_Position;
}

