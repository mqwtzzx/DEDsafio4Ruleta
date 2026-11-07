#version 150

// Código generado por Claude (Anthropic).
// Los meteoritos de los Phora. Modo:
//  0 roca: bola de roca volcánica oscura con grietas de magma que brillan y laten (vColor.rgb = normal de la esfera)
//  1 resplandor: halo de fuego alrededor de la roca (se suma)
//  2 estela de fuego: blanca pegada a la roca, luego amarilla, naranja y roja (se suma); vUV.x = 0 en la roca, 1 en la cola
//  3 estela de humo: humo oscuro que queda detrás (se mezcla)
//  4 polvo: bocanadas del hongo de polvo (vColor.r = tono, vColor.g = semilla, vColor.b = brillo de fuego, vColor.a = cuánto queda)
//  5 color plano (destello de pantalla)
//  6 llama que envuelve la roca: como un cometa, cabeza casi blanca y lenguas de fuego naranja que se van hacia atrás
//  7 onda expansiva: anillo de polvo y aire que corre por el suelo (vUV.y = 0 dentro .. 1 fuera del anillo)

uniform float Modo;
uniform float Tiempo;
uniform float Semilla;

in vec4 vColor;
in vec2 vUV;
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
        p = p * 2.07 + vec3(5.3, 9.1, 2.7);
        a *= 0.5;
    }
    return s;
}

void main() {
    float T = Tiempo;
    if (Modo < 0.5) {
        vec3 n = normalize(vColor.rgb * 2.0 - 1.0);
        vec3 p = n * 2.3 + vec3(Semilla);
        // relieve de la roca
        float roca = fbm(p * 1.4);
        // grietas: líneas finas donde el ruido cruza por la mitad (como placas de lava enfriada)
        float r1 = 1.0 - abs(fbm(p * 1.1 + 3.0) * 2.0 - 1.0);
        float r2 = 1.0 - abs(fbm(p * 2.3 + 8.0) * 2.0 - 1.0);
        float grieta = pow(r1, 14.0) * 1.2 + pow(r2, 22.0) * 0.8;
        float late = 0.75 + 0.25 * sin(T * 6.0 + roca * 9.0);
        vec3 basalto = mix(vec3(0.07, 0.045, 0.04), vec3(0.22, 0.12, 0.08), smoothstep(0.35, 0.75, roca));
        // por la cara que va delante (la que roza el aire) la roca está más caliente
        float calor = 0.35 + 0.25 * roca;
        vec3 magma = mix(vec3(1.0, 0.32, 0.04), vec3(1.0, 0.85, 0.45), clamp(grieta - 0.6, 0.0, 1.0));
        vec3 col = basalto + vec3(0.9, 0.25, 0.04) * calor * 0.35;
        col = mix(col, magma * 1.6, clamp(grieta * late, 0.0, 1.0));
        // borde encendido (la roca brilla por los lados)
        float borde = pow(1.0 - clamp(abs(vUV.y * 2.0 - 1.0), 0.0, 1.0), 0.5);
        fragColor = vec4(min(col, vec3(1.0)), 1.0);
        return;
    }

    vec2 c = vUV * 2.0 - 1.0;
    // nada llega al borde del cuadrado: así no se ve nunca la caja
    float sinCaja = smoothstep(1.0, 0.8, length(c));
    if (Modo < 1.5) {
        // la cabeza del meteorito: un punto de luz blanca cegadora con resplandor amarillo (como en el video, no se ve roca)
        float r = length(c);
        float nucleo = exp(-r * r * 60.0) * 1.6;
        float halo = exp(-r * r * 6.0) * 0.8 + exp(-r * 3.5) * 0.35;
        vec3 col = mix(vec3(1.0, 0.78, 0.40), vec3(1.0, 1.0, 0.96), clamp(nucleo, 0.0, 1.0));
        fragColor = vec4(col, clamp((nucleo + halo) * sinCaja * vColor.a, 0.0, 1.0));
        return;
    }

    if (Modo < 2.5) {
        // estela: blanca cegadora pegada a la cabeza y fuego naranja detrás que se va apagando en rojo
        float u = vUV.x;
        float v = vUV.y * 2.0 - 1.0;
        float tur = fbm(vec3(u * 8.0 - T * 6.0, v * 2.0, Semilla));
        float ancho = mix(0.55, 1.0, smoothstep(0.0, 0.35, u)) * mix(1.0, 0.45, smoothstep(0.4, 1.0, u)) * (0.8 + 0.4 * tur);
        float nucleo = exp(-pow(v / (0.22 * ancho), 2.0));
        float brillo = exp(-pow(v / (0.65 * ancho), 2.0)) * 0.6;
        float largo = (1.0 - smoothstep(0.35, 1.0, u)) * (0.7 + 0.5 * tur);
        float calor = clamp(nucleo * (1.0 - u * 1.6), 0.0, 1.0);
        vec3 col = mix(vec3(0.95, 0.30, 0.06), vec3(1.0, 0.58, 0.16), smoothstep(0.0, 0.5, nucleo * (1.0 - u)));
        col = mix(col, vec3(1.0, 0.85, 0.5), smoothstep(0.35, 0.7, calor));
        col = mix(col, vec3(1.0, 1.0, 0.97), smoothstep(0.65, 0.9, calor) * (1.0 - smoothstep(0.08, 0.3, u)));
        fragColor = vec4(col, clamp((nucleo + brillo) * largo * vColor.a, 0.0, 1.0));
        return;
    }

    if (Modo < 3.5) {
        float u = vUV.x;
        float v = abs(vUV.y * 2.0 - 1.0);
        float n = fbm(vec3(u * 6.0 - T * 1.5, vUV.y * 2.5, Semilla + 4.0));
        float ancho = mix(0.35, 1.0, u);
        float perfil = smoothstep(ancho, ancho * 0.2, v + (n - 0.5) * 0.5);
        vec3 col = mix(vec3(0.20, 0.12, 0.08), vec3(0.32, 0.27, 0.24), u);
        float a = perfil * smoothstep(0.02, 0.15, u) * (1.0 - smoothstep(0.6, 1.0, u)) * (0.45 + 0.5 * n);
        fragColor = vec4(col, clamp(a * 0.75 * vColor.a, 0.0, 1.0));
        return;
    }

    if (Modo < 4.5) {
        // bocanada de polvo con volumen: se sombrea como una bola (luz desde arriba, panza oscura, borde que se ilumina)
        float semilla = vColor.g * 100.0;
        float r = length(c);
        if (r > 1.0) discard;
        vec3 q = vec3(c, 0.0);
        float n = fbm(vec3(c * 1.8, semilla + T * 0.10));
        float n2 = fbm(vec3(c * 4.5 + 7.0, semilla * 1.3 - T * 0.15));
        // borde grumoso de coliflor
        float ang = atan(c.y, c.x);
        float grumo = fbm(vec3(cos(ang) * 2.5, sin(ang) * 2.5, semilla + T * 0.08));
        float borde = 0.72 + 0.28 * grumo;
        float dentro = 1.0 - r / borde;
        float a = smoothstep(0.0, 0.35, dentro) * (0.75 + 0.35 * n);
        // normal falsa de la esfera, abultada por el ruido
        float rr = clamp(r / borde, 0.0, 1.0);
        vec3 nor = normalize(vec3(c / borde, sqrt(max(0.0, 1.0 - rr * rr)) + (n2 - 0.5) * 0.8));
        float luz = clamp(dot(nor, normalize(vec3(-0.35, 0.85, 0.45))), 0.0, 1.0);
        float sombra = 0.38 + 0.62 * luz;
        vec3 claro = vec3(1.0, 0.91, 0.72);
        vec3 oscuro = vec3(0.42, 0.27, 0.15);
        vec3 col = mix(oscuro, claro, sombra) * mix(0.85, 1.08, n2);
        // pliegues anaranjados
        col = mix(col, vec3(0.86, 0.55, 0.30), smoothstep(0.55, 0.85, n2) * 0.35);
        col *= vColor.r;
        // el fuego del impacto lo ilumina por dentro y por debajo
        float desdeAbajo = clamp(0.5 - c.y * 0.5, 0.0, 1.0);
        vec3 brasa = mix(vec3(1.0, 0.52, 0.16), vec3(1.0, 0.82, 0.45), luz * n);
        // lo más caliente (la bola de fuego recién nacida) es casi blanco
        brasa = mix(brasa, vec3(1.0, 0.97, 0.86), smoothstep(0.8, 1.0, vColor.b) * (0.4 + 0.6 * luz));
        col = mix(col, brasa * (1.0 + 0.3 * n), clamp(vColor.b * (0.55 + 0.45 * desdeAbajo) * 1.15, 0.0, 0.95));
        fragColor = vec4(col, clamp(a * vColor.a, 0.0, 1.0));
        return;
    }

    if (Modo < 5.5) {
        fragColor = vColor;
        return;
    }

    if (Modo < 6.5) {
        float u = vUV.x;                        // 0 delante de la roca .. 1 punta de la cola
        float v = vUV.y * 2.0 - 1.0;            // -1 .. 1 de lado a lado
        // turbulencia que corre hacia atrás
        float t1 = fbm(vec3(u * 6.0 - T * 9.0, v * 2.5, Semilla));
        float t2 = fbm(vec3(u * 14.0 - T * 15.0, v * 5.0, Semilla + 3.0));
        // forma de gota: redonda delante, se abre un poco y se afina hacia la cola
        float ancho = sqrt(clamp(u / 0.16, 0.0, 1.0)) * mix(1.0, 0.25, smoothstep(0.2, 1.0, u)) * (0.85 + 0.35 * t1);
        float dentro = 1.0 - abs(v + (t1 - 0.5) * 0.45 * u) / max(ancho, 0.001);
        float llama = clamp(dentro, 0.0, 1.0);
        llama = pow(llama, 0.8) * (0.65 + 0.7 * t2) * (1.0 - smoothstep(0.6, 1.0, u));
        // más caliente cerca de la cabeza
        float calor = llama * mix(1.6, 0.4, smoothstep(0.0, 0.7, u));
        vec3 col = vec3(0.55, 0.06, 0.01);
        col = mix(col, vec3(1.0, 0.35, 0.05), smoothstep(0.15, 0.55, calor));
        col = mix(col, vec3(1.0, 0.75, 0.25), smoothstep(0.55, 0.95, calor));
        col = mix(col, vec3(1.0, 0.98, 0.88), smoothstep(0.95, 1.4, calor));
        fragColor = vec4(col, clamp(llama * 1.1 * vColor.a, 0.0, 1.0));
        return;
    }

    if (Modo > 7.5) {
        // 8: la bola de luz del impacto: enorme, blanca en el centro y amarilla anaranjada hacia fuera
        float r = length(c);
        float a = (exp(-r * r * 2.6) + exp(-r * 4.5) * 0.4) * sinCaja;
        vec3 col = mix(vec3(1.0, 0.62, 0.25), vec3(1.0, 0.98, 0.88), exp(-r * 2.5));
        fragColor = vec4(col, clamp(a * vColor.a, 0.0, 1.0));
        return;
    }

    // 7: onda expansiva
    float rr = vUV.y;
    float ang = vUV.x * 6.2831853;
    float n = fbm(vec3(cos(ang) * 3.0, sin(ang) * 3.0, Semilla + rr * 2.0 - T * 0.5));
    float banda = smoothstep(0.0, 0.55, rr) * smoothstep(1.0, 0.7, rr);
    vec3 col = mix(vec3(0.75, 0.58, 0.40), vec3(1.0, 0.85, 0.65), smoothstep(0.6, 0.95, rr));
    fragColor = vec4(col, clamp(banda * (0.5 + 0.7 * n) * vColor.a, 0.0, 1.0));
}
