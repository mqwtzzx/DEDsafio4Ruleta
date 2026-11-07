#version 150

// Código generado por Claude (Anthropic).
//
// Cielo del Momento Reviil. Se dibuja en un cubo alrededor de la cámara: cada píxel sabe en qué dirección mira y
// aquí se calcula qué hay en esa dirección. Todo es procedural (no usa texturas).
//
//  1. Tormenta: una mancha granate muy oscura y difuminada que se abre desde lo alto y se extiende hasta el
//     horizonte. A medida que cubre el cielo se forma un remolino: un ojo oscuro en lo alto y dos brazos en espiral
//     como cortinas de luz roja, con la orilla ondulada que brilla y un resplandor que se apaga hacia dentro.
//  2. Rayo: una línea de neón rojo quebrada, con tres ramas, que cruza lo alto del cielo. Con cada golpe el cielo
//     se enciende de rojo un instante y sale un anillo de luz desde lo alto.
//  3. Grieta: el rayo se abre en una grieta grande (más ancha en el medio, donde va la cabeza de Reviil, y en punta
//     en los extremos) con bordes de neón. Por dentro, un fondo liso azul (como el cielo de
//     Minecraft) con un toque morado y algunas motas de polvo cálidas. Se queda
//     abierta hasta que acaba el momento.
//
// Uniforms (los pone client/reviil/CieloReviil):
//   Tiempo       segundos desde que empezó
//   Cubre        hasta dónde llegó la tormenta, en radianes desde lo alto (0 = nada; > 3.2 = todo, también abajo)
//   Forma        x = giro del borde de la mancha, y = semilla de la tormenta
//   Grieta       x = largo del rayo (0..1), y = apertura (0..1), z = sin uso, w = destello (0..1)
//   GrietaForma  x = giro de la grieta (rad), y = semilla de su forma, z = radio del anillo de luz, w = su fuerza

uniform float Tiempo;
uniform float Cubre;
uniform vec4 Forma;
uniform vec4 Grieta;
uniform vec4 GrietaForma;

in vec3 direccion;

out vec4 fragColor;

const float PI = 3.14159265;
/** Medio largo de la grieta (rad): mide unos 115 grados de punta a punta. */
const float LARGO = 1.0;
/** Medio ancho máximo de la grieta abierta (rad), sin contar el ensanche del medio. */
const float ANCHO = 0.11;

// ------------------------------------------------------------ azar y ruido

// azar entero: mezcla los bits de tres números (función tipo PCG); da tres valores independientes
uvec3 mezclar(uvec3 v) {
    v = v * 1664525u + 1013904223u;
    v.x += v.y * v.z;
    v.y += v.z * v.x;
    v.z += v.x * v.y;
    v ^= v >> 16u;
    v.x += v.y * v.z;
    v.y += v.z * v.x;
    v.z += v.x * v.y;
    return v;
}

// dirección al azar (componentes de -1 a 1) para cada esquina entera de la rejilla
vec3 pendiente(vec3 esquina) {
    return vec3(mezclar(uvec3(ivec3(esquina)))) * (2.0 / 4294967295.0) - 1.0;
}

// ruido de gradiente 3D: cada esquina de la rejilla tiene una pendiente al azar y en medio se interpola con curva
// suave. Resultado aproximado de 0 a 1
float ruido(vec3 p) {
    vec3 i = floor(p);
    vec3 f = p - i;
    vec3 s = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);
    float a = dot(pendiente(i), f);
    float b = dot(pendiente(i + vec3(1, 0, 0)), f - vec3(1, 0, 0));
    float c = dot(pendiente(i + vec3(0, 1, 0)), f - vec3(0, 1, 0));
    float d = dot(pendiente(i + vec3(1, 1, 0)), f - vec3(1, 1, 0));
    float e = dot(pendiente(i + vec3(0, 0, 1)), f - vec3(0, 0, 1));
    float g = dot(pendiente(i + vec3(1, 0, 1)), f - vec3(1, 0, 1));
    float h = dot(pendiente(i + vec3(0, 1, 1)), f - vec3(0, 1, 1));
    float k = dot(pendiente(i + vec3(1, 1, 1)), f - vec3(1, 1, 1));
    float n = mix(mix(mix(a, b, s.x), mix(c, d, s.x), s.y), mix(mix(e, g, s.x), mix(h, k, s.x), s.y), s.z);
    return clamp(0.5 + 0.6 * n, 0.0, 1.0);
}

// giro entre capas del ruido (para que las capas no se alineen entre sí)
const mat3 ROTA = mat3(0.36, 0.48, -0.80, -0.80, 0.60, 0.00, 0.48, 0.64, 0.60);

// suma de capas de ruido, cada una el doble de fina y algo menos de la mitad de fuerte
float nubosidad(vec3 p, int capas) {
    float suma = 0.0;
    float peso = 0.55;
    float total = 0.0;
    for (int i = 0; i < 5; i++) {
        if (i >= capas) {
            break;
        }

        suma += peso * ruido(p);
        total += peso;
        p = ROTA * p * 2.1 + vec3(0.7, 3.9, 1.3);
        peso *= 0.47;
    }

    return suma / total;
}

// azar de 0 a 1 para un número
float azar(float x) {
    return float(mezclar(uvec3(ivec3(floor(x), floor(x * 0.37 + 91.0), 7))).x) / 4294967295.0;
}

// azar de 0 a 1 para dos números
float azar2(vec2 p) {
    return float(mezclar(uvec3(ivec2(floor(p)), 13)).y) / 4294967295.0;
}

// ruido 1D suave (valores al azar en los enteros, interpolados)
float ondula(float x) {
    float i = floor(x);
    float f = x - i;
    f = f * f * (3.0 - 2.0 * f);
    return mix(azar(i * 7.0 + 3.0), azar(i * 7.0 + 10.0), f);
}

vec2 rota2(vec2 p, float a) {
    float c = cos(a);
    float s = sin(a);
    return vec2(c * p.x - s * p.y, s * p.x + c * p.y);
}

// ------------------------------------------------------------ tormenta

// d: dirección; th: radianes desde lo alto; phi: ángulo alrededor de lo alto. Devuelve color y opacidad
vec4 tormenta(vec3 d, float th, float phi) {
    float semilla = Forma.y;
    float extendida = min(1.0, Cubre);

    // borde de la mancha: avanza desde lo alto, irregular y muy difuminado (como humo espeso visto de lejos)
    float grande = nubosidad(vec3(cos(phi) * 1.4, sin(phi) * 1.4, semilla + Tiempo * 0.05), 4) - 0.5;
    float fino = nubosidad(d * 5.0 + vec3(semilla, Tiempo * 0.07, 0.0), 3) - 0.5;
    float borde = Cubre * (1.0 + 0.35 * grande) + 0.03 * fino * extendida;
    float dentro = borde - th;                              // > 0 dentro de la mancha
    float suave = 0.06 + 0.22 * extendida;
    float opacidad = smoothstep(-suave, suave * 0.8, dentro);
    if (opacidad <= 0.0) {
        return vec4(0.0);
    }

    // el remolino gira entero a la misma velocidad: si el centro girara más rápido que las orillas, con el tiempo los
    // brazos se irían enrollando cada vez más hasta perder la forma de espiral
    float giro = mod(Tiempo * 0.05, 2.0 * PI) + semilla;
    vec3 dr = vec3(cos(giro) * d.x - sin(giro) * d.z, d.y, sin(giro) * d.x + cos(giro) * d.z);

    // fondo: granate muy oscuro; el ojo de lo alto todavía más oscuro
    vec3 color = vec3(0.105, 0.0, 0.010);
    color *= 0.70 + 0.30 * smoothstep(0.05, 0.45, th);
    // variación suave de humo en el fondo
    color *= 0.85 + 0.3 * nubosidad(dr * 3.0 + vec3(semilla), 3);

    // brazos en espiral (dos brazos, separados siempre lo mismo, como anillos que se enrollan). Cada brazo es como una cortina de luz:
    // por fuera tiene una orilla nítida y ondulada que brilla mucho, y hacia dentro se apaga poco a poco
    float psi = phi - giro - 7.0 * th;
    float ondas = nubosidad(dr * 2.0 + vec3(0.0, Tiempo * 0.04, semilla), 4) - 0.5;        // orilla ondulada
    float rizos = nubosidad(dr * 7.5 + vec3(semilla, 0.0, Tiempo * 0.06), 3) - 0.5;        // orilla deshilachada
    float v = fract(2.0 * psi / (2.0 * PI) + 0.50 * ondas + 0.20 * rizos);
    // w: distancia a la orilla del brazo (0 en la orilla, negativo hacia dentro, positivo hacia fuera)
    float w = fract(v - 0.9 + 0.5) - 0.5;
    // el grosor del brazo cambia a lo largo de él
    float grueso = 0.6 + 0.8 * nubosidad(dr * 1.9 + vec3(9.0, semilla, Tiempo * 0.02), 3);
    float subida = w < 0.0 ? exp(-w * w / pow(0.24 * grueso, 2.0)) : exp(-w * w / pow(0.08 * grueso, 2.0));   // se apaga despacio hacia dentro y rápido hacia fuera
    float pliegues = nubosidad(dr * 5.0 + vec3(3.0, semilla, Tiempo * 0.03), 3);   // zonas más y menos claras dentro
    float cortina = subida * (0.55 + 0.75 * pliegues);
    float orilla = exp(-w * w / pow(0.065 * grueso, 2.0));
    // cada brazo tiene tramos fuertes y tramos que casi se apagan
    float tramo = smoothstep(0.30, 0.68, nubosidad(dr * 1.7 + vec3(semilla, 7.0, Tiempo * 0.03), 3));
    cortina *= 0.25 + 0.75 * tramo;
    orilla *= 0.20 + 0.80 * tramo;

    // los brazos nacen alrededor del ojo y aparecen a medida que la mancha cubre el cielo; hacia el horizonte se
    // apagan un poco
    float aparece = smoothstep(0.7, 1.9, Cubre) * smoothstep(0.10, 0.40, th) * (1.0 - 0.35 * smoothstep(1.2, 1.9, th));
    // mientras aparecen, primero se ven como vetas oscuras dentro de la mancha
    color *= 1.0 - 0.35 * (1.0 - aparece) * smoothstep(0.10, 0.40, th) * (1.0 - subida);

    color = mix(color, vec3(0.44, 0.005, 0.02), clamp(cortina, 0.0, 1.0) * aparece);
    color = mix(color, vec3(0.86, 0.07, 0.06), orilla * aparece * (0.65 + 0.35 * pliegues));
    color = mix(color, vec3(0.96, 0.50, 0.48), orilla * aparece * smoothstep(0.62, 0.85, pliegues) * 0.45);

    // brasas: puntitos rojos sueltos que flotan
    vec3 bq = d * 60.0 + vec3(0.0, Tiempo * 0.4, 0.0);
    vec3 bc = floor(bq);
    float bh = float(mezclar(uvec3(ivec3(bc) + 4096)).x) / 4294967295.0;
    if (bh > 0.99) {
        float bd = length(fract(bq) - 0.5);
        color = mix(color, vec3(0.95, 0.06, 0.05), smoothstep(0.20, 0.05, bd) * aparece * (0.6 + 0.4 * sin(Tiempo * 3.0 + bh * 90.0)));
    }

    return vec4(color, opacidad);
}

// ------------------------------------------------------------ grieta

// línea central de la grieta (desvío de lado) a la distancia x de su centro: ondas grandes más quiebros pequeños
float centro(float x, float s) {
    float ondas = 0.07 * (ondula(x * 2.5 + s) - 0.5) + 0.025 * (ondula(x * 9.0 + s * 2.0) - 0.5);
    float quiebros = 0.018 * (ondula(x * 31.0 + s * 3.0) - 0.5) + 0.008 * (ondula(x * 83.0 + s * 5.0) - 0.5);
    return ondas + quiebros;
}

// medio ancho de la grieta abierta a la distancia x del centro (el lado: +1 arriba, -1 abajo)
float medioAncho(float x, float lado, float s) {
    float u = clamp(abs(x) / LARGO, 0.0, 1.0);
    float cuerpo = ANCHO * pow(1.0 - u * u, 0.85);
    float ensanche = 0.17 * exp(-x * x / (0.42 * 0.42));          // el hueco grande del medio, donde va la cabeza
    float rasgado = 1.0 + 0.35 * (ondula(x * 11.0 + lado * 17.0 + s) - 0.5);
    if (lado < 0.0) {
        // la orilla de abajo va mucho más cerrada: tapa la cabeza de Reviil hasta casi la mitad
        return (cuerpo * 0.5 + ensanche * 0.08) * rasgado;
    }
    return (cuerpo + ensanche) * rasgado;
}

// luz de una línea de neón a distancia d (rad) con grosor g: {núcleo casi blanco, franja roja, resplandor}
vec3 neon(float d, float g) {
    return vec3(exp(-d / (0.25 * g)), exp(-d / g), exp(-d / (5.0 * g)));
}

// violeta oscuro (añil y malva) con nebulosa y algunas estrellas
vec3 espacio(vec2 q) {
    // el espacio que se ve por la grieta: azul muy profundo con nebulosas moradas que se mueven despacio y estrellas
    float n1 = nubosidad(vec3(q * 2.2, Tiempo * 0.03 + 5.0), 4);
    float n2 = nubosidad(vec3(q * 4.5 + 3.0, Tiempo * 0.05 + 11.0), 3);
    vec3 c = mix(vec3(0.02, 0.03, 0.14), vec3(0.08, 0.12, 0.42), smoothstep(0.30, 0.80, n1));
    c = mix(c, vec3(0.36, 0.10, 0.58), 0.75 * smoothstep(0.45, 0.85, n2));        // nubes moradas
    c = mix(c, vec3(0.20, 0.30, 0.85), 0.35 * smoothstep(0.62, 0.95, n1 * n2 * 1.6)); // brillos azules
    c += vec3(0.25, 0.05, 0.35) * 0.35 * smoothstep(0.05, 0.0, abs(abs(q.y) - 0.12)); // morado hacia las orillas

    // estrellas: pequeñas, blancas y azuladas, que titilan
    vec2 g = (q + vec2(Tiempo * 0.006, Tiempo * 0.002)) * 90.0;
    vec2 celda = floor(g);
    float h = azar2(celda);
    if (h > 0.86) {
        vec2 sitio = vec2(azar2(celda + 17.0), azar2(celda + 31.0)) * 0.6 + 0.2;
        float dist = length(fract(g) - sitio);
        float titila = 0.6 + 0.4 * sin(Tiempo * (1.5 + 4.0 * h) + h * 50.0);
        vec3 tono = mix(vec3(0.75, 0.82, 1.0), vec3(1.0), azar2(celda + 7.0));
        c = mix(c, tono, smoothstep(0.10 + 0.08 * (h - 0.86) / 0.14, 0.0, dist) * titila);
    }

    return c;
}

// cuánto se mete este píxel dentro de la grieta (< 0 = dentro); lo deja grieta() para las pasadas de la cabeza
float g_dentro = 1.0;
// 1 cerca de la grieta y 0 en los bordes de la zona donde se calcula: todo lo que brilla alrededor (resplandor, neón,
// chispas, vetas) se apaga suave antes de llegar a esos bordes. Antes se cortaba de golpe y se veía un rectángulo
// semitransparente alrededor de toda la grieta
float g_marco = 0.0;
// 1 a lo largo de la grieta y baja suave pasada su punta: lo que brilla alrededor (vetas, resplandor) ya no se corta en
// línea recta justo donde termina la grieta
float g_punta = 0.0;

// solo la forma de la grieta (sin colores): < 0 si el punto está dentro de la grieta abierta. Para la pasada del espacio de
// dentro (Grieta.z = 1), que así no tiene que calcular la tormenta, el neón ni nada más (es la mitad del coste cuando la
// grieta está abierta, que es casi todo el momento)
float soloDentro(vec2 p) {
    if (Grieta.y <= 0.0) {
        return 1.0;
    }
    vec2 q = rota2(p, -GrietaForma.x);
    if (abs(q.x) >= LARGO || abs(q.y) > 0.7) {
        return 1.0;
    }
    float s = GrietaForma.y;
    float x = q.x;
    float y = q.y - centro(x, s);
    float lado = y >= 0.0 ? 1.0 : -1.0;
    float u = abs(x) / LARGO;
    float abierta = clamp(Grieta.y * 1.5 - u * 0.5, 0.0, 1.0);
    abierta = abierta * abierta * (3.0 - 2.0 * abierta);
    return abs(y) - medioAncho(x, lado, s) * abierta;
}

vec4 grieta(vec2 p) {
    float largo = Grieta.x;
    float abre = Grieta.y;
    if (largo <= 0.0 && abre <= 0.0) {
        return vec4(0.0);
    }

    float s = GrietaForma.y;
    vec2 q = rota2(p, -GrietaForma.x);
    g_marco = 0.0;
    if (abs(q.x) > LARGO + 0.6 || abs(q.y) > 0.7) {
        return vec4(0.0);
    }
    g_marco = (1.0 - smoothstep(LARGO + 0.25, LARGO + 0.58, abs(q.x))) * (1.0 - smoothstep(0.38, 0.68, abs(q.y)));

    float x = q.x;
    float y = q.y - centro(x, s);
    float lado = y >= 0.0 ? 1.0 : -1.0;
    float u = abs(x) / LARGO;
    // el rayo va de un extremo al otro; se abre primero en el medio y luego hacia las puntas
    float alcanza = 1.0 - smoothstep(largo * LARGO - 0.03, largo * LARGO, abs(x));
    float abierta = clamp(abre * 1.5 - u * 0.5, 0.0, 1.0);
    abierta = abierta * abierta * (3.0 - 2.0 * abierta);
    float dentro = abs(y) - medioAncho(x, lado, s) * abierta;   // < 0 dentro de la grieta

    vec3 color = vec3(0.0);
    float alfa = 0.0;
    g_punta = 1.0 - smoothstep(0.92, 1.35, u);
    // pasada la punta nunca cuenta como "dentro" (la cabeza no se ve por ahí), pero sigue midiendo la distancia para que
    // las vetas y el resplandor se apaguen suave en vez de cortarse
    g_dentro = (u < 1.0) ? dentro : max(dentro, 0.001);
    if (dentro < 0.0 && u < 1.0) {
        if (Grieta.z > 1.5) {
            // pasada de encima: por dentro solo las orillas encendidas; el resto queda transparente y se ve la cabeza
            float orilla = exp(dentro / 0.02) * 0.9;
            color = vec3(1.0, 0.25, 0.15);
            alfa = orilla;
        } else {
            color = espacio(q);
            // las orillas por dentro, encendidas
            color += vec3(1.0, 0.25, 0.15) * exp(dentro / 0.02) * 0.9;
            alfa = 1.0;
        }
    }

    // borde de neón del rayo / grieta
    vec3 luz = neon(max(dentro, -dentro * 2.0), 0.010 + 0.022 * abierta) * alcanza;
    // en la punta: la línea de neón termina enseguida, pero su resplandor ancho se apaga más despacio (antes se cortaba
    // de golpe y dejaba una frontera recta y oscura justo donde acaba la grieta)
    luz.xy *= 1.0 - smoothstep(0.97, 1.08, u);
    luz.z *= 1.0 - smoothstep(0.95, 1.35, u);


    // ramas del rayo: tres bifurcaciones finas que salen del rayo y se apagan cuando la grieta se abre
    for (int i = 0; i < 3; i++) {
        float fi = float(i);
        float xb = mix(-0.75, 0.75, (fi + 0.2 + 0.6 * azar(s * 13.0 + fi * 5.0)) / 3.0);
        float sx = azar(s * 7.0 + fi * 3.0) > 0.5 ? 1.0 : -1.0;
        float lb = 0.16 + 0.16 * azar(s * 11.0 + fi * 9.0);
        float ub = (x - xb) * sx / lb;                             // 0 en el arranque, 1 en la punta
        if (ub >= 0.0 && ub <= 1.0) {
            float lado2 = mod(fi, 2.0) < 0.5 ? 1.0 : -1.0;
            float yb = centro(xb, s) + lado2 * (0.45 + 0.4 * azar(s + fi * 17.0)) * lb * ub
                + 0.016 * (ondula(x * 47.0 + fi * 13.0) - 0.5) * ub;
            // crece después de que el rayo pasa por su arranque
            float crece = clamp((largo * LARGO - abs(xb)) / 0.12, 0.0, 1.0);
            float vis = step(ub, crece) * (1.0 - abierta) * (1.0 - 0.3 * ub);
            vec3 rl = neon(abs(q.y - yb), 0.0065) * vis;
            luz = max(luz, rl);
        }
    }

    // chispas que se alejan de las orillas abiertas
    float chispas = 0.0;
    if (abierta > 0.05 && dentro > 0.0 && dentro < 0.5 && u < 1.05) {
        vec2 g = vec2(x * 24.0, (dentro - 0.02 * Tiempo) * 24.0 + lado * 23.0);
        vec2 celda = floor(g);
        float h = azar2(celda + 3.0);
        if (h > 0.72) {
            vec2 sitio = vec2(azar2(celda + 41.0), azar2(celda + 59.0)) * 0.6 + 0.2;
            float dist = length(fract(g) - sitio);
            chispas = smoothstep(0.30, 0.0, dist) * exp(-dentro / 0.30) * abierta * (0.6 + 0.4 * sin(Tiempo * 7.0 + h * 40.0))
                * (1.0 - smoothstep(0.9, 1.05, u));
        }
    }

    // resplandor rojo alrededor de la grieta abierta
    float aura = exp(-max(dentro, 0.0) / 0.25) * abierta * (1.0 - smoothstep(0.75, 1.25, u)) * 0.75;

    // borde como el de la referencia: núcleo crema muy fino, ribete naranja y luego la franja roja con su resplandor
    vec3 crema = mix(vec3(1.0, 0.86, 0.66), vec3(1.0, 0.95, 0.82), Grieta.w);
    vec3 naranja = vec3(1.0, 0.42, 0.10);
    if (dentro < 0.0 && u < 1.0) {
        // por dentro de la grieta no hay resplandor rojo: se ve limpio el espacio azul y morado (y la cabeza)
        aura = 0.0;
        luz.z *= exp(dentro / 0.015);
    }
    // fuera de la grieta, todo lo que brilla se apaga suave hacia los bordes de la zona (sin cortes rectos)
    if (!(dentro < 0.0 && u < 1.0)) {
        luz *= g_marco;
        aura *= g_marco;
        chispas *= g_marco;
    }
    float rojo = clamp(max(luz.z * 0.95, aura), 0.0, 1.0);
    color = mix(color, vec3(0.85, 0.02, 0.04), rojo * (1.0 - 0.88 * alfa));   // dentro de la grieta casi no tiñe de rojo
    color = mix(color, vec3(1.0, 0.16, 0.10), clamp(chispas * 1.8, 0.0, 1.0));
    color = mix(color, vec3(0.96, 0.10, 0.06), clamp(luz.y, 0.0, 1.0));
    color = mix(color, naranja, clamp(luz.x * 1.1, 0.0, 1.0));
    color = mix(color, crema, clamp(luz.x * luz.x * 1.4, 0.0, 1.0));
    alfa = max(alfa, clamp(max(max(rojo, luz.y), max(luz.x, chispas * 1.8)), 0.0, 1.0));
    return vec4(color, alfa);
}

void main() {
    vec3 d = normalize(direccion);
    float th = acos(clamp(d.y, -1.0, 1.0));
    float phi = atan(d.z, d.x);
    vec2 plano = d.xz / max(length(d.xz), 1e-5) * th;

    // OPTIMIZACIÓN: nada de discard (con discard la tarjeta no puede saltarse los píxeles tapados por el terreno ANTES de
    // calcular el cielo; con alfa 0 y la mezcla activa el resultado es el mismo, pero lo tapado no se calcula)
    if (Grieta.z > 0.5 && Grieta.z < 1.5) {
        // pasada 1: solo el espacio de dentro de la grieta (lo demás, transparente)
        if (soloDentro(plano) >= 0.0) {
            fragColor = vec4(0.0);
            return;
        }
        fragColor = vec4(espacio(rota2(plano, -GrietaForma.x)), 1.0);
        return;
    }

    vec4 cielo = Cubre > 0.0 ? tormenta(d, th, phi) : vec4(0.0);
    vec3 color = cielo.rgb;
    float alfa = cielo.a;

    // destello de cada golpe del rayo: la tormenta se enciende de rojo y brilla lo alto
    float destello = Grieta.w * alfa;
    color = mix(color, vec3(0.8, 0.03, 0.03), destello * 0.7);
    color += vec3(1.0, 0.45, 0.2) * exp(-th / 0.07) * destello;

    // onda expansiva de cada golpe: un anillo grueso de luz roja que sale de lo alto y se agranda. Brilla más por un
    // lado que por otro, deja un resplandor alrededor y, al pasar, aparta las nubes: por dentro el cielo queda más
    // oscuro un momento
    if (GrietaForma.w > 0.0) {
        float r = GrietaForma.z;
        float fuerza = GrietaForma.w;
        float grosor = 0.035 + 0.05 * r;
        float dd = th - r;
        float irregular = nubosidad(vec3(cos(phi) * 1.8, sin(phi) * 1.8, r * 2.5 + GrietaForma.y), 3);
        float lados = 0.35 + 1.0 * smoothstep(0.30, 0.70, irregular);
        float aro = exp(-dd * dd / (grosor * grosor)) * fuerza * lados;
        float halo = exp(-abs(dd) / (grosor * 3.0)) * fuerza * lados * 0.45;
        float hueco = (1.0 - smoothstep(r - 0.08, r - 0.01, th)) * fuerza * 0.55;
        color *= 1.0 - hueco * alfa;
        color = mix(color, vec3(0.75, 0.02, 0.03), clamp(halo, 0.0, 1.0) * alfa);
        color = mix(color, vec3(1.0, 0.12, 0.08), clamp(aro, 0.0, 1.0) * alfa);
    }

    vec4 g = grieta(plano);
    // Grieta.z: 0 = todo junto; 1 = solo el espacio de dentro de la grieta (va detrás de la cabeza);
    //           2 = todo menos el espacio de dentro (va delante de la cabeza: la tormenta y los bordes la tapan)
    if (Grieta.z > 1.5 && g_dentro < 0.0) {
        alfa = 0.0;    // dentro de la grieta no hay tormenta
    }
    // vetas: líneas finas rojas, como curvas de nivel de un mapa, que rodean el borde de la grieta sobre la tormenta.
    // (los campos y la derivada se calculan siempre, fuera de cualquier if, para que fwidth sea válido)
    // (solo con la grieta ya en el cielo: es una condición igual para todos los píxeles, así fwidth sigue siendo válido)
    if (Grieta.x > 0.0 || Grieta.y > 0.0) {
        vec2 qv = rota2(plano, -GrietaForma.x);
        float campoVetas = nubosidad(vec3(qv * 3.2, GrietaForma.y * 1.7 + Tiempo * 0.015), 3);
        float nivel = campoVetas * 9.0;
        float anchoPx = fwidth(nivel);
        float distNivel = abs(fract(nivel + 0.5) - 0.5);
        float linea = 1.0 - smoothstep(anchoPx * 0.7, anchoPx * 1.9, distNivel);
        float trozos = smoothstep(0.42, 0.62, nubosidad(vec3(qv * 2.0 + 7.0, GrietaForma.y + Tiempo * 0.01), 2));
        float cerca = g_dentro > 0.0 ? exp(-g_dentro / 0.30) * (1.0 - smoothstep(0.55, 0.9, g_dentro)) * g_marco * g_punta : 0.0;
        float vetas = clamp(linea * trozos * cerca * 1.3, 0.0, 1.0) * alfa;
        color = mix(color, vec3(0.80, 0.08, 0.05), vetas);
        // el resplandor de la franja roja baña la tormenta cerca del borde
        color += vec3(0.28, 0.015, 0.01) * cerca * 0.55 * alfa;
    }

    if (g.a > 0.0) {
        float juntos = g.a + alfa * (1.0 - g.a);
        color = (g.rgb * g.a + color * alfa * (1.0 - g.a)) / juntos;
        alfa = juntos;
    }

    if (alfa < 0.002) {
        fragColor = vec4(0.0);
        return;
    }

    // un grano casi invisible: en los rojos muy oscuros no se ven escalones de color
    color += (azar2(gl_FragCoord.xy) - 0.5) / 255.0;
    fragColor = vec4(color, alfa);
}
