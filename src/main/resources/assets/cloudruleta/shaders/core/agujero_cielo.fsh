#version 150
// Código generado por Claude (Anthropic).
// Cielo del agujero negro (como en los clips): estrellas, galaxia, constelaciones y luna roja. Lo caro (nebulosas, galaxia,
// líneas de constelaciones) se hornea una sola vez en dos texturas (agujero_bake) y aquí solo se leen: así el cielo
// cuesta casi nada por fotograma. Se dibuja solo donde hay cielo (el terreno y los mobs lo tapan por la profundidad).

uniform float Alfa;

uniform float Tiempo;
uniform sampler2D Nebulosa;          // atlas 3x2 con nebulosas y galaxia ya calculadas (ver CieloBake)
uniform sampler2D Constelaciones;    // atlas 3x2 con estrellas grandes y líneas de las constelaciones

// 0 = nebulosas magenta/violeta, 1 = nebulosas rojas; va y viene sola (ciclo de 90 s, cambia en ~10 s)
float faseRoja() {
    return 0.0;   // sin fase roja: el cielo se queda siempre en grises, violetas apagados y turquesa
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

// galaxias espirales: discos inclinados azul blanquecinos con núcleo brillante, brazos y nubes de estrellas (como en el clip)
vec3 espiral(vec3 d) {
    vec3 total = vec3(0.0);
    float roja = faseRoja();
    for (int k = 0; k < 1; k++) {
        vec3 hk = h33(vec3(float(k) * 5.9 + 1.0, 8.3, 2.1));
        vec3 g = normalize(vec3(hk.x * 2.0 - 1.0, 0.12 + 0.88 * hk.y, hk.z * 2.0 - 1.0));
        if (k == 0) {
            g = normalize(vec3(-0.62, 0.30, 0.52));
        }
        float tam = 0.11 + 0.12 * h13(vec3(float(k), 4.4, 6.6));
        float cd = dot(d, g);
        if (cd < cos(min(tam * 3.4, 1.2))) {
            continue;
        }
        vec3 u;
        vec3 v;
        base(g, u, v);
        vec2 q = vec2(dot(d, u), dot(d, v)) / cd / tam;
        float ang = hk.x * 6.2831853;
        q = mat2(cos(ang), -sin(ang), sin(ang), cos(ang)) * q;
        q.y /= 0.32 + 0.45 * hk.y;                                    // inclinación del disco
        float r = length(q);
        float a = atan(q.y, q.x);
        float brazos = pow(0.5 + 0.5 * cos(2.0 * a - 6.5 * log(r + 0.08) + hk.z * 6.0), 1.4);
        float disco = exp(-r * 1.9) * (0.15 + 0.85 * brazos);
        float nucleo = exp(-r * r * 20.0) * 1.9 + exp(-r * 5.0) * 0.45;
        float halo = exp(-r * 0.8) * 0.10;
        float motas = smoothstep(0.74, 0.93, ruido(vec3(q * 13.0, float(k)))) * exp(-r * 1.3) * brazos;
        vec3 azul = mix(vec3(0.28, 0.52, 1.0), vec3(0.75, 0.40, 0.95), 0.35 * hk.z);
        azul = mix(azul, vec3(1.0, 0.55, 0.60), roja * 0.6);
        vec3 calido = mix(vec3(1.0, 0.92, 0.80), vec3(1.0, 0.75, 0.60), hk.y);
        vec3 col = azul * (disco + halo) * 1.5 + calido * nucleo + vec3(0.85, 0.92, 1.0) * motas * 1.2;
        total += col * (1.0 - smoothstep(2.3, 3.4, r));
    }
    return total;
}
// luna roja con cráteres claros y halo
vec3 lunaRoja(vec3 d, inout float tapa) {
    vec3 m = normalize(vec3(0.62, 0.46, -0.64));
    float R = 0.12;
    float cd = dot(d, m);
    if (cd < 0.9) {
        return vec3(0.0);
    }
    vec3 u;
    vec3 v;
    base(m, u, v);
    vec2 q = vec2(dot(d, u), dot(d, v)) / cd / R;
    float ang = atan(q.y, q.x);
    float borde = 1.0 + 0.035 * (ruido(vec3(cos(ang) * 4.0, sin(ang) * 4.0, 2.0)) - 0.5);
    float r = length(q) / borde;
    if (r < 1.0) {
        vec3 s = vec3(q / borde, sqrt(max(0.0, 1.0 - r * r)));
        // fondo rojo intenso con manchas más oscuras
        vec3 col = mix(vec3(0.62, 0.06, 0.12), vec3(0.86, 0.16, 0.22), fbm(s * 4.0 + 2.0));
        // cráteres: muchos círculos de tamaños distintos, fondo rosado claro y borde rojo oscuro
        for (int k = 0; k < 3; k++) {
            float esc = 3.0 + float(k) * 2.6;
            vec3 p = s * esc + float(k) * 7.3;
            vec3 c = floor(p);
            float mejor = 9.0;
            float tamano = 0.0;
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        vec3 cc = c + vec3(float(x), float(y), float(z));
                        vec3 hh = h33(cc);
                        float dd = length(p - cc - hh) / (0.24 + 0.26 * hh.x);
                        if (dd < mejor) {
                            mejor = dd;
                            tamano = hh.y;
                        }
                    }
                }
            }
            if (tamano > 0.25) {
                col = mix(col, vec3(0.97, 0.66, 0.68), smoothstep(1.0, 0.7, mejor) * (0.75 + 0.25 * fbm(p * 2.0)));      // fondo claro
                col = mix(col, vec3(0.40, 0.02, 0.06), smoothstep(0.12, 0.0, abs(mejor - 1.0)) * 0.7); // borde oscuro
            }
        }
        col *= 0.6 + 0.4 * s.z;
        col += vec3(1.0, 0.55, 0.62) * smoothstep(0.88, 1.0, r) * 0.7;      // filo rosado
        tapa = 1.0;
        return col;
    }
    return vec3(1.0, 0.25, 0.40) * exp(-(r - 1.0) * 10.0) * 0.55 + vec3(0.7, 0.1, 0.3) * exp(-(r - 1.0) * 3.0) * 0.10;
}

// ---- cielo horneado: las nebulosas y las constelaciones se calculan UNA vez (agujero_bake) y aquí solo se leen ----
const float CARA_S = 1.01;   // cada cara del atlas cubre un poco más de 90 grados (así el filtrado no sangra entre caras)

vec2 uvAtlas(vec3 d) {
    vec3 a = abs(d);
    float cara;
    float s;
    float t;
    if (a.x >= a.y && a.x >= a.z) {
        if (d.x > 0.0) { cara = 0.0; s = -d.z; t = -d.y; } else { cara = 1.0; s = d.z; t = -d.y; }
        s /= a.x;
        t /= a.x;
    } else if (a.y >= a.z) {
        if (d.y > 0.0) { cara = 2.0; s = d.x; t = d.z; } else { cara = 3.0; s = d.x; t = -d.z; }
        s /= a.y;
        t /= a.y;
    } else {
        if (d.z > 0.0) { cara = 4.0; s = d.x; t = -d.y; } else { cara = 5.0; s = -d.x; t = -d.y; }
        s /= a.z;
        t /= a.z;
    }
    vec2 baldosa = vec2(mod(cara, 3.0), floor(cara / 3.0));
    vec2 local = 0.5 + 0.5 * vec2(s, t) / CARA_S;
    return (baldosa + local) / vec2(3.0, 2.0);
}

// lo que se ve al mirar en la dirección d
vec3 fondo(vec3 d, inout float tapa) {
    vec2 uv = uvAtlas(d);
    vec4 neb = texture(Nebulosa, uv);
    vec4 con = texture(Constelaciones, uv);
    float roja = faseRoja();
    float banda = neb.a;
    float masa = neb.g * neb.g * 4.0;
    float hilo = neb.b * neb.b;
    vec3 c1 = vec3(0.40, 0.26, 0.52);     // violeta apagado
    vec3 c2 = vec3(0.30, 0.31, 0.38);     // gris azulado (bruma)
    vec3 col = vec3(0.030, 0.034, 0.045) + vec3(0.030, 0.034, 0.042) * (1.0 - abs(d.y));
    col += mix(c2, c1, neb.r) * masa * 0.55 + vec3(0.10, 0.34, 0.38) * hilo * 0.8;   // nebulosas suaves + velos turquesa
    float dens = 0.07 + banda * 0.10;     // menos estrellas, más finas
    col += capaEstrellas(d, 90.0, dens, 0.12, 1.1);
    col += capaEstrellas(d, 190.0, dens + 0.05, 0.11, 0.8);
    col += capaEstrellas(d, 360.0, 0.12 + banda * 0.15, 0.10, 0.45);
    col += capaEstrellas(d, 40.0, 0.05, 0.08, 1.6);
    col += espiral(d);
    vec3 tono = mix(vec3(0.86, 0.92, 1.0), vec3(1.0, 0.92, 0.80), roja);
    col += tono * (con.r * con.r * 4.0) + vec3(0.82, 0.90, 1.0) * con.g;
    float t = 0.0;
    vec3 luna = lunaRoja(d, t);
    col = t > 0.5 ? luna : col + luna;
    tapa = max(tapa, t);
    return col;
}

// el cielo final de una dirección, con la saturación suave de "película"
vec3 cieloFinal(vec3 d) {
    float tapa = 0.0;
    vec3 col = fondo(d, tapa);
    return col / (1.0 + max(max(col.r, col.g), col.b) * 0.15);
}

in vec3 direccion;
out vec4 fragColor;

void main() {
    fragColor = vec4(cieloFinal(normalize(direccion)), Alfa);
}
