#version 150
// Código generado por Claude (Anthropic).
// Distorsión alrededor de los agujeros negros: el terreno, los mobs y todo lo que no es cielo se dobla hacia fuera como a
// través de una lente y gira en remolino, más cuanto más cerca del agujero. El cielo no se toca aquí (ya se dobla en su
// propio shader). Cada Lente es x, y = centro en pantalla (0..1), z = radio de la sombra (fracción del alto), w = fuerza.

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;   // profundidad de la pantalla principal: 1.0 = cielo
uniform vec2 InSize;
uniform float Tiempo;
uniform vec4 LenteA;
uniform vec4 LenteB;
uniform vec4 LenteC;

in vec2 texCoord;
out vec4 fragColor;

vec2 doblar(vec2 uv, vec4 l) {
    if (l.w <= 0.0 || l.z <= 0.0) {
        return uv;
    }
    float asp = InSize.x / InSize.y;
    vec2 v = (uv - l.xy) * vec2(asp, 1.0);
    float r = length(v);
    float R = l.z;
    if (r > R * 8.0) {
        return uv;
    }
    float entra = smoothstep(R * 0.9, R * 1.8, r);
    float apaga = 1.0 - smoothstep(R * 3.5, R * 8.0, r);
    float k = entra * apaga * l.w;
    float nr = r - R * R * 0.9 / max(r, 1e-4) * k;
    float giro = k * (0.9 * (R / r) * (R / r) + 0.05 * sin(Tiempo * 0.7) * (R / r));
    float c = cos(giro);
    float s = sin(giro);
    vec2 dir = v / max(r, 1e-4);
    dir = vec2(dir.x * c - dir.y * s, dir.x * s + dir.y * c);
    return l.xy + dir * nr / vec2(asp, 1.0);
}

void main() {
    if (texture(DepthSampler, texCoord).r >= 0.99999) {
        fragColor = vec4(texture(DiffuseSampler, texCoord).rgb, 1.0);
        return;
    }
    vec2 uv = texCoord;
    uv = doblar(uv, LenteA);
    uv = doblar(uv, LenteB);
    uv = doblar(uv, LenteC);
    uv = 1.0 - abs(1.0 - abs(uv));          // espejo en los bordes: sin rayas estiradas
    fragColor = vec4(texture(DiffuseSampler, clamp(uv, vec2(0.001), vec2(0.999))).rgb, 1.0);
}
