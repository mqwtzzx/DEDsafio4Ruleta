# DEDsafio 4 Ruleta Fan Project — versión Fabric 1.21.1

Este es el mod **DEDsafio4RuletaFanProject 1.1.0** (Forge 1.20.1) pasado a **Fabric para Minecraft 1.21.1**.
Es un **proyecto de código fuente** (Gradle + Fabric Loom), listo para compilarlo. **No es un .jar**: en el
entorno donde se preparó no había acceso a los servidores de Mojang/Fabric, así que **no se ha compilado ni
probado en el juego**. Leer «Estado» más abajo.

- JAR original: `DEDsafio4RuletaFanProject-1_1_0.jar`, SHA-256 `1bf15a2e1e3acc50682d6194ff0683b8ebf5e6c6c02332e455334685e72cab33`.
- El JAR no traía el código fuente. El código Java se ha reconstruido a partir de los `.class` del JAR y se ha
  reescrito para Fabric 1.21.1 clase por clase, manteniendo la lógica, los textos, los números y los nombres.
  Las 200 clases del original tienen su equivalente (ver `docs/VERIFICACION.md`).
- Los recursos (texturas, modelos, sonidos, idiomas, shaders, estructuras, datos…) están **byte a byte igual**
  que en el JAR, salvo 3 ficheros cuyo formato cambió en 1.21 y 6 que solo cambian de carpeta.
  Cada uno lleva su SHA-256 en `docs/INTEGRIDAD_RECURSOS.tsv`.

## Versiones

| | |
|---|---|
| Minecraft | 1.21.1 (exacta) |
| Fabric Loader | 0.19.3 o posterior |
| Fabric API | 0.116.17+1.21.1 |
| Loom | 1.17.21 (plugin `net.fabricmc.fabric-loom-remap`; para que Gradle se ejecute con Java 21) |
| Gradle | 9.8.2 (wrapper incluido) |
| Java | 21 |
| Mapeos | oficiales de Mojang |

Los IDs no cambian: mod `ruleta` y los espacios de nombres de recursos `ruleta`, `supernova` y `nuclearcreeper`.

## Cómo compilar

1. Instalar un JDK 21.
2. En esta carpeta: `./gradlew build` (en Windows: `gradlew.bat build`).
3. El mod queda en `build/libs/DEDsafio4RuletaFanProject-fabric-1.21.1-1.1.0.jar` (el que no termina en `-sources`).
4. Para jugar: copiar ese `.jar` y **Fabric API para 1.21.1** a la carpeta `mods` de una instalación de
   Fabric Loader para Minecraft 1.21.1. Es necesario en el cliente y en el servidor.

Para probarlo desde el proyecto: `./gradlew runClient` (o `runServer`).

Sin instalar nada: subir esta carpeta a un repositorio de GitHub; el flujo `.github/workflows/build.yml` lo compila
con Java 21 y deja el `.jar` en los artefactos de la ejecución (pestaña «Actions»).

Si Gradle no encuentra alguna versión (por ejemplo el Loom `1.17.21`), ajustar `gradle.properties` con
las versiones que indique https://fabricmc.net/develop para 1.21.1. No hace falta tocar nada más.

## Para quien lo compile (ChatGPT u otra persona)

- **No cambiar nada de `src/main/resources`**: son los ficheros originales. Para comprobarlo:
  `python3 tools/verificar_integridad.py ruta/al/DEDsafio4RuletaFanProject-1_1_0.jar` (debe decir 0 problemas).
- Si aparece algún error de compilación, será de API (una firma o un nombre de 1.21.1 distinto del esperado):
  corregir esa llamada con la API de 1.21.1 conservando lo que hace; no quitar funciones ni simplificar.
- Si al arrancar falla un mixin, el registro dice qué mixin y qué punto de inyección. Están explicados en
  `docs/CAMBIOS_PORT.md` (qué evento de Forge sustituye cada uno y dónde lo lanzaba Forge).
- La carpeta `compat/` imita lo que el mod usaba de Forge (bus de eventos, `@SubscribeEvent`, eventos,
  `DeferredRegister`, `RegistryObject`…) para que el código original se conserve casi tal cual.

## Estructura

```
src/main/java      código común (servidor y cliente)
  net/ruleta/...         el mod (ruleta, momentos, cambios, bunkers, eclipse, agujero negro, phora…)
  net/ruleta/nuclear     Creeper nuclear, tótems y corazones (recursos «nuclearcreeper»)
  com/supernova          Supernova (recursos «supernova»)
  net/ruleta/compat      capa que sustituye a Forge (Bus, eventos, registros, Puente, Ganchos)
  net/ruleta/mixin       mixins comunes: lanzan los eventos que Fabric API no tiene
src/client/java    código solo de cliente (pantallas, HUD, efectos, shaders, renderizadores)
  net/ruleta/client/RuletaClient.java   punto de entrada del cliente
  net/ruleta/compat/client              eventos de cliente de Forge
  net/ruleta/mixin/client               mixins de cliente
src/main/resources   recursos originales + fabric.mod.json + ruleta.mixins.json
src/client/resources ruleta.client.mixins.json
docs/
  INTEGRIDAD_RECURSOS.tsv   ruta original, ruta nueva, estado y SHA-256 de cada recurso
  CAMBIOS_PORT.md           qué se cambió y por qué; diferencias conocidas
  VERIFICACION.md           qué se comprobó y qué no se pudo comprobar
  original_forge/           mods.toml y MANIFEST.MF del JAR original (sin tocar, como referencia)
tools/verificar_integridad.py
```

## Estado

- **Sin compilar y sin probar.** Todo lo que se dice aquí sale de revisar el código, no de ejecutarlo.
- Comprobado sin compilar: sintaxis de los 219 ficheros Java (0 errores), que todas las clases del original
  tienen equivalente, que los textos del original siguen en el código, que los nombres de clases y métodos de
  Minecraft 1.21.1 y de Fabric API usados existen (comprobación automática parcial) y que los recursos son los
  originales. Detalle en `docs/VERIFICACION.md`.
- Lo más delicado son los **mixins de cliente** (sobre todo el del mapa de luz del eclipse) y los de servidor
  que sustituyen eventos de Forge: sus puntos de inyección se eligieron según el código de Forge/NeoForge y
  los datos de 1.21.1, pero sin poder ver el código de Minecraft 1.21.1 ni arrancar el juego.

## Ajuste de compilación (6 de noviembre de 2026)

Para mantener el requisito de **compilar con Java 21**, se cambió solamente
`gradle.properties` de Loom `1.19-SNAPSHOT` a `1.17.21`, una versión estable
compatible con la JVM 21. Loom 1.18 y posteriores requieren Java 25 para
ejecutar Gradle, aunque el mod resultante siga siendo Java 21.

El error `UnknownHostException: services.gradle.org` no se corrige cambiando
código Java: significa que el equipo que ejecuta Gradle no tiene resolución
DNS o salida a Internet. Este paquete NO incluye un mod compilado.

En Windows abre `COMPILAR_JAR_JAVA21.bat` (con JDK 21 instalado).
Si tienes Internet, el wrapper descargará Gradle y Loom, ejecutará
`gradlew.bat build` y el JAR estará en `build/libs/`.

GitHub Actions (`.github/workflows/build.yml`) también compila con Java 21,
sube el JAR si Gradle termina correctamente y adjunta el registro de
compilación aunque falle.

**No se ha demostrado BUILD SUCCESSFUL ni compatibilidad en juego.**
No se ha modificado ningún archivo de `src/main/resources`.
