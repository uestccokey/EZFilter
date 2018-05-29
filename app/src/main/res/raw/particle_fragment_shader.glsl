precision highp float;

varying float v_Progress;
varying vec4 v_Color;

uniform sampler2D u_TextureUnit;

void main()
{
    if (v_Progress > 1.0 || v_Progress < 0.0) {
        discard;
    } else {
        vec4 texture = texture2D(u_TextureUnit, gl_PointCoord);
        gl_FragColor = texture * v_Color * (1.0 - v_Progress);
    }
 }