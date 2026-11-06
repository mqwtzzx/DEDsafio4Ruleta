#version 150
// Código generado por Claude (Anthropic).
// Momento Nutria: el mundo se deforma como en el video.
//  - Arcoíris: TODA la imagen cambia de tono muy rápido (el cielo pasa por amarillo, rosa, celeste, verde... y el pasto
//    al color opuesto). Medido en el video: ~6 vueltas de color por segundo (unos 36° de tono por cuadro a 60 fps),
//    siempre hacia delante, y la imagen un poco más saturada. Se gira el tono sin cambiar el brillo (espacio YIQ).
//  - Líneas: cada fila de la pantalla se corre a un lado en zigzag (dientes de sierra de ~1/68 del alto de la pantalla),
//    por eso los bordes verticales se ven dentados como en el video.
//  - Además: ondas suaves, zoom que late, desenfoque radial y colores separados en cada golpe de la música.
// Golpe (0..1) sube en cada golpe; Fuerza (0..1) es la entrada y salida; Arco (0..1) enciende el arcoíris y las líneas.

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float Tiempo;
uniform float Fuerza;
uniform float Golpe;
uniform float Arco;

in vec2 texCoord;
out vec4 fragColor;

// Ajustes sacados del video
const float VUELTAS_POR_SEGUNDO = 6.1;   // velocidad del cambio de color
const float SATURACION = 1.25;           // el cielo del video sale ~25% más saturado
const float FILAS = 68.0;                // dientes del zigzag a lo alto de la pantalla
const float ZIGZAG = 0.0042;             // cuánto se corre cada fila (fracción del ancho)

vec3 muestra(vec2 uv) {
    return texture(DiffuseSampler, clamp(uv, 0.001, 0.999)).rgb;
}

vec3 girarTono(vec3 c, float ang) {
    // RGB -> YIQ, se gira el color (I,Q) y se vuelve: el brillo (Y) no cambia
    float y = dot(c, vec3(0.299, 0.587, 0.114));
    float i = dot(c, vec3(0.596, -0.274, -0.322));
    float q = dot(c, vec3(0.211, -0.523, 0.312));
    float cs = cos(ang), sn = sin(ang);
    vec2 iq = vec2(i * cs - q * sn, i * sn + q * cs) * SATURACION;
    return vec3(y + 0.956 * iq.x + 0.621 * iq.y,
                y - 0.272 * iq.x - 0.647 * iq.y,
                y - 1.106 * iq.x + 1.703 * iq.y);
}

void main() {
    float t = Tiempo;
    vec2 base = texCoord;
    // líneas en zigzag: cada fila se corre a un lado, en dientes de sierra que bajan despacio
    float fila = base.y * FILAS + t * 1.5;
    float diente = abs(fract(fila) - 0.5) * 2.0 - 0.5;       // -0.5..0.5, en triángulo
    base.x += diente * ZIGZAG * 2.0 * Arco;

    vec2 c = base - 0.5;
    float r = length(c);
    // ondas que doblan la imagen (más fuertes hacia los bordes)
    vec2 onda = vec2(sin(base.y * 9.0 + t * 3.3) + 0.5 * sin(base.y * 23.0 - t * 5.1),
                     sin(base.x * 8.0 - t * 2.7) + 0.5 * sin(base.x * 19.0 + t * 4.4));
    vec2 uv = base + onda * (0.003 + 0.007 * r + 0.010 * Golpe) * Fuerza;
    // zoom que late y un pequeño giro
    float zoom = 1.0 - (0.02 + 0.05 * Golpe) * Fuerza;
    float ang = (0.012 * sin(t * 1.7) + 0.02 * Golpe * sin(t * 13.0)) * Fuerza;
    vec2 q = uv - 0.5;
    q = mat2(cos(ang), -sin(ang), sin(ang), cos(ang)) * q * zoom;
    uv = q + 0.5;
    // desenfoque radial + separación de colores (sobre todo en los golpes)
    vec3 suma = vec3(0.0);
    float largo = (0.01 + 0.06 * Golpe) * Fuerza;
    for (float k = 0.0; k < 8.0; k += 1.0) {
        float s = 1.0 - largo * (k / 8.0);
        vec2 p = 0.5 + (uv - 0.5) * s;
        vec2 sep = (uv - 0.5) * (0.004 + 0.016 * Golpe) * Fuerza;
        suma += vec3(muestra(p + sep).r, muestra(p).g, muestra(p - sep).b);
    }
    vec3 col = suma / 8.0;

    // el arcoíris del video: giro de tono de toda la imagen
    vec3 girado = girarTono(col, t * VUELTAS_POR_SEGUNDO * 6.2831853);
    col = mix(col, girado, Arco);
    col += col * 0.25 * Golpe * Fuerza;
    fragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
}
