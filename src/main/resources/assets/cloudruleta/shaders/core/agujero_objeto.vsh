#version 150
// Código generado por Claude (Anthropic).
// Agujero negro como objeto en el mundo: un cuadrado perpendicular a la visual. Position es relativa a la cámara, así que
// también es la dirección real en la que se mira (para trazar el rayo de cada píxel).
in vec3 Position;
in vec2 UV0;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat4 VistaMundo;   // giro inverso de la cámara: de coordenadas de vista al mundo
out vec2 uv;
out vec3 dir;
void main() {
    uv = UV0 * 2.0 - 1.0;
    dir = mat3(VistaMundo) * Position;   // Position llega en coordenadas de vista (como en la cloudruleta_supernova)
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
