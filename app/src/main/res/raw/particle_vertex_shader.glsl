precision highp float;

varying vec4 v_Color;
varying mat2 v_Rotation;
varying float v_Progress;
varying float v_UnitTextureCoord;
varying vec2 v_CoordClamp;

uniform float u_Time;
uniform float u_TextureCount;

attribute float a_BirthTime;
attribute float a_Duration;
attribute float a_FromSize;
attribute float a_ToSize;
attribute float a_FromRotation;
attribute float a_ToRotation;
attribute vec3 a_BirthPosition;
attribute vec3 a_DirectionVector;
attribute vec4 a_FromColor;
attribute vec4 a_ToColor;
attribute float a_TextureIndex;

void main()
{
    v_UnitTextureCoord = 1.0 / u_TextureCount;
    float coordPrefix = a_TextureIndex * v_UnitTextureCoord;
    v_CoordClamp = vec2(coordPrefix, coordPrefix + v_UnitTextureCoord);

    float elapsedTime = u_Time - a_BirthTime;
    v_Progress = elapsedTime / a_Duration;

    v_Color = a_FromColor + (a_ToColor - a_FromColor) * v_Progress;
    v_Color = clamp(v_Color, vec4(0.0), vec4(1.0));

    float theta = a_FromRotation + (a_ToRotation - a_FromRotation) * v_Progress;
    v_Rotation = mat2(cos(theta), sin(theta), -sin(theta), cos(theta));

    vec3 currentPosition = a_BirthPosition + (a_DirectionVector * elapsedTime);
    gl_Position = vec4(currentPosition, 1.0);

    gl_PointSize = a_FromSize + (a_ToSize - a_FromSize) * v_Progress;
}
