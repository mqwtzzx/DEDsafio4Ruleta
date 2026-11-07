#version 150
// Código generado por Claude (Anthropic).
// Tinte del agujero negro: el mundo se vuelve gris oscuro, pero no gris puro: le queda un poco de color y un tono
// violáceo (en el clip, RGB medio ~ 27, 18, 30), con algo de viñeta en los bordes. El cielo ya está dibujado y no se tiñe (se salta por la profundidad).

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;   // profundidad de la pantalla principal: 1.0 = cielo
uniform float Fuerza;
uniform vec4 Neblina;   // rgb = color de la neblina (según el cielo), a = fuerza
uniform vec4 Rango;     // x = plano cercano, y = plano lejano, z = distancia de vista (bloques)

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec3 c = texture(DiffuseSampler, texCoord).rgb;
    if (texture(DepthSampler, texCoord).r >= 0.99999) {      // cielo: el cielo del agujero trae sus propios colores
        fragColor = vec4(c, 1.0);
        return;
    }
    float l = dot(c, vec3(0.299, 0.587, 0.114));
    vec3 gris = vec3(l) * vec3(0.95, 0.97, 1.02);          // gris neutro, apenas frío
    vec3 t = mix(c, gris, 0.92);                           // casi sin color (como la referencia)
    t *= 0.55;                                             // más oscuro
    // neblina del color del cielo (como en el Momento Reviil y el Momento Phora): un velo parejo y más espesa cuanto más lejos
    float z = texture(DepthSampler, texCoord).r;
    float dist = 2.0 * Rango.x * Rango.y / (Rango.y + Rango.x - (z * 2.0 - 1.0) * (Rango.y - Rango.x));
    float espesor = Neblina.a * (0.22 + 0.62 * smoothstep(0.08, 1.0, dist / max(Rango.z, 1.0)));
    t = mix(t, Neblina.rgb, espesor);
    vec2 b = texCoord - 0.5;
    t *= 1.0 - 0.35 * dot(b, b) * 2.0;                     // viñeta suave
    fragColor = vec4(mix(c, t, Fuerza), 1.0);
}
