precision highp float;

varying vec4 v_Color;
varying mat2 v_Rotation;
varying float v_Progress;
varying float v_UnitTextureCoord;
varying vec2 v_CoordClamp;

uniform sampler2D u_TextureUnit;

void main()
{
    if (v_Progress > 1.0 || v_Progress < 0.0) {
        discard;
    } else {
        vec2 newCoord = v_Rotation * (gl_PointCoord - vec2(0.5)) + vec2(0.5);
        newCoord.y = newCoord.y * v_UnitTextureCoord + v_CoordClamp.x;
        newCoord.y = clamp(newCoord.y, v_CoordClamp.x, v_CoordClamp.y);

        vec4 texture = texture2D(u_TextureUnit, newCoord);
        gl_FragColor = texture * v_Color * (1.0 - v_Progress);
    }
}