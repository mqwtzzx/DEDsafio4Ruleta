#version 150

// Código generado por Claude (Anthropic).
// El cielo del Momento Phora (como en el video): una tormenta de polvo marrón anaranjada que lo tapa todo, con nubes de
// humo oscuro que se arrastran despacio, remolinos, un sol apagado detrás del polvo y el horizonte más claro y polvoriento.
// Fuerza (0..1) = entra y sale suave. Destello (0..1) = el cielo se aclara un instante con cada impacto cercano.

uniform float Tiempo;
uniform float Fuerza;
uniform float Destello;

in vec3 direccion;
out vec4 fragColor;

float hash(vec3 p) {
    p = fract(p * 0.3183099 + 0.1);
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

float ruido(vec3 x) {
    vec3 i = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(hash(i), hash(i + vec3(1, 0, 0)), f.x), mix(hash(i + vec3(0, 1, 0)), hash(i + vec3(1, 1, 0)), f.x), f.y),
               mix(mix(hash(i + vec3(0, 0, 1)), hash(i + vec3(1, 0, 1)), f.x), mix(hash(i + vec3(0, 1, 1)), hash(i + vec3(1, 1, 1)), f.x), f.y), f.z);
}

float fbm(vec3 p) {
    float a = 0.5, s = 0.0;
    for (int i = 0; i < 5; i++) {
        s += a * ruido(p);
        p = p * 2.03 + vec3(11.7, 3.1, 7.9);
        a *= 0.5;
    }
    return s;
}

void main() {
    vec3 d = normalize(direccion);
    float alto = d.y;
    float T = Tiempo;

    // polvo de fondo: más claro y amarillento en el horizonte, más oscuro y rojizo arriba
    // como en el video: humo marrón oscuro arriba y polvo beige anaranjado hacia el horizonte
    vec3 horizonte = vec3(0.74, 0.58, 0.40);
    vec3 cenit = vec3(0.24, 0.14, 0.08);
    vec3 col = mix(horizonte, cenit, smoothstep(-0.05, 0.75, alto));

    // nubes de humo: se proyectan sobre una "cúpula" y se arrastran con el viento; con remolinos
    vec2 q = d.xz / (max(alto, 0.0) + 0.25);
    float ang = 0.35 * sin(length(q) * 0.8 - T * 0.05);
    q = mat2(cos(ang), -sin(ang), sin(ang), cos(ang)) * q;
    vec3 p = vec3(q * 0.9 + vec2(T * 0.020, T * 0.008), T * 0.015);
    float w = fbm(p * 0.8);
    float nube = fbm(p * 1.6 + w * 1.8);
    float humo = smoothstep(0.42, 0.78, nube);
    float vetas = smoothstep(0.55, 0.62, fbm(p * 3.0 + w * 2.5)) * 0.35;
    vec3 oscuro = vec3(0.11, 0.065, 0.04);
    col = mix(col, oscuro, humo * 0.8 * smoothstep(-0.05, 0.25, alto));
    // los bordes de las nubes de humo, iluminados en naranja
    float orilla = smoothstep(0.30, 0.45, nube) * (1.0 - smoothstep(0.45, 0.62, nube));
    col += vec3(0.85, 0.42, 0.15) * orilla * 0.55 * smoothstep(-0.05, 0.3, alto);
    col += vec3(0.20, 0.12, 0.05) * vetas * (1.0 - humo);

    // sol apagado detrás del polvo
    vec3 sol = normalize(vec3(0.35, 0.62, -0.7));
    float s = max(dot(d, sol), 0.0);
    // un resplandor rojizo grande detrás del humo (como el disco rojo del video)
    col += vec3(0.90, 0.35, 0.14) * (pow(s, 40.0) * 0.6 + pow(s, 5.0) * 0.25) * (1.0 - humo * 0.5);

    // por debajo del horizonte, polvo espeso
    if (alto < 0.0) {
        col = mix(col, vec3(0.42, 0.28, 0.16), smoothstep(0.0, -0.3, alto));
    }

    // granitos de polvo que vuelan
    float g = hash(floor(d * 900.0 + vec3(T * 9.0, 0.0, T * 4.0)));
    col += vec3(0.25, 0.16, 0.08) * step(0.997, g) * 0.6;

    col = mix(col, vec3(1.0, 0.94, 0.80), Destello * 0.75);
    fragColor = vec4(col, Fuerza);
}
