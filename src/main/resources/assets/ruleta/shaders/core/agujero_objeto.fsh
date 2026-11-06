#version 150
// Código generado por Claude (Anthropic).
// Agujero negro EN 3D, con trazado de rayos de verdad (ecuación de la luz alrededor de un agujero de Schwarzschild, radio = 1).
// Se dibuja en un cuadrado perpendicular a la visual (billboard), PERO ya no es una imagen plana:
//  - el disco de gas vive en el MUNDO (tiene una orientación fija en el mundo), no pegado a la pantalla;
//  - los rayos salen de la posición REAL de la cámara (perspectiva de verdad): al moverte o girar alrededor ves el disco
//    desde otro ángulo, el arco de detrás doblado por arriba y por abajo, y la lente cambia con la distancia;
//  - lo que cae dentro es negro puro (la sombra), lo que cruza el disco toma su color (con efecto Doppler) y lo que
//    escapa trae el cielo de detrás ya doblado.
// Barato: los rayos que pasan lejos (b > BCORTE) no tocan el disco y se doblan con una fórmula; solo los cercanos se integran,
// y el cielo (nebulosas, constelaciones) sale de las texturas horneadas en vez de calcularse.

uniform float MezclaCielo;   // 0..1: cuánto está puesto el cielo del evento (así el fondo del cuadrado empalma con el cielo de verdad)
uniform vec3 CamRel;         // posición de la cámara respecto al agujero, en radios del horizonte
uniform vec3 QDer;           // ejes del cuadrado (mundo): sirven para pintar el chorro
uniform vec3 QArr;

uniform float Tiempo;
uniform sampler2D Nebulosa;          // atlas 3x2 con nebulosas y galaxia ya calculadas (ver CieloBake)
uniform sampler2D Constelaciones;    // atlas 3x2 con estrellas grandes y líneas de las constelaciones
uniform sampler2D Escena;            // copia de la pantalla de ESTE fotograma (terreno, mobs, cielo): lo que hay detrás de verdad
uniform sampler2D EscenaProf;        // su profundidad: para saber si lo que hay en un píxel está DETRÁS del agujero
uniform float Escala;                // bloques por radio del horizonte
uniform float Gris;                  // cuánto está puesto el evento: lo doblado del TERRENO se ve gris como el mundo
uniform mat4 ProjMat;                // proyección de la cámara (para saber en qué píxel de la pantalla cae una dirección)
uniform mat4 VistaMundo;             // giro de vista -> mundo (su traspuesta va de mundo -> vista)

// lo que se ve en la pantalla mirando en la dirección d (mundo). ok = 1 si esa dirección cae dentro de la pantalla Y lo que
// hay ahí está DETRÁS del agujero (un agujero real solo dobla la luz de lo que tiene detrás: nunca tu skin ni el suelo
// que tienes delante). ok = 0 si hay que rellenar con otra cosa.
vec3 escena(vec3 d, float distAgujero, out float ok) {
    vec3 dv = transpose(mat3(VistaMundo)) * d;
    vec4 cl = ProjMat * vec4(dv, 0.0);
    ok = 0.0;
    if (cl.w <= 1e-4) {
        return vec3(0.0);
    }
    vec2 p = cl.xy / cl.w * 0.5 + 0.5;
    vec2 m = min(p, 1.0 - p);
    float dentro = smoothstep(0.0, 0.04, min(m.x, m.y));
    vec2 q = clamp(p, 0.001, 0.999);
    float z = texture(EscenaProf, q).r;
    float detras = 1.0;
    if (z < 0.9999) {
        // distancia (a lo largo de la visual) de lo que hay en ese píxel
        float zn = z * 2.0 - 1.0;
        float lin = ProjMat[3][2] / (-zn - ProjMat[2][2]);
        float dist = -lin / max(-normalize(dv).z, 0.05);       // de profundidad en la visual a distancia real
        detras = smoothstep(distAgujero * 0.75, distAgujero * 0.95, dist);
    }
    ok = dentro * detras;
    vec3 col = texture(Escena, q).rgb;
    if (z < 0.9999 && Gris > 0.0) {
        // el filtro gris no pasa por los píxeles del agujero: se aplica aquí al terreno que se ve doblado
        float l = dot(col, vec3(0.299, 0.587, 0.114));
        col = mix(col, vec3(l) * vec3(0.95, 0.97, 1.02) * 0.5, Gris * 0.92);
    }
    return col;
}

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

in vec2 uv;                  // -1..1 en el cuadrado
in vec3 dir;                 // dirección real (mundo) de este píxel
out vec4 fragColor;

const float BCORTE = 11.5;   // más allá de este parámetro de impacto la luz ya no toca el disco (con 7.7 la imagen
                             // doblada del disco salía más allá y se cortaba en un círculo: los bordes rectos de la mancha)
const float R0 = 24.0;       // esfera donde empieza y acaba la integración
const int PASOS = 240;
const float RIN = 1.6;       // borde de dentro del disco: pegado a la esfera de fotones (1.5), así el disco llega
                             // hasta el borde de la sombra sin franja vacía en medio (antes 2.7)
const float ROUT = 3.3;      // borde de fuera: anillo compacto pegado a la sombra (como en la referencia)
const float GROSOR = 0.62;   // medio grosor del disco

vec3 colorDisco(vec3 x, vec3 N, vec3 E1, vec3 E2, vec3 rayo, out float a) {
    float rr = length(x);
    float ang = atan(dot(x, E2), dot(x, E1));
    // el gas gira: más rápido cerca (Kepler)
    float giro = ang + Tiempo * 1.8 / pow(rr, 1.5);
    // vetas con contraste de verdad (antes 0.8..1.0: el disco salía de un solo color, plano)
    float vetas = 0.45 + 0.75 * fbm(vec3(rr * 3.0, cos(giro) * 3.0, sin(giro) * 3.0));
    float dentro = smoothstep(RIN, RIN + 0.15, rr);
    float fuera = 1.0 - smoothstep(ROUT - 1.3, ROUT, rr);
    // crema pegado al borde de dentro, durazno hacia fuera y más oscuro en el filo exterior
    // naranja: casi blanco-amarillo pegado a la sombra, naranja en medio, rojizo oscuro hacia fuera
    vec3 c = mix(vec3(1.0, 0.88, 0.60), vec3(1.0, 0.55, 0.18), smoothstep(RIN, RIN + 0.7, rr));
    c = mix(c, vec3(0.70, 0.24, 0.06), smoothstep(RIN + 0.6, ROUT, rr));
    // Doppler: el gas que viene hacia ti brilla más, el que se aleja menos
    vec3 g = cross(N, x);
    float lg = length(g);
    float dop = lg > 1e-4 ? clamp(1.0 - 0.45 * dot(g / lg, rayo), 0.45, 1.6) : 1.0;
    a = dentro * fuera * 0.97;
    // más caliente (y más brillante) pegado al borde de dentro
    float calor = 1.0 + 1.4 * exp(-(rr - RIN) * 1.6);
    return c * vetas * dop * dop * calor;
}

void main() {
    vec3 v = normalize(dir);
    vec3 c = CamRel;
    float D = length(c);
    float tc = -dot(c, v);                 // hasta el punto de máximo acercamiento
    vec3 pc = c + v * tc;
    float b = length(pc);                  // parámetro de impacto: lo cerca que pasa la recta de la cámara
    vec3 perp = pc / max(b, 1e-4);

    // lo lejos que pasa el rayo del agujero, en "cuadrados" (sale de la dirección real, no de la geometría: así vale igual
    // con el cuadrado de lejos que con el cubo que rodea la cámara de cerca o dentro)
    float lu = (tc > 0.0 ? b : D) / 9.0;
    if (lu > 1.0) {
        fragColor = vec4(0.0);
        return;
    }

    // dónde cae el píxel en el plano del cuadrado (perpendicular a la visual hacia el agujero): para el chorro
    vec3 nh = -c / max(D, 1e-4);
    float cosang = dot(v, nh);
    vec2 uvp = vec2(2.0);
    if (cosang > 1e-3) {
        vec3 punto = c + v * (D / cosang);
        uvp = vec2(dot(punto, QDer), dot(punto, QArr)) / 9.0;
    }

    // el disco: un plano del MUNDO, inclinado
    vec3 N = normalize(vec3(0.24, 0.96, 0.14));
    vec3 E1 = normalize(cross(N, vec3(0.0, 0.0, 1.0)));
    vec3 E2 = cross(N, E1);
    // la lente se apaga MUY poco a poco: a 40 radios la luz casi no se dobla (unos 3 grados), así no queda un borde ni una
    // "burbuja" (antes se cortaba a 7,5 radios, donde todavía doblaba ~15 grados: de ahí el borde y el recuadro)
    // la lente llega a CERO antes del borde del cuadrado (a 0,8): así el borde no corta nada y no se ve ningún cuadrado.
    // (antes al borde todavía quedaba un poco de lente y el horizonte se partía en un escalón recto)
    float w = 1.0 - smoothstep(0.20, 0.80, lu);

    vec3 disco = vec3(0.0);                // luz del disco acumulada
    float paso = 1.0;                      // luz de detrás que todavía pasa
    bool cae = false;
    float rmin = 99.0;
    vec3 d = v;

    if (b > BCORTE) {
        // lejos: no toca el disco, solo se dobla (fórmula de campo débil, con el término de la cámara a distancia finita)
        float bi = 1.0 / b;
        float f = 0.5 * (1.0 + tc * inversesqrt(tc * tc + b * b));
        float ang = f * bi * (2.0 + bi * (2.945 + bi * 5.333));
        d = normalize(v - perp * ang);
    } else {
        vec3 p = c;
        if (D > R0) {
            p = c + v * (tc - sqrt(R0 * R0 - b * b));   // avanza en línea recta hasta la esfera R0
        }

        vec3 vel = v;
        float h2 = b * b;                  // momento angular al cuadrado (se conserva)
        bool salio = false;
        for (int i = 0; i < PASOS; i++) {
            float r2 = dot(p, p);
            float r = sqrt(r2);
            rmin = min(rmin, r);
            if (r < 1.0) {
                cae = true;
                break;
            }
            if (r > R0 && dot(p, vel) > 0.0) {
                salio = true;
                break;
            }

            float ds = clamp(0.07 * r, 0.03, 1.2);
            float dt = ds / length(vel);
            vel += (-1.5 * h2 / (r2 * r2 * r)) * p * dt;
            vec3 pn = p + vel * dt;

            // disco GRUESO: un gas suave alrededor del plano; se va sumando mientras el rayo lo cruza
            float alto = dot(pn, N);
            if (abs(alto) < GROSOR * 2.5) {
                vec3 xp = pn - N * alto;
                float rr = length(xp);
                if (rr > RIN && rr < ROUT) {
                    float a;
                    vec3 col = colorDisco(xp, N, E1, E2, normalize(vel), a);
                    float dens = min(a * exp(-(alto * alto) / (GROSOR * GROSOR)) * ds * 6.0, 1.0);
                    disco += paso * dens * col;
                    paso *= 1.0 - dens;
                    if (paso < 0.02) {
                        salio = true;
                        break;
                    }
                }
            }
            p = pn;
        }

        if (!salio && !cae) {
            // se acabaron los pasos: si el rayo sigue lejos del horizonte NO ha caído (antes se daba por caído y de cerca
            // salía una franja negra separando la sombra de la distorsión)
            if (length(p) > 1.6) {
                salio = true;
            } else {
                cae = true;
            }
        }
        d = normalize(vel);
    }

    d = normalize(mix(v, d, w));

    // la lente dobla lo que hay DETRÁS del agujero visto desde donde estás: como mucho ~22 grados. Así, si lo miras desde
    // abajo, ves el cielo de detrás doblado y no el césped que tienes a tu espalda (la luz que daba la vuelta entera)
    {
        float ang = acos(clamp(dot(v, d), -1.0, 1.0));
        const float ANG_MAX = 0.38;
        if (ang > ANG_MAX) {
            vec3 o = normalize(d - v * dot(v, d));
            d = normalize(v * cos(ANG_MAX) + o * sin(ANG_MAX));
        }
    }

    // REMOLINO que gira: el fondo doblado además se retuerce alrededor del agujero (más cerca, más), y encima corren
    // ondas en espiral que van girando con el tiempo. El eje es la línea cámara -> agujero, así que el remolino siempre
    // está "de cara" a ti desde donde lo mires (arriba, abajo, de lado) y cambia al moverte.
    {
        vec3 eje = -c / max(D, 1e-4);
        float phi = atan(dot(perp, QArr), dot(perp, QDer));
        float giro = w * (1.15 * 2.6 / max(b, 2.6) + 0.10 * sin(2.0 * phi + 3.0 * log(max(b, 1.0)) - Tiempo * 1.6));
        float cg = cos(giro);
        float sg = sin(giro);
        d = normalize(d * cg + cross(eje, d) * sg + eje * dot(eje, d) * (1.0 - cg));   // Rodrigues
    }

    float fondoVisible = cae ? 0.0 : paso;

    // anillo de fotones: la luz que da vueltas rozando la esfera de fotones forma el aro fino crema
    float anillo = cae ? 0.0 : exp(-(rmin - 1.55) * (rmin - 1.55) / 0.01);
    disco += vec3(1.0, 0.90, 0.70) * anillo * 0.6;

    // lo de detrás: la PANTALLA REAL (terreno, cielo, mobs...) en la dirección doblada, en tiempo real. Solo si esa
    // dirección se sale de la pantalla se usa el cielo del evento como relleno
    vec3 fondoCol = vec3(0.0);
    if (fondoVisible > 0.003) {
        float ok;
        vec3 tras = escena(d, D * Escala, ok);
        fondoCol = tras;
        if (ok < 0.999) {
            // relleno: con el cielo del evento, ese cielo doblado; sin evento, lo que hay en este mismo píxel (nunca negro:
            // antes salían franjas negras en los bordes de la pantalla)
            float ok2;
            vec3 recto = escena(v, D * Escala, ok2);
            vec3 relleno = MezclaCielo > 0.001 ? mix(recto, cieloFinal(d), MezclaCielo) : recto;
            fondoCol = mix(relleno, tras, ok);
        }
    }

    // salida PREMULTIPLICADA (mezcla ONE, 1-srcalpha). En el borde del cuadrado la lente ya no dobla (d = v), así que
    // la imagen doblada empalma sin costura con la pantalla de alrededor
    float borde = 1.0 - smoothstep(0.85, 1.0, lu);
    fragColor = vec4((disco + fondoVisible * fondoCol) * borde, borde);
}
