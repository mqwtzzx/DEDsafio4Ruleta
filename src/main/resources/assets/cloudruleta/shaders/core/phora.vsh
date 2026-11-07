#version 150

// Código generado por Claude (Anthropic). Meteoritos de los Phora: posición, color (o normal) y textura.

in vec3 Position;
in vec4 Color;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 vColor;
out vec2 vUV;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vColor = Color;
    vUV = UV0;
}
