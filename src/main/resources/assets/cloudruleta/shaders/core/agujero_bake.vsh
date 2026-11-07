#version 150
// Código generado por Claude (Anthropic).
// Cuadrado a pantalla completa (de -1 a 1) para hornear una cara del cielo.
in vec3 Position;
out vec2 pos;
void main() {
    pos = Position.xy;
    gl_Position = vec4(Position.xy, 0.0, 1.0);
}
