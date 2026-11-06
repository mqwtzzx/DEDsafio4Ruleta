#version 150

// Código generado por Claude (Anthropic).
// El cielo del Momento Reviil se dibuja en un cubo centrado en la cámara: la posición de cada vértice es también
// la dirección en la que se mira, y se la pasa al fragment shader para que calcule el cielo en esa dirección.

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec3 direccion;

void main() {
    direccion = Position;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
