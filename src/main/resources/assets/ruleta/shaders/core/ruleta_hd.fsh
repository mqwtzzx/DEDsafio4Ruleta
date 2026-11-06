#version 150

// Código generado por Claude (Anthropic).
// Como position_tex_color de Minecraft, pero sin descartar lo casi transparente: así los brillos
// y los bordes suaves de la ruleta se ven enteros.
//
// Recolor (solo lo usa el relleno de las casillas): x = giro de tono (vueltas, 1 = 360 grados),
// y = cuánto sube o baja la luminosidad (-1..1), z = multiplica la saturación, w = 1 activo / 0 apagado.
// Se aplica a cada píxel de la textura, así la casilla rellenada conserva su degradado.

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform vec4 Recolor;

in vec2 uv;
in vec4 tinte;

out vec4 fragColor;

vec3 aHsl(vec3 c) {
    float mx = max(c.r, max(c.g, c.b));
    float mn = min(c.r, min(c.g, c.b));
    float l = (mx + mn) * 0.5;
    float d = mx - mn;
    if (d < 1.0e-5) {
        return vec3(0.0, 0.0, l);
    }
    float s = l > 0.5 ? d / (2.0 - mx - mn) : d / (mx + mn);
    float h;
    if (mx == c.r) {
        h = (c.g - c.b) / d + (c.g < c.b ? 6.0 : 0.0);
    } else if (mx == c.g) {
        h = (c.b - c.r) / d + 2.0;
    } else {
        h = (c.r - c.g) / d + 4.0;
    }
    return vec3(h / 6.0, s, l);
}

float canal(float p, float q, float t) {
    t = fract(t);
    if (t < 1.0 / 6.0) return p + (q - p) * 6.0 * t;
    if (t < 0.5) return q;
    if (t < 2.0 / 3.0) return p + (q - p) * (2.0 / 3.0 - t) * 6.0;
    return p;
}

vec3 deHsl(vec3 hsl) {
    float h = hsl.x;
    float s = hsl.y;
    float l = hsl.z;
    if (s < 1.0e-5) {
        return vec3(l);
    }
    float q = l < 0.5 ? l * (1.0 + s) : l + s - l * s;
    float p = 2.0 * l - q;
    return vec3(canal(p, q, h + 1.0 / 3.0), canal(p, q, h), canal(p, q, h - 1.0 / 3.0));
}

void main() {
    vec4 tex = texture(Sampler0, uv);
    if (Recolor.w > 0.5) {
        vec3 hsl = aHsl(tex.rgb);
        hsl.x = fract(hsl.x + Recolor.x);
        hsl.y = clamp(hsl.y * Recolor.z, 0.0, 1.0);
        // la luz solo cambia las zonas claras: el contorno oscuro de la ranura se queda oscuro
        hsl.z = clamp(hsl.z + Recolor.y * smoothstep(0.12, 0.32, hsl.z), 0.0, 1.0);
        tex.rgb = deHsl(hsl);
    }
    vec4 c = tex * tinte * ColorModulator;
    if (c.a <= 0.0) {
        discard;
    }
    fragColor = c;
}
