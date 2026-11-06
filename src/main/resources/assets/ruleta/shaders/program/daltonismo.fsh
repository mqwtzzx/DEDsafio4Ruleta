#version 150
// Código generado por Claude (Anthropic).
// Daltonismo (cambio "Mis Ojos!"): visión de un daltónico de verdad (deuteranopía: el rojo y el verde se confunden)
// más una aberración cromática que separa el rojo, el verde y el azul hacia los bordes y tiembla un poco.

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float Tiempo;
uniform float Fuerza;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 c = texCoord - 0.5;
    float r = length(c);
    float tiembla = 1.0 + 0.25 * sin(Tiempo * 3.1) + 0.15 * sin(Tiempo * 7.3);
    vec2 corre = c * (0.010 + 0.022 * r) * tiembla * Fuerza;
    float R = texture(DiffuseSampler, texCoord + corre).r;
    float G = texture(DiffuseSampler, texCoord).g;
    float B = texture(DiffuseSampler, texCoord - corre).b;
    vec3 col = vec3(R, G, B);
    // deuteranopía (Machado 2009, severidad 1)
    mat3 deuter = mat3(
        0.367322, 0.280085, -0.011820,
        0.860646, 0.672501, 0.042940,
       -0.227968, 0.047413, 0.968881);
    vec3 d = clamp(deuter * col, 0.0, 1.0);
    fragColor = vec4(mix(col, d, Fuerza), 1.0);
}
