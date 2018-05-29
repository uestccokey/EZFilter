 precision highp float;

 varying float v_Progress;
 varying vec4 v_Color;

 uniform float u_Time;

 attribute float a_BirthTime;
 attribute float a_Duration;
 attribute float a_Size;
 attribute vec3 a_BirthPosition;
 attribute vec3 a_DirectionVector;
 attribute vec4 a_Color;

 void main()
 {
     v_Color = a_Color;

     float elapsedTime = u_Time - a_BirthTime;
     v_Progress = elapsedTime / a_Duration;

     vec3 currentPosition = a_BirthPosition + (a_DirectionVector * elapsedTime);

     gl_Position = vec4(currentPosition, 1.0);
     gl_PointSize = a_Size;
 }