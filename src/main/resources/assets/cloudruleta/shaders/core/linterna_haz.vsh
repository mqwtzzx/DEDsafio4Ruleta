#version 150

// Código generado por Claude (Anthropic).
// El haz de la Linterna: un rectángulo que cubre toda la pantalla (las posiciones ya vienen en -1..1).

in vec3 Position;

out vec2 uv;

void main() {
    uv = Position.xy * 0.5 + 0.5;
    gl_Position = vec4(Position.xy, 0.0, 1.0);
}
