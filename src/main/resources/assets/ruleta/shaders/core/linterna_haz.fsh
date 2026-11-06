#version 150

// Código generado por Claude (Anthropic).
//
// La luz de la Linterna como una linterna de verdad, calculada en CADA PÍXEL (no bloque por bloque): con la profundidad
// de la pantalla se saca dónde está en el mundo lo que se ve en ese píxel, y se mira si cae dentro del cono de alguna
// linterna (centro brillante, borde que se difumina, se apaga con la distancia). Así el haz se mueve suave con la cámara.
// Solo existe en el cliente: el servidor no pone ningún bloque de luz.
//
// Modo 0: aclara la escena (se mezcla multiplicando: pantalla * (1 + luz)).
// Modo 1: eclipse, todo negro salvo la luz (pantalla * (Fuera + (1 - Fuera) * luz)).

uniform sampler2D Sampler0;      // profundidad de la escena
uniform mat4 InvMat;             // de pantalla a mundo (relativo a la cámara)
uniform vec4 Luz0Pos;            // xyz = origen (relativo a la cámara), w = fuerza (0 = apagada)
uniform vec4 Luz0Dir;            // xyz = hacia dónde apunta
uniform vec4 Luz1Pos;
uniform vec4 Luz1Dir;
uniform vec4 Luz2Pos;
uniform vec4 Luz2Dir;
uniform vec4 Luz3Pos;
uniform vec4 Luz3Dir;
uniform float Modo;
uniform float Fuera;

in vec2 uv;
out vec4 fragColor;

const float ALCANCE = 30.0;          // bloques hasta donde llega
const float COS_DENTRO = 0.9763;     // 12,5 grados: el centro del haz
const float COS_FUERA = 0.9063;      // 25 grados: el borde, difuminado

float cono(vec4 pos, vec4 dir, vec3 p, vec3 n) {
    if (pos.w <= 0.0) {
        return 0.0;
    }
    vec3 v = p - pos.xyz;
    float d = length(v);
    if (d > ALCANCE || d < 1e-3) {
        return 0.0;
    }
    vec3 l = v / d;
    float c = dot(l, normalize(dir.xyz));
    float borde = smoothstep(COS_FUERA, COS_DENTRO, c);
    // un centro un poco más fuerte, como el foco de una linterna real
    float centro = 0.75 + 0.25 * smoothstep(0.985, 0.998, c);
    // se apaga suave con la distancia (y del todo al llegar al alcance)
    float cae = 1.0 / (1.0 + d * d * 0.012) * (1.0 - smoothstep(ALCANCE * 0.7, ALCANCE, d));
    // las caras que miran hacia la linterna se iluminan más que las que la ven de canto
    float cara = mix(0.45, 1.0, clamp(dot(n, -l), 0.0, 1.0));
    return borde * centro * cae * cara * pos.w;
}

void main() {
    float prof = texture(Sampler0, uv).r;
    vec4 w = InvMat * vec4(uv * 2.0 - 1.0, prof * 2.0 - 1.0, 1.0);
    vec3 p = w.xyz / w.w;
    // la normal de la superficie, sacada de los píxeles vecinos (fuera de cualquier if, para que dFdx sea válido)
    vec3 cr = cross(dFdx(p), dFdy(p));
    float lc = length(cr);
    vec3 n = lc > 1e-8 ? cr / lc : -normalize(p);
    // que la normal mire hacia la cámara
    if (dot(n, p) > 0.0) {
        n = -n;
    }

    float luz = 0.0;
    if (prof < 1.0) {
        // (el cielo no se ilumina)
        luz = cono(Luz0Pos, Luz0Dir, p, n) + cono(Luz1Pos, Luz1Dir, p, n)
            + cono(Luz2Pos, Luz2Dir, p, n) + cono(Luz3Pos, Luz3Dir, p, n);
    }

    if (Modo > 0.5) {
        float k = Fuera + (1.0 - Fuera) * clamp(luz * 1.3, 0.0, 1.0);
        fragColor = vec4(vec3(k), 1.0);
    } else {
        // luz cálida; multiplica lo que ya hay (pantalla * (1 + esto))
        // (antes 2,2: se quemaban las texturas a blanco)
        fragColor = vec4(vec3(1.0, 0.95, 0.85) * min(luz, 1.0) * 1.5, 1.0);
    }
}
