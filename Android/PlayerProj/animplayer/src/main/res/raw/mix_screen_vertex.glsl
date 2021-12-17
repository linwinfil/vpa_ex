//2d纹理 + oes纹理
attribute vec4 a_Position;//顶点坐标
attribute vec2 a_TextureOesCoordionates;
attribute vec2 a_TextureSrcCoordinates;
varying vec2 v_TextureOesCoordionates;//oes纹理坐标
varying vec2 v_TextureSrcCoordinates;//2d纹理坐标
void main()
{
    v_TextureOesCoordionates = a_TextureOesCoordionates;
    v_TextureSrcCoordinates = a_TextureSrcCoordinates;
    gl_Position = a_Position;
}

