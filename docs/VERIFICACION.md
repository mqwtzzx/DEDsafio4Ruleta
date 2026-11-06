# Qué se comprobó (y qué no)

**El proyecto no se ha compilado ni arrancado.** En el entorno donde se preparó no había acceso a los servidores de
Mojang ni de Fabric (Gradle no puede descargar Minecraft, Loom ni Fabric API). Todas las comprobaciones de abajo son
estáticas: revisan el código y los ficheros, no lo ejecutan.

## Resultados

| Comprobación | Resultado |
|---|---|
| Sintaxis Java (analizador de javac, sin resolver tipos) de los 219 ficheros | 0 errores |
| Cobertura: cada una de las 200 clases del JAR original tiene equivalente | 191 directas; 7 clases anónimas presentes como clases anónimas; las 2 del bus MOD del cliente (`RuletaClient$ModBus`, `AgujeroCliente$Registro`) están dentro de `RuletaClient.onInitializeClient` |
| API de Minecraft 1.21.1 usada (nombres oficiales de Mojang): imports, `Clase.miembro`, `variable.metodo(…)` con tipo declarado y número de argumentos, `new Clase(…)`, `Clase::metodo`, y en los mixins `@Shadow`, `@Accessor`, `method = "…"` y los descriptores de `target` | 0 avisos |
| API de Fabric API 1.21.1 usada (imports y miembros) contra su código fuente | 0 avisos |
| Textos: cada cadena de cada clase original aparece en el código portado | Todas salvo 23, explicadas abajo |
| Recursos contra el JAR original (SHA-256) | 1410 ficheros: 1401 idénticos, 6 movidos de carpeta (mismos bytes), 3 convertidos; 0 problemas; ningún fichero del JAR fuera de la tabla |
| `fabric.mod.json` y las dos configuraciones de mixins | JSON válido; cada mixin listado existe y cada fichero de mixin está listado |

### Las 23 cadenas que no aparecen tal cual

- 13 son nombres de constantes de enum (`AMARILLO`, `AZUL`, … de `RuletaColor`; `EON`, `GOLEM`, … de `TotemItem.Kind`):
  el compilador las genera solas y las constantes siguen en el código.
- 4 son `` escrito como escape en el original y como el propio carácter en el portado (es la misma cadena).
- 5 son los UUID y nombres de los modificadores de atributos: en 1.21.1 los modificadores se identifican con un
  `ResourceLocation` (ver CAMBIOS_PORT.md, sección 3).
- 1 es el aviso de bunkers, que ahora nombra la carpeta `structure` de 1.21 en vez de `structures`.

## Con qué datos

- Firmas de Minecraft 1.21.1 y 1.20.1 (clases, campos, métodos con tipos de parámetros, constructores y jerarquía) a
  partir de los mapeos oficiales y de mappings.dev, y los datos de Parchment 1.21.1.
- Código fuente de Fabric API de la rama 1.21.1, de JOML y del ejemplo oficial de mod de Fabric (para la estructura Gradle).
- Parches de Forge 1.20.x y de NeoForge 1.21.1 para saber dónde lanzaba Forge cada evento.
- El JAR original (`.class` leídos con `javap` y con un lector de bytecode propio; recursos copiados byte a byte).

## Qué no se pudo comprobar

- Que compile: tipos de retorno, genéricos, sobrecargas por tipo de argumento y llamadas encadenadas (`a.b().c()`) no
  se comprueban del todo sin compilar.
- Que los mixins apliquen: sus puntos de inyección (ver CAMBIOS_PORT.md, sección 6) dependen del bytecode de 1.21.1.
- Que se vea y funcione igual en el juego (efectos de pantalla, shaders, sonidos, comandos, momentos, ruleta…).

## Cómo repetir la comprobación de recursos

```
python3 tools/verificar_integridad.py ruta/al/DEDsafio4RuletaFanProject-1_1_0.jar
```
