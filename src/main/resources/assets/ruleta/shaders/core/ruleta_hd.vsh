#version 150

// Código generado por Claude (Anthropic). Shader de la ruleta: posición, textura y color por vértice.

in vec3 Position;
in vec2 UV0;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 uv;
out vec4 tinte;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    uv = UV0;
    tinte = Color;
}
