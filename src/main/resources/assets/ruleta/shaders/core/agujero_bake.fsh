#version 150
// Código generado por Claude (Anthropic).
// Horneado del cielo: dibuja UNA cara del atlas (3x2 caras) con lo caro del cielo.
//  Modo 0 (Nebulosa):       r = mezcla de colores, g = raíz de la masa de nebulosa, b = raíz de los hilos, a = banda de la galaxia
//  Modo 1 (Constelaciones): r = raíz del brillo de las estrellas grandes, g = líneas
// Lo que depende del tiempo (fase roja, parpadeo) NO se hornea: lo pone el shader del cielo al leer.

uniform float Tiempo;
uniform float Cara;     // 0..5
uniform float Modo;     // 0 = nebulosa, 1 = constelaciones

// 0 = nebulosas magenta/violeta, 1 = nebulosas rojas; va y viene sola (ciclo de 90 s, cambia en ~10 s)
float faseRoja() {
    return smoothstep(0.55, 0.85, 0.5 + 0.5 * sin(Tiempo * 6.2831853 / 90.0 - 1.2));
}


const float PI = 3.14159265;

float sq(float x) {
    return x * x;
}

float h13(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.zyx + 31.32);
    return fract((p.x + p.y) * p.z);
}

vec3 h33(vec3 p) {
    p = fract(p * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yxz + 33.33);
    return fract((p.xxy + p.yxx) * p.zyx);
}

float ruido(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(h13(i), h13(i + vec3(1, 0, 0)), f.x), mix(h13(i + vec3(0, 1, 0)), h13(i + vec3(1, 1, 0)), f.x), f.y),
               mix(mix(h13(i + vec3(0, 0, 1)), h13(i + vec3(1, 0, 1)), f.x), mix(h13(i + vec3(0, 1, 1)), h13(i + vec3(1, 1, 1)), f.x), f.y), f.z);
}

float fbm(vec3 p) {
    float s = 0.0;
    float a = 0.5;
    for (int i = 0; i < 5; i++) {
        s += a * ruido(p);
        p = p * 2.03 + vec3(1.7, 9.2, 3.1);
        a *= 0.5;
    }
    return s;
}

// base ortonormal alrededor de una dirección (u = "derecha", v = "arriba" del cielo cuando se puede)
void base(vec3 n, out vec3 u, out vec3 v) {
    vec3 a = abs(n.y) < 0.95 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0);
    u = normalize(cross(a, n));
    v = cross(n, u);
}

// una capa de estrellas: el cielo se parte en celdas y en algunas hay una estrella (punto con brillo suave)
vec3 capaEstrellas(vec3 d, float escala, float densidad, float tam, float brillo) {
    vec3 p = d * escala;
    vec3 c = floor(p);
    vec3 r = h33(c);
    if (r.x > densidad) {
        return vec3(0.0);
    }
    vec3 sitio = normalize((c + 0.25 + 0.5 * h33(c + 7.13)) / escala);
    float dist = length(d - sitio) * escala;
    float t = tam * (0.5 + r.y * r.y * 1.6);
    float luz = exp(-dist * dist / (t * t)) + 0.15 * exp(-dist / (t * 2.5));
    float parpadeo = 0.75 + 0.25 * sin(Tiempo * (1.5 + r.z * 3.0) + r.y * 40.0);
    vec3 color = mix(vec3(0.72, 0.82, 1.0), vec3(1.0, 0.97, 0.92), r.z);
    if (r.y > 0.93) {
        color = vec3(1.0, 0.85, 0.65);
    }
    return color * luz * parpadeo * brillo * (0.4 + r.y);
}

// constelaciones REALES (Osa Mayor, Casiopea, Orión, Cisne, Lira y Leo, con sus posiciones de verdad, vistas desde ~42°
// norte, con su forma real pero agrandadas y repartidas por el cielo): estrellas como puntos de luz con halo y líneas finas blanco azuladas, un poco transparentes
const int NE = 39;
const int NS = 37;
const vec3 ESTRELLAS[NE] = vec3[](
    vec3(0.2845, 0.8447, 0.4533),
    vec3(0.2947, 0.7660, 0.5713),
    vec3(0.1028, 0.7505, 0.6528),
    vec3(0.0182, 0.8122, 0.5831),
    vec3(-0.1328, 0.7875, 0.6019),
    vec3(-0.2445, 0.7532, 0.6107),
    vec3(-0.3220, 0.6344, 0.7028),
    vec3(0.7766, 0.6054, 0.1742),
    vec3(0.7750, 0.5475, 0.3155),
    vec3(0.6727, 0.6592, 0.3361),
    vec3(0.6223, 0.6440, 0.4450),
    vec3(0.4903, 0.7124, 0.5021),
    vec3(0.6615, 0.6989, -0.2721),
    vec3(0.5570, 0.6814, -0.4748),
    vec3(0.7495, 0.4760, -0.4600),
    vec3(0.7207, 0.4967, -0.4837),
    vec3(0.6900, 0.5208, -0.5027),
    vec3(0.8480, 0.2560, -0.4641),
    vec3(0.6902, 0.2949, -0.6608),
    vec3(0.1024, 0.8312, -0.5465),
    vec3(-0.0162, 0.7532, -0.6576),
    vec3(0.1054, 0.6090, -0.7861),
    vec3(-0.2132, 0.8290, -0.5169),
    vec3(-0.3166, 0.4661, -0.8262),
    vec3(-0.7191, 0.6752, -0.1641),
    vec3(-0.6671, 0.7099, -0.2257),
    vec3(-0.7305, 0.6333, -0.2555),
    vec3(-0.7122, 0.6037, -0.3582),
    vec3(-0.7960, 0.4161, -0.4396),
    vec3(-0.8211, 0.4491, -0.3523),
    vec3(-0.5539, 0.5043, 0.6625),
    vec3(-0.4911, 0.6103, 0.6216),
    vec3(-0.5065, 0.6813, 0.5284),
    vec3(-0.4376, 0.7488, 0.4978),
    vec3(-0.2899, 0.7723, 0.5652),
    vec3(-0.2917, 0.7264, 0.6223),
    vec3(-0.8516, 0.5087, 0.1270),
    vec3(-0.7009, 0.6719, 0.2394),
    vec3(-0.7681, 0.5676, 0.2963));
const ivec2 LINEAS[NS] = ivec2[](
    ivec2(0, 1),
    ivec2(1, 2),
    ivec2(2, 3),
    ivec2(3, 0),
    ivec2(3, 4),
    ivec2(4, 5),
    ivec2(5, 6),
    ivec2(7, 8),
    ivec2(8, 9),
    ivec2(9, 10),
    ivec2(10, 11),
    ivec2(12, 13),
    ivec2(12, 14),
    ivec2(13, 16),
    ivec2(14, 15),
    ivec2(15, 16),
    ivec2(14, 17),
    ivec2(16, 18),
    ivec2(19, 20),
    ivec2(20, 23),
    ivec2(22, 20),
    ivec2(20, 21),
    ivec2(24, 25),
    ivec2(24, 26),
    ivec2(26, 27),
    ivec2(27, 28),
    ivec2(28, 29),
    ivec2(29, 26),
    ivec2(30, 31),
    ivec2(31, 32),
    ivec2(32, 33),
    ivec2(33, 34),
    ivec2(34, 35),
    ivec2(32, 37),
    ivec2(37, 36),
    ivec2(36, 38),
    ivec2(38, 30));


vec4 hornearNebulosa(vec3 d) {
    vec3 n = normalize(vec3(0.35, 0.5, 0.79));
    float lat = dot(d, n);
    float onda = 0.12 * fbm(d * 2.0 + 4.0);
    float banda = exp(-sq(lat + onda - 0.05) / 0.06);
    float nubes = fbm(d * 2.6 + vec3(2.0, 0.0, 5.0));
    float grandes = smoothstep(0.45, 0.75, fbm(d * 1.7 + vec3(9.0, 3.0, 1.0)));
    float finas = fbm(d * 8.0 + 11.0);
    float mezcla = smoothstep(0.35, 0.7, fbm(d * 1.6 + 8.0));
    float masa = max(banda * (0.3 + 1.2 * nubes * nubes), grandes * 0.9) * (0.65 + 0.7 * finas);
    float nube = masa * smoothstep(0.18, 0.5, nubes) * 1.9;
    float hilo = smoothstep(0.6, 0.8, fbm(d * 4.5 + 30.0)) * (banda + grandes);
    float polvo = smoothstep(0.5, 0.68, fbm(d * 5.0 + 3.0)) * (banda + grandes);
    float oscuro = 1.0 - 0.7 * min(polvo, 1.0);
    return vec4(mezcla,
                sqrt(clamp(nube * oscuro / 4.0, 0.0, 1.0)),
                sqrt(clamp(hilo * 0.45 * oscuro, 0.0, 1.0)),
                max(banda, grandes));
}

vec4 hornearConstelaciones(vec3 d) {
    float linea = 0.0;
    for (int i = 0; i < NS; i++) {
        vec3 a = normalize(ESTRELLAS[LINEAS[i].x]);
        vec3 b = normalize(ESTRELLAS[LINEAS[i].y]);
        vec3 n = normalize(cross(a, b));
        float dist = abs(dot(d, n));
        if (dist > 0.004 || dot(cross(a, d), n) < 0.0 || dot(cross(d, b), n) < 0.0 || dot(d, a + b) < 0.0) {
            continue;
        }
        linea = max(linea, smoothstep(0.0028, 0.0009, dist));
    }
    float brillo = 0.0;
    for (int i = 0; i < NE; i++) {
        float dist = length(d - normalize(ESTRELLAS[i]));
        if (dist > 0.05) {
            continue;
        }
        float tam = 0.7 + 0.6 * h13(vec3(float(i), 2.0, 5.0));
        brillo += exp(-dist * dist * 1.5e5 / tam) * 3.0 + exp(-dist * dist * 9.0e3 / tam) * 0.7 + exp(-dist * 90.0) * 0.25;
    }
    return vec4(sqrt(clamp(brillo / 4.0, 0.0, 1.0)), linea * 0.75, 0.0, 1.0);
}

const float CARA_S = 1.01;

vec3 direccionCara(float cara, vec2 st) {
    float s = st.x;
    float t = st.y;
    if (cara < 0.5) { return vec3(1.0, -t, -s); }
    if (cara < 1.5) { return vec3(-1.0, -t, s); }
    if (cara < 2.5) { return vec3(s, 1.0, t); }
    if (cara < 3.5) { return vec3(s, -1.0, -t); }
    if (cara < 4.5) { return vec3(s, -t, 1.0); }
    return vec3(-s, -t, -1.0);
}

in vec2 pos;
out vec4 fragColor;

void main() {
    vec3 d = normalize(direccionCara(Cara, pos * CARA_S));
    if (Modo < 0.5) {
        fragColor = hornearNebulosa(d);
    } else {
        fragColor = hornearConstelaciones(d);
    }
}
