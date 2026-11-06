#version 150

// Código generado por Claude (Anthropic).
// Eclipse solar (como en el video del DEDsafío):
//  - parcial: la Luna entra por un lado y va tapando el Sol (queda una media luna brillante); el cielo se apaga poco a poco
//  - anillo de diamante: justo antes y justo después de la totalidad, un punto de luz cegador con rayos en el borde
//  - totalidad: disco negro con corona blanca y rayitos, cielo azul de noche con estrellas y un atardecer naranja
//    alrededor de TODO el horizonte (360 grados)
// Fase: x = posición de la Luna (-1..1; 0 = justo delante del Sol), y = noche (0..1), z = anillo de diamante (0..1),
//       w = opacidad de este cielo (entra y sale suave)
// Brillo: x = cuánto Sol queda sin tapar (0..1), y = corona (0..1)

uniform float Tiempo;
uniform vec3 SolDir;
uniform vec3 Lado;
uniform vec3 Cielo;
uniform vec3 Horizonte;
uniform vec4 Fase;
uniform vec2 Brillo;

in vec3 direccion;
out vec4 fragColor;

const float RADIO_SOL = 0.10;        // radio del Sol en el cielo (radianes): casi el doble que antes (0,055)
const float RADIO_LUNA = 0.1045;     // la Luna un poco más grande, así en la totalidad no se ve nada del Sol
const float ESCALA = RADIO_SOL / 0.055;   // el halo, la corona y el anillo crecen con el Sol

float hash(vec3 p) {
    p = fract(p * 0.3183099 + 0.1);
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

float ang(vec3 a, vec3 b) {
    return acos(clamp(dot(a, b), -1.0, 1.0));
}

void main() {
    vec3 d = normalize(direccion);
    float alto = d.y;
    float noche = Fase.y;

    // el cielo de siempre (azul arriba, más claro en el horizonte)
    vec3 dia = mix(Horizonte, Cielo, smoothstep(-0.05, 0.45, alto));

    // el cielo del eclipse: azul de noche arriba y un atardecer naranja-rosa alrededor de todo el horizonte
    vec3 cenit = vec3(0.025, 0.035, 0.085);
    vec3 naranja = vec3(0.95, 0.52, 0.30);
    vec3 rosa = vec3(0.55, 0.36, 0.45);
    float banda = exp(-max(alto + 0.02, 0.0) / 0.07);
    vec3 eclipse = mix(cenit, rosa, exp(-max(alto, 0.0) / 0.22) * 0.8);
    eclipse = mix(eclipse, naranja, banda);
    if (alto < -0.02) {
        eclipse = mix(naranja * 0.55, vec3(0.06, 0.05, 0.07), smoothstep(-0.02, -0.25, alto));
    }
    vec3 col = mix(dia, eclipse, noche);
    // el atardecer del horizonte aparece ya durante la fase parcial
    col = mix(col, mix(col, naranja, banda * 0.5), smoothstep(0.1, 0.5, noche) * (1.0 - noche));

    // estrellas (solo cuando ya está oscuro)
    vec3 celda = floor(d * 300.0);
    float h = hash(celda);
    if (h > 0.9965 && alto > -0.02) {
        // quietas, sin parpadear (como en el video); cada una con su propio brillo fijo
        float brilloFijo = 0.55 + 0.45 * fract(h * 731.0);
        col += vec3(0.85, 0.9, 1.0) * smoothstep(0.55, 0.95, noche) * brilloFijo * (h - 0.9965) / 0.0035;
    }

    // el Sol y la Luna
    vec3 luna = normalize(SolDir + Lado * (Fase.x * RADIO_SOL * 2.4));
    float aSol = ang(d, SolDir);
    float aLuna = ang(d, luna);
    float discoSol = smoothstep(RADIO_SOL, RADIO_SOL * 0.97, aSol);
    float discoLuna = smoothstep(RADIO_LUNA, RADIO_LUNA * 0.985, aLuna);
    float queda = Brillo.x;

    // resplandor alrededor del Sol, según cuánto queda sin tapar
    float halo = exp(-max(aSol - RADIO_SOL, 0.0) / (0.12 * ESCALA)) * 0.55 + exp(-max(aSol - RADIO_SOL, 0.0) / (0.035 * ESCALA)) * 0.6;
    col += vec3(1.0, 0.97, 0.9) * halo * queda * (1.0 - discoLuna);

    // corona de la totalidad: luz blanca alrededor de la Luna con rayitos que se mueven muy despacio
    if (Brillo.y > 0.001 && aLuna > RADIO_LUNA * 0.98) {
        vec3 t1 = normalize(Lado);
        vec3 t2 = normalize(cross(luna, t1));
        float a = atan(dot(d, t2), dot(d, t1));
        float rayos = 0.55 + 0.25 * sin(a * 7.0 + Tiempo * 0.05) + 0.15 * sin(a * 19.0 - 1.3) + 0.12 * sin(a * 41.0 + 2.0);
        float r = aLuna - RADIO_LUNA;
        float corona = exp(-r / (0.010 * ESCALA)) * 1.1 + exp(-r / (0.035 * ESCALA * rayos)) * 0.75 + exp(-r / (0.16 * ESCALA)) * 0.18;
        col += vec3(0.95, 0.97, 1.0) * corona * Brillo.y;
    }

    // el disco del Sol (lo que la Luna no tapa) y la Luna
    col = mix(col, vec3(1.0, 0.99, 0.95) * 1.4, discoSol * (1.0 - discoLuna));
    // la Luna se ve oscura delante del Sol (y casi negra en la totalidad)
    float lunaVisible = discoLuna * max(smoothstep(0.35, 0.0, abs(Fase.x)), discoSol);
    col = mix(col, vec3(0.012, 0.012, 0.02), lunaVisible * (0.55 + 0.45 * noche));

    // anillo de diamante: un punto cegador en el borde por donde la Luna deja escapar el último rayo, con rayos en cruz
    if (Fase.z > 0.001) {
        vec3 punto = normalize(SolDir - Lado * sign(Fase.x + 0.0001) * RADIO_SOL * 0.98);
        vec3 t1 = normalize(Lado);
        vec3 t2 = normalize(cross(punto, t1));
        vec2 q = vec2(dot(d - punto, t1), dot(d - punto, t2));
        float dist = length(q);
        float brillo = exp(-dist / (0.012 * ESCALA)) * 2.0 + exp(-dist / (0.08 * ESCALA)) * 0.7;
        float cruz = exp(-abs(q.y) / 0.0025) * exp(-abs(q.x) / 0.35) + exp(-abs(q.x) / 0.0025) * exp(-abs(q.y) / 0.35);
        vec2 qd = vec2(q.x + q.y, q.x - q.y) * 0.7071;
        float diag = exp(-abs(qd.y) / 0.002) * exp(-abs(qd.x) / 0.18) + exp(-abs(qd.x) / 0.002) * exp(-abs(qd.y) / 0.18);
        col += vec3(1.0, 0.98, 0.95) * (brillo + cruz * 1.2 + diag * 0.6) * Fase.z;
    }

    fragColor = vec4(min(col, vec3(1.0)), Fase.w);
}
