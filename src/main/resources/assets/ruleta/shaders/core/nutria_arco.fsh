#version 150

// Código generado por Claude (Anthropic).
// Las nutrias con el mismo arcoíris que el mundo (el mismo giro de tono y la misma saturación que momento_nutria.fsh),
// pero SIN el zigzag ni las ondas: se quedan quietas en su sitio y solo cambian de color.

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float Giro;
uniform float Arco;

in vec2 uv;
out vec4 fragColor;

const float SATURACION = 1.25;

vec3 girarTono(vec3 c, float ang) {
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
    vec4 c = texture(Sampler0, uv) * ColorModulator;
    if (c.a < 0.004) {
        discard;
    }
    c.rgb = mix(c.rgb, clamp(girarTono(c.rgb, Giro), 0.0, 1.0), Arco);
    fragColor = c;
}
