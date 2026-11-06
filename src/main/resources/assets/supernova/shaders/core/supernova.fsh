#version 150

in vec4 vColor;
in vec2 vUV;
in vec3 vPos;

uniform float Time;
uniform float Charge;      // 0 en reposo, 0 -> 1 durante la cuenta atrás
uniform float Expl;        // 0 -> 1 en el primer segundo y medio de la explosión (sale del blanco)
uniform float Fade;        // 1 a pleno, baja a 0 al esfumarse
uniform float Debug;       // diagnóstico: 1 = color sólido magenta
uniform float Mode;        // 0 estrella, 2 explosión, 3 cielo, 4 destello, 5 chorro, 6 color plano, 7 brillo del borde, 8 luz en el suelo, 9 gas
uniform float Intensity;   // brillo general del modo (destello / chorro / borde)
uniform float SkyMix;      // opacidad del cielo de nebulosa
uniform float SkyBright;   // cielo iluminado por la estrella en la cuenta atrás
uniform float SkyDark;     // cielo apagado tras la explosión
uniform vec3 SphereCenter; // centro de la esfera en espacio de vista
uniform vec3 ViewDirW;
uniform float Aparece;     // segundos desde que apareció la supernova (para la onda del principio)
uniform vec3 StarDirW;     // dirección (mundo) de la cámara hacia la estrella: los destellos del cielo
uniform float Radius;      // radio real de la explosión (para el brillo del borde)     // dirección (mundo) del centro de la esfera hacia la cámara

out vec4 fragColor;

float hash(vec3 p) {
    p = fract(p * 0.3183099 + 0.1);
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

float noise(vec3 x) {
    vec3 i = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(mix(hash(i + vec3(0.0, 0.0, 0.0)), hash(i + vec3(1.0, 0.0, 0.0)), f.x),
            mix(hash(i + vec3(0.0, 1.0, 0.0)), hash(i + vec3(1.0, 1.0, 0.0)), f.x), f.y),
        mix(mix(hash(i + vec3(0.0, 0.0, 1.0)), hash(i + vec3(1.0, 0.0, 1.0)), f.x),
            mix(hash(i + vec3(0.0, 1.0, 1.0)), hash(i + vec3(1.0, 1.0, 1.0)), f.x), f.y),
        f.z);
}

float fbm(vec3 p) {
    float a = 0.5;
    float s = 0.0;
    for (int i = 0; i < 5; i++) {
        s += a * noise(p);
        p = p * 2.02 + vec3(13.1, 7.7, 3.3);
        a *= 0.5;
    }
    return s;
}

float fbm3(vec3 p) {
    float a = 0.5;
    float s = 0.0;
    for (int i = 0; i < 3; i++) {
        s += a * noise(p);
        p = p * 2.02 + vec3(13.1, 7.7, 3.3);
        a *= 0.5;
    }
    return s;
}

float ridge(float v) {
    return 1.0 - abs(2.0 * v - 1.0);
}

// ------------------------------------------------------------------ cielo de nebulosa magenta
vec3 skyColor(vec3 d) {
    float h = d.y;
    float hc = max(h, 0.0);
    // como en el video: un cielo de polvo naranja ardiente
    vec3 horizon = vec3(0.86, 0.36, 0.10);
    vec3 zenith = vec3(0.52, 0.16, 0.05);
    vec3 col = mix(horizon, zenith, smoothstep(0.0, 0.65, hc));
    col = mix(col, horizon * 0.8, smoothstep(0.0, -0.25, h));

    float t = Time * 0.004;
    // brillo de fondo muy suave (sin umbrales: no deja bordes ni manchas)
    float glow = fbm3(d * 1.6 + vec3(5.0, 1.0, t));
    col *= 0.85 + 0.35 * glow;

    // zarcillos magenta: ruido muy deformado y estirado, tipo llamas/filamentos como en el video
    vec3 w1 = vec3(fbm3(d * 2.0 + vec3(t, 0.0, 0.0)), fbm3(d * 2.0 + vec3(4.1, t, 1.7)), fbm3(d * 2.0 + vec3(8.3, 2.9, t)));
    vec3 q = d * vec3(3.0, 5.5, 3.0) + (w1 - 0.5) * 3.2;
    float f1 = fbm(q);
    float f2 = fbm(q * 2.1 + vec3(3.3, 0.0, 7.1) + (w1 - 0.5) * 2.0);
    float wisp = pow(clamp((f1 - 0.33) * 2.4, 0.0, 1.0), 2.2);            // forma grande, borde difuso
    float fil = pow(clamp((f2 - 0.40) * 2.4, 0.0, 1.0), 2.5);              // filamentos finos dentro
    float curt = wisp * (0.45 + 0.9 * fil);
    curt *= smoothstep(-0.20, 0.10, h) * (1.0 - 0.5 * smoothstep(0.55, 0.95, h));
    vec3 fuego = mix(vec3(0.75, 0.22, 0.04), vec3(1.0, 0.62, 0.22), clamp(fil * 1.2, 0.0, 1.0));
    col += fuego * curt * 0.9;
    col += vec3(0.30, 0.10, 0.02) * wisp * 0.5;                            // polvo alrededor, muy tenue

    // estrellas
    vec3 g = d * 170.0;
    vec3 id = floor(g);
    vec3 off = (vec3(hash(id + 1.3), hash(id + 7.1), hash(id + 3.7)) - 0.5) * 0.6;
    float hs = hash(id);
    float dd = length(fract(g) - 0.5 - off);
    float star = step(0.94, hs) * (1.0 - smoothstep(0.0, 0.22, dd)) * (0.6 + 0.4 * sin(Time * 2.0 + hs * 60.0));
    star *= smoothstep(-0.05, 0.15, h) * (1.0 - curt * 0.6) * 0.25;      // el polvo casi no deja ver estrellas
    col += mix(vec3(0.85, 0.85, 1.0), vec3(1.0, 0.8, 0.9), fract(hs * 31.0)) * star * 1.4;

    // la estrella deslumbra: resplandor grande y anillos de destello de lente alrededor (como en el video)
    float ca = dot(d, normalize(StarDirW));
    float ang = acos(clamp(ca, -1.0, 1.0));
    col += vec3(1.0, 0.62, 0.25) * exp(-ang * 7.0) * 0.9;
    // onda al aparecer: un anillo sale de la estrella, se expande por el cielo y se desvanece (Aparece = segundos de vida)
    float onda = clamp(1.0 - Aparece / 3.5, 0.0, 1.0);
    if (onda > 0.0) {
        float ra = 0.03 + Aparece * 0.45;
        float an = 0.008 + Aparece * 0.012;
        col += vec3(1.0, 0.75, 0.35) * exp(-pow((ang - ra) / an, 2.0)) * 0.9 * onda * onda;
    }
    // cuenta atrás: la luz de la estrella aclara el cielo
    col = mix(col, col * 1.2 + vec3(0.07, 0.05, 0.13), SkyBright);
    // tras la explosión: más apagado
    col *= 1.0 - 0.35 * SkyDark;
    return col;
}

// ------------------------------------------------------------------ destello / resplandor (billboard, UV -1..1)
vec4 flare(vec2 uv) {
    float d = length(uv);
    vec3 idleC = vec3(1.0, 0.50, 0.16);
    vec3 hotC = vec3(0.86, 0.92, 1.0);
    float c = smoothstep(0.0, 0.5, Charge);
    vec3 col = mix(idleC, hotC, c);

    float glow = exp(-d * 6.0) * 1.2 + exp(-d * 2.6) * 0.35;
    glow *= 1.0 - smoothstep(0.85, 1.0, d);
    float rays = 0.0;
    float rk = smoothstep(0.0, 0.5, Charge) + Expl;
    if (rk > 0.001) {
        float hS = exp(-abs(uv.y) * 90.0) * exp(-abs(uv.x) * 2.2);          // gran raya horizontal
        float vS = exp(-abs(uv.x) * 70.0) * exp(-abs(uv.y) * 3.5) * 0.6;    // raya vertical
        vec2 r = vec2(uv.x + uv.y, uv.x - uv.y) * 0.7071;
        float dS = (exp(-abs(r.y) * 80.0) * exp(-abs(r.x) * 5.0) + exp(-abs(r.x) * 80.0) * exp(-abs(r.y) * 5.0)) * 0.35;
        float ang = atan(uv.y, uv.x);
        float spokes = pow(max(0.0, noise(vec3(ang * 9.0, 0.0, Time * 0.6))), 3.0) * exp(-d * 3.0) * 0.8;
        rays = (hS + vS + dS + spokes) * rk;
    }
    float core = exp(-d * 22.0) * 2.0;
    float mask = 1.0 - smoothstep(0.45, 0.98, d);        // nada llega al borde: no se ve el cuadrado
    float a = (glow + rays + core) * mask * Intensity * Fade;
    vec3 outc = mix(col, vec3(1.0), clamp(core + rays * 0.5, 0.0, 1.0));
    return vec4(outc, clamp(a, 0.0, 1.0));
}

// ------------------------------------------------------------------ chorro de luz (billboard a lo largo del eje)
vec4 jet(vec2 uv) {
    float a = abs(uv.x);                         // 0 centro, 1 punta
    float w = 0.07 + 0.55 * a;                   // se ensancha como un cono
    float y = uv.y / w;
    float core = exp(-y * y * 3.0);
    float n = fbm3(vec3(uv.x * 6.0 - Time * 0.8 * sign(uv.x), uv.y * 3.0, 0.0));
    float along = (1.0 - smoothstep(0.5, 1.0, a)) * (0.6 + 0.4 * n);
    float inten = core * along * (0.6 + 0.4 * exp(-a * 4.0));
    vec3 col = mix(vec3(0.62, 0.78, 1.0), vec3(1.0), pow(core, 4.0) * (1.0 - a));
    return vec4(col, clamp(inten * Intensity * Fade, 0.0, 1.0));
}

// ------------------------------------------------------------------ brillo rosa alrededor de la explosión (cáscara)
// 9: cubierta de gas alrededor de la estrella (en reposo y en la cuenta atrás). Radius = radio de la estrella; la cáscara
// llega hasta 1.5 radios. Se mide cuánto gas atraviesa cada rayo de vista (más espeso pegado a la estrella y en el borde)
// y se le da forma de humo que gira y se retuerce. SkyBright (0..1) = cuánto está activándose: de naranja pasa a blanco
vec4 gas(vec3 n) {
    vec3 v = normalize(vPos);
    float x = length(cross(v, SphereCenter)) / max(Radius, 1e-3);   // 1 = borde de la estrella
    if (dot(v, SphereCenter) < 0.0) x = 10.0;
    float exterior = 1.5;
    if (x > exterior) return vec4(0.0);
    // grosor del gas que cruza el rayo (por fuera de la estrella): cuerda de la cáscara menos la parte tapada por la estrella
    float cuerda = sqrt(max(exterior * exterior - x * x, 0.0)) - sqrt(max(1.0 - x * x, 0.0));
    float grosor = cuerda / 1.12;
    // humo: dos capas de ruido que giran en sentidos distintos y se deforman
    float T = Time;
    float c1 = cos(T * 0.06), s1 = sin(T * 0.06);
    vec3 q = vec3(c1 * n.x - s1 * n.z, n.y, s1 * n.x + c1 * n.z);
    float w = fbm3(q * 2.2 + vec3(0.0, T * 0.05, 0.0));
    float humo = fbm(q * 3.4 + w * 1.6 + vec3(T * 0.03, 0.0, -T * 0.02));
    float hilos = pow(1.0 - abs(fbm3(q * 6.0 - w * 2.0 + T * 0.04) * 2.0 - 1.0), 4.0);
    float d = grosor * (0.35 + 0.95 * smoothstep(0.30, 0.75, humo)) + hilos * 0.25 * grosor;
    d *= smoothstep(exterior, exterior - 0.25, x);
    float act = clamp(SkyBright, 0.0, 1.0);
    vec3 naranja = mix(vec3(1.0, 0.42, 0.10), vec3(1.0, 0.78, 0.40), hilos);
    vec3 blanco = mix(vec3(0.80, 0.88, 1.0), vec3(1.0), hilos);
    vec3 col = mix(naranja, blanco, act);
    return vec4(col, clamp(d * 0.75 * Intensity * Fade, 0.0, 1.0));
}

vec4 rimGlow(vec3 n) {
    // distancia del rayo de vista al centro, en radios de la esfera: 1.0 = justo el borde que se ve
    vec3 v = normalize(vPos);
    float x = length(cross(v, SphereCenter)) / max(Radius, 1e-3);
    if (dot(v, SphereCenter) < 0.0) x = 10.0;
    float wob = (noise(n * 4.0 + vec3(Time * 0.25)) - 0.5) * 0.10;
    float o = max(x - 1.0 - wob, 0.0);
    float g = exp(-o * 10.0) * smoothstep(0.86, 1.0, x);
    g *= 1.0 - smoothstep(1.22, 1.34, x);
    vec3 col = vec3(1.0, 0.24, 0.46);
    return vec4(col, clamp(g * 0.8 * Intensity * Fade, 0.0, 1.0));
}

void main() {
    if (Debug > 0.5) {
        fragColor = vec4(1.0, 0.0, 1.0, 1.0);
        return;
    }
    if (Mode > 8.5) { fragColor = gas(normalize(vColor.rgb * 2.0 - 1.0)); return; }
    if (Mode > 7.5) {                                               // 8: luz del chorro donde cae (mancha suave en el suelo)
        vec2 q = vUV * 2.0 - 1.0;
        float d = length(q);
        float a = exp(-d * d * 2.6) * (1.0 - smoothstep(0.8, 1.0, d));
        fragColor = vec4(vColor.rgb, vColor.a * a * Intensity * Fade);
        return;
    }
    if (Mode > 6.5) { fragColor = rimGlow(normalize(vColor.rgb * 2.0 - 1.0)); return; }
    if (Mode > 5.5) { fragColor = vColor; return; }                 // velo de pantalla completa
    if (Mode > 4.5) { fragColor = jet(vUV * 2.0 - 1.0); return; }
    if (Mode > 3.5) { fragColor = flare(vUV * 2.0 - 1.0); return; }

    vec3 n = normalize(vColor.rgb * 2.0 - 1.0);                   // dirección en la esfera, fija al mundo
    if (Mode > 2.5) {
        fragColor = vec4(skyColor(n), clamp(SkyMix, 0.0, 1.0));
        return;
    }

    vec3 nv = normalize(vPos - SphereCenter);
    float mu = clamp(abs(dot(nv, normalize(-vPos))), 0.0, 1.0);    // 1 = centro del disco, 0 = borde
    float r = sqrt(max(0.0, 1.0 - mu * mu));                        // radio en el disco que ves (0 centro, 1 borde)

    // ---------------------------------------------------------------- explosión
    if (Mode > 1.5) {
        vec3 V = normalize(ViewDirW);
        vec3 up = abs(V.y) > 0.95 ? vec3(1.0, 0.0, 0.0) : vec3(0.0, 1.0, 0.0);
        vec3 e1 = normalize(cross(V, up));
        vec3 e2 = cross(e1, V);
        vec3 p = n - V * dot(n, V);
        float ang = atan(dot(p, e2), dot(p, e1));
        vec2 cs = vec2(cos(ang), sin(ang));
        float T = Time;

        // rayas radiales (efecto de zoom, salen del centro)
        float s1 = fbm3(vec3(cs * 9.0, r * 1.4 - T * 0.35));
        float s2 = noise(vec3(cs * 28.0, r * 2.5 - T * 0.6));
        float streak = s1 * 0.65 + s2 * 0.45;

        // vetas moteadas rosa / turquesa, arrastradas hacia fuera
        vec3 q = n * 5.0 + vec3(cs * r * 2.2, -T * 0.08);
        float w = fbm3(q * 0.7 + T * 0.03);
        float m1 = fbm(q + w * 1.7);
        float m2 = fbm(q * 1.8 + w * 2.2 + 7.0);
        float vein = pow(ridge(m1), 5.0);
        float vein2 = pow(ridge(m2), 7.0);
        float which = smoothstep(0.42, 0.58, fbm3(n * 2.5 + 3.0));

        vec3 base = mix(vec3(1.0, 0.98, 1.0), vec3(0.86, 0.84, 0.90), smoothstep(0.0, 0.55, r));
        base *= 0.60 + 0.62 * streak;
        base = mix(base, vec3(1.0, 0.62, 0.78), smoothstep(0.55, 0.85, s2) * 0.30 * r);
        vec3 pinkV = vec3(1.0, 0.38, 0.60);
        vec3 tealV = vec3(0.45, 1.0, 0.92);
        vec3 col = base;
        col = mix(col, mix(pinkV, tealV, which), clamp(vein * 1.3 * smoothstep(0.05, 0.4, r), 0.0, 1.0) * 0.55);
        col = mix(col, mix(tealV, pinkV, which), clamp(vein2 * 1.5 * smoothstep(0.1, 0.5, r), 0.0, 1.0) * 0.45);

        // anillos de color hacia el borde con aberración cromática: turquesa dentro, rosa-rojo fuera
        float edgeN = (fbm3(vec3(cs * 5.0, T * 0.15)) - 0.5) * 0.08;
        float rr = r + edgeN;
        float tealRing = smoothstep(0.72, 0.86, rr) * (1.0 - smoothstep(0.87, 0.93, rr));
        col = mix(col, vec3(0.55, 1.0, 0.93) * (0.85 + 0.3 * streak), tealRing * 0.6);
        float pinkRim = smoothstep(0.83, 0.94, rr);
        col = mix(col, vec3(1.0, 0.25, 0.45) * (0.9 + 0.35 * streak), pinkRim);

        // núcleo blanco ardiente
        col += vec3(1.0, 0.98, 1.0) * (exp(-r * 9.0) * 1.1 + exp(-r * 3.0) * 0.15);
        col = mix(vec3(1.0), col, smoothstep(0.0, 1.0, Expl));

        float alpha = (1.0 - smoothstep(0.95, 1.0, rr)) * mix(0.93, 0.80, smoothstep(0.3, 0.9, r));
        fragColor = vec4(col, clamp(alpha * Fade, 0.0, 1.0));
        return;
    }

    // ---------------------------------------------------------------- estrella (reposo: sol naranja; cuenta: blanca)
    float spd = 0.10 + 0.6 * Charge;
    vec3 q = n * 3.2 + vec3(0.0, Time * spd * 0.15, Time * spd * 0.1);
    float w = fbm3(q * 0.9 - Time * 0.02);
    float f = fbm(q + w * 1.5);                         // manchas grandes
    float gr = fbm(n * 14.0 + w * 2.0 + Time * 0.05);   // granulación
    float vein = pow(ridge(fbm(q * 1.6 + w * 2.0 + 4.0)), 6.0);

    vec3 dark = vec3(0.55, 0.10, 0.03);
    vec3 orange = vec3(1.0, 0.42, 0.08);
    vec3 yellow = vec3(1.0, 0.78, 0.38);
    vec3 col = mix(dark, orange, smoothstep(0.30, 0.55, f));
    col = mix(col, yellow, smoothstep(0.55, 0.75, f * 0.6 + gr * 0.5));
    col *= 0.75 + 0.5 * gr;
    col = mix(col, vec3(0.45, 0.07, 0.02), vein * 0.7);                 // vetas oscuras
    col *= 0.55 + 0.6 * pow(mu, 0.6);                                   // oscurecimiento al borde
    col += vec3(1.0, 0.55, 0.2) * pow(1.0 - mu, 3.0) * 0.6;             // borde encendido

    float wh = smoothstep(0.05, 0.6, Charge);
    vec3 hot = mix(vec3(0.82, 0.88, 1.0), vec3(1.0), pow(mu, 2.0)) * (1.2 + 0.4 * gr);
    col = mix(col, hot, wh);
    col *= 1.0 + (0.04 + 0.25 * Charge) * sin(Time * (5.0 + 40.0 * Charge));

    float edge = smoothstep(0.0, 0.12, mu + (f - 0.5) * 0.08);
    fragColor = vec4(col, clamp(edge * Fade, 0.0, 1.0));
}
