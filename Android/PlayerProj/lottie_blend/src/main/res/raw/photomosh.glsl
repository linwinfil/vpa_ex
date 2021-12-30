precision highp float;
attribute vec3 position;
attribute vec2 uv;

uniform mat4 modelViewMatrix;
uniform mat4 projectionMatrix;

varying vec2 vUv;

void main() {
    vUv = uv;
    gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.);
}


uniform sampler2D tDiffuse;
uniform float time;
uniform float distortion;
uniform float distortion2;
uniform float speed;
uniform float rollSpeed;
varying vec2 vUv;

//${ um}

void main() {

    vec2 p = vUv;
    float ty = time * speed * 17.346;
    float yt = p.y - ty;

    //thick distortion
    float offset = noise2d(vec2(yt*3.0, 0.0))*0.2;
    offset = offset*distortion * offset*distortion * offset;
    //fine distortion
    offset += noise2d(vec2(yt*50.0, 0.0))*distortion2*0.002;

    //combine distortion on X with roll on Y
    gl_FragColor = texture2D(tDiffuse, vec2(fract(p.x + offset), fract(p.y - time * rollSpeed)));

}
