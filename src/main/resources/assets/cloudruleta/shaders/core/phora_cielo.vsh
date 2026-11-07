#version 150

// Código generado por Claude (Anthropic).
// El cielo de los Phora se dibuja en un cubo centrado en la cámara: cada vértice es también la dirección en la que se mira.

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec3 direccion;

void main() {
    direccion = Position;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
