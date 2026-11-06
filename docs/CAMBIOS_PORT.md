# Cambios del port Forge 1.20.1 → Fabric 1.21.1

Objetivo: que el mod haga lo mismo que el original. Donde Fabric o Minecraft 1.21.1 obligan a cambiar algo,
aquí se dice qué y por qué. Las diferencias de comportamiento conocidas están en la sección 5.

## 1. Cómo se sustituye Forge

| En Forge 1.20.1 | En este proyecto |
|---|---|
| `@Mod("ruleta")`, constructor con el bus del mod | `net.ruleta.Ruleta implements ModInitializer` (fabric.mod.json → `main`) |
| `FMLClientSetupEvent` y demás suscriptores del bus MOD del cliente | `net.ruleta.client.RuletaClient implements ClientModInitializer` (→ `client`) |
| `MinecraftForge.EVENT_BUS`, `@SubscribeEvent`, `@Mod.EventBusSubscriber` | `compat.Bus` + `compat.SubscribeEvent` (mismas prioridades, `receiveCanceled`, cancelación y `Event.Result`; los manejadores de una superclase reciben también los eventos de sus subclases, como en Forge). Cada clase suscrita se registra a mano con `Bus.register(Clase.class)` en `Ruleta` / `RuletaClient` |
| Eventos de Forge | Clases con el mismo nombre y los mismos métodos en `compat.event` y `compat.client`, publicadas desde Fabric API (`compat.Puente`, `compat.client.PuenteCliente`) o desde mixins (sección 2) |
| `DeferredRegister` / `RegistryObject` | `compat.DeferredRegister` / `compat.RegistryObject` (registran al momento; `get()` y `holder()`) |
| `SimpleChannel` (`ruleta:main` protocolo "6", `supernova:sync`, `nuclearcreeper:main`) | Un payload de Fabric por mensaje (`PayloadTypeRegistry` + `ServerPlayNetworking` / `ClientPlayNetworking`). Mismos datos en el mismo orden. Los receptores del cliente corren en el hilo del cliente (como `consumerMainThread`/`enqueueWork`) |
| `DistExecutor.unsafeRunWhenOn(Dist.CLIENT, …)` | El receptor de cliente llama directamente a la clase de cliente (`RuletaClient.registrarReceptores`) |
| `Entity.getPersistentData()` | `compat.DatosPersistentes` (mixin de `Entity`); se guarda con la misma clave `"ForgeData"` |
| `ServerLifecycleHooks.getCurrentServer()` | `compat.Puente.servidor()` |
| `FMLPaths.CONFIGDIR` | `FabricLoader.getConfigDir()` (la misma carpeta `config`) |
| `RegisterKeyMappingsEvent` | `KeyBindingHelper.registerKeyBinding` |
| `EntityRenderersEvent.RegisterRenderers` / `RegisterLayerDefinitions` | `EntityRendererRegistry` / `EntityModelLayerRegistry` / `BlockEntityRendererRegistry` |
| `RegisterShadersEvent` | `CoreShaderRegistrationCallback` (Fabric API permite shaders con espacio de nombres `ruleta:` / `supernova:`; los JSON de los shaders no se tocan) |
| `RegisterDimensionSpecialEffectsEvent` (efectos del Overworld → `EfectosEclipse`) | Se pone en el mapa `DimensionSpecialEffects.EFFECTS` (accessor). `DimensionRenderingRegistry` de Fabric no reemplaza efectos ya existentes |
| `IDimensionSpecialEffectsExtension` (`adjustLightmapColors`, `renderSky`, `renderClouds`) | Métodos normales de `EfectosEclipse` llamados por los mixins de `LightTexture` y `LevelRenderer` |
| `IClientFluidTypeExtensions` (textura y tinte de fluidos en el mapa) | `FluidRenderHandlerRegistry` de Fabric API |
| `ForgeSpawnEggItem` | `SpawnEggItem` de vanilla (mismos colores) |
| `RegisterClientCommandsEvent` | `ClientCommandRegistrationCallback`; los comandos de cliente usan `ClientCommandManager` y `FabricClientCommandSource` |
| `mods.toml` | `fabric.mod.json` (mismo id, nombre, versión, autor, créditos, licencia y descripción). El original está en `docs/original_forge/` |

## 2. Dónde se lanza ahora cada evento de Forge

Lado común/servidor (`compat.Puente` y `net.ruleta.mixin`):

| Evento | Punto |
|---|---|
| ServerTickEvent START/END | `ServerTickEvents` |
| PlayerTickEvent START/END | `PlayerMixin` (`Player.tick`, principio y final) |
| ServerStopping/Stopped | `ServerLifecycleEvents` |
| RegisterCommandsEvent | `CommandRegistrationCallback` |
| PlayerLoggedIn/Out, Clone, PlayerRespawn, PlayerChangedDimension | `ServerPlayerEvents`, `ServerEntityWorldChangeEvents` |
| EntityLeaveLevelEvent | `ServerEntityEvents.ENTITY_UNLOAD` |
| EntityJoinLevelEvent (servidor) | `PersistentEntitySectionManagerMixin` (`addEntity`, tras comprobar el UUID) |
| BlockEvent.BreakEvent | `PlayerBlockBreakEvents.BEFORE` |
| PlayerInteractEvent.LeftClickBlock / EntityInteract(Specific) | `AttackBlockCallback` / `UseEntityCallback` |
| PlayerInteractEvent.RightClickBlock | `ServerPlayerGameModeMixin` (`useItemOn`) |
| ServerChatEvent | `ServerMessageEvents.ALLOW_CHAT_MESSAGE` |
| LivingDeath, LivingHurt/Damage, LivingDrops, LivingUseTotem, LivingTick, visibilidad, LivingEntityUseItemEvent.Finish | `LivingEntityMixin`, `PlayerMixin`, `ServerPlayerMixin` |
| PlayerEvent.NameFormat | `PlayerMixin` (`getDisplayName`) |
| EntityTravelToDimensionEvent | `EntityMixin` / `ServerPlayerMixin` (`changeDimension`, aviso adelantado en `handlePortal`) |
| EntityTeleportEvent.EnderPearl | `ThrownEnderpearlMixin` (`onHit`) |
| ExplosionEvent.Start / Detonate | `LevelMixin` + `ServerLevelMixin` / `ExplosionMixin` |
| MobSpawnEvent.FinalizeSpawn (y aparición anulada) | `MobMixin` + `WorldGenRegionMixin` |
| BlockEvent.EntityPlaceEvent / EntityMultiPlaceEvent | `BlockItemMixin` / `CarvedPumpkinBlockMixin` |
| BlockEvent.FluidPlaceBlockEvent | `LiquidBlockMixin`, `LavaFluidMixin` |
| FillBucketEvent | `BucketItemMixin` |

Cliente (`compat.client.PuenteCliente` y `net.ruleta.mixin.client`):

| Evento | Punto (igual que Forge 1.20.1 salvo lo indicado) |
|---|---|
| ClientTickEvent START/END | `ClientTickEvents` |
| ClientPlayerNetworkEvent LoggingIn / LoggingOut | `ClientPlayConnectionEvents.JOIN` / `DISCONNECT` |
| ClientChatReceivedEvent | `ClientReceiveMessageEvents.MODIFY_GAME` (mensajes del sistema; ver 5) |
| RegisterClientCommandsEvent | `ClientCommandRegistrationCallback` |
| ChunkEvent.Load (cliente) | `ClientChunkEvents.CHUNK_LOAD` |
| RenderLevelStageEvent AFTER_TRANSLUCENT_BLOCKS | `LevelRendererMixin`: final de `renderSectionLayer(translucent)`, antes de `clearRenderState` |
| RenderLevelStageEvent AFTER_PARTICLES | `LevelRendererMixin`: justo después de cada `ParticleEngine.render` |
| RenderLevelStageEvent AFTER_LEVEL | `LevelRendererMixin`: al terminar `renderLevel` (Forge: en `GameRenderer`, nada más volver de `renderLevel`) |
| `renderSky` / `renderClouds` de los efectos de dimensión | `LevelRendererMixin`: al principio de `renderSky` / `renderClouds` |
| ViewportEvent.ComputeCameraAngles | `GameRendererMixin`: tras `camera.setup(...)`; se aplica con `CameraMixin` |
| ViewportEvent.ComputeFogColor | `FogRendererMixin`: final de `setupColor`, antes de `RenderSystem.clearColor` |
| ViewportEvent.RenderFog | `FogRendererMixin`: final de `setupFog` (si se cancela se aplican sus valores) |
| RenderGuiEvent.Pre / Post | `GuiMixin`: principio y final de `Gui.render` |
| RenderGuiOverlayEvent.Pre (PLAYER_HEALTH) | `GuiMixin`: principio de `Gui.renderHearts` |
| RenderNameTagEvent | `EntityRendererMixin`: principio de `EntityRenderer.render` (+ resultado y texto) |
| InputEvent.InteractionKeyMappingTriggered (usar) | `MinecraftMixin`: `startUseItem`, al empezar cada mano |
| PlaySoundEvent | `SoundEngineMixin`: `SoundEngine.play` |
| adjustLightmapColors | `LightTextureMixin` (`updateLightTexture`, antes de la visión nocturna, con los mismos argumentos que Forge) |

## 3. Cambios de Minecraft 1.20.1 → 1.21.1 aplicados

- `new ResourceLocation(ns, path)` → `ResourceLocation.fromNamespaceAndPath`.
- Objetos: NBT del objeto → componentes de datos. La linterna guarda `Carga` y `Prendida` dentro de
  `minecraft:custom_data` (donde Minecraft mueve el NBT antiguo al actualizar un mundo).
- Atributos: los modificadores se identifican por `ResourceLocation` y no por UUID:
  `ruleta:zombie_inteligente` (antes 5d1c7a0e-3b8f-4c52-9a6e-17f0c2b4e8a1), `nuclearcreeper:corazon_rojo`
  (antes 5b0f6a52-…, «Corazon Rojo») y `nuclearcreeper:totem_golem` (antes c3a1e7b4-…, «Totem Golem»).
- Efectos y sonidos como `Holder` (`hasEffect`/`getEffect` con `Efectos.X.holder()`, `SoundEvents.GENERIC_EXPLODE.value()`).
- `setSecondsOnFire` → `igniteForSeconds`; `broadcastBreakEvent` → `onEquippedItemBroken` / `hurtAndBreak` con hueco;
  `setMaxUpStep` → atributo `STEP_HEIGHT`; `Blocks.GRASS` → `SHORT_GRASS`; `getExploder()` → `getDirectSourceEntity()`;
  `use(...)` de bloques → `useWithoutItem`; `finalizeSpawn` sin `CompoundTag`; `findRespawnPositionAndUseSpawnBlock`
  → cálculo equivalente de `ServerPlayer` (`DimensionTransition`); comida `saturationMod`/`alwaysEat` → `saturationModifier`/`alwaysEdible`.
- Red: `FriendlyByteBuf.writeComponent`/`writeItem` → códecs de componentes y `ItemStack.OPTIONAL_STREAM_CODEC`.
- Datos: carpetas en singular (`tags/block`, `loot_table`, `recipe`, `structure`), recetas con `result.id`,
  `pack_format` 34. Por eso el aviso de bunkers dice `data/ruleta/structure/bunker.nbt`.
- Dibujo: la pila de modelo-vista es un `Matrix4fStack` de JOML; `Tesselator.begin(...)` +
  `addVertex/setColor/setUv/setOverlay/setLight/setNormal` (sin `endVertex`) y `buildOrThrow()`/`build()`;
  `PoseStack.mulPoseMatrix` → `mulPose(Matrix4f)`; `Model.renderToBuffer`/`ModelPart.render` con color ARGB en un `int`;
  `NativeImage`, `DynamicTexture`, `TextureTarget` y `upload` con sus nuevos argumentos.
- `DefaultVertexFormat.POSITION_COLOR_TEX` y su shader ya no existen: para los shaders propios del mod (phora,
  supernova ×3) se define `compat.client.Formatos.POSITION_COLOR_TEX` con los mismos elementos en el mismo orden
  (Position, Color, UV0); donde se usaba el shader de vanilla se usa `POSITION_TEX_COLOR` (mismo resultado: textura × color).
- Pantallas: `mouseScrolled` recibe desplazamiento horizontal y vertical (se usa el vertical); `Screen.render` de 1.21.1
  pinta el fondo por su cuenta, así que `renderBackground` se deja vacío y el fondo se pinta como en 1.20.1
  (`Fondo.oscurecer`); el mapa no tenía fondo y sigue sin tenerlo.
- `GameRenderer.loadEffect` es privado: `GameRendererAccessor`.
- `ChunkStatus` está en `net.minecraft.world.level.chunk.status`.
- Constructor protegido de `StairBlock`: subclase anónima con el mismo comportamiento.

## 4. Puntos que no cambian nada visible pero conviene saber

- Las clases del bus MOD `RuletaClient$ModBus` y `AgujeroCliente$Registro` no existen como clases: su contenido está
  en `RuletaClient.onInitializeClient`.
- `fabric.mod.json` exige Minecraft 1.21.1 exacta: el código usa API que cambió en 1.21.2.
- El mod tiene que estar en cliente y servidor (en Forge también: el canal `ruleta:main` era obligatorio).

## 5. Diferencias de comportamiento conocidas

- **Cámara (temblores):** Forge solo giraba la vista; en 1.21.1 la vista sale de la rotación de la cámara, así que la
  guiñada, la inclinación y el alabeo se aplican a la cámara (como NeoForge 1.21.1). Las partículas y la orientación
  del oyente de sonido siguen también ese temblor.
- **Corazones ocultos (miope, daltónico, vida oculta):** se ocultan los corazones (`Gui.renderHearts`). En Forge, al
  ocultar la capa de vida la barra de armadura bajaba una fila; en 1.21.1 se queda en su sitio.
- **Chat:** Fabric solo deja cambiar los mensajes del sistema. El mod solo cambiaba esos (muertes, entradas y salidas),
  así que no hay diferencia práctica.
- **Comandos de cliente:** responden en el chat local con `sendFeedback` de Fabric.
- **Mapa:** si un fluido de otro mod no tiene manejador de dibujo de Fabric, se usa el modelo del bloque.
- **RenderLevelStageEvent AFTER_SKY** no se publica: ningún manejador del mod lo usaba.
- **Mundos de la versión Forge:** no es un objetivo de este port, pero los datos de entidades se guardan con la misma
  clave `"ForgeData"` y los objetos con NBT antiguo los convierte Minecraft a `custom_data`.

## 6. Mixins cuyo punto de inyección depende del código de 1.21.1

No se pudo ver el código de Minecraft 1.21.1 ni arrancar el juego. Los puntos se eligieron con el código de Forge 1.20.1,
los parches de NeoForge 1.21.1 (que muestran que esas zonas no cambiaron), los mixins de Fabric API 1.21.1 y las
firmas de 1.21.1. Si alguno no aplica, el juego avisa al arrancar con el nombre del mixin:

- `LightTextureMixin`: cuenta píxeles con `Vector3f.set(FFF)` y aplica el ajuste en el primero de: `Vector3f.x()`
  (ordinal 0, visión nocturna), `Vector3f.add(FFF)` (ordinal 0, oscuridad) o `clampColor` (ordinal 1).
- `MinecraftMixin`: `LocalPlayer.getItemInHand` (ordinal 0) dentro de `startUseItem`, con la mano como variable local.
- `LevelRendererMixin`: `RenderType.clearRenderState` en `renderSectionLayer` y `ParticleEngine.render` en `renderLevel`.
- `GameRendererMixin`: `Camera.setup` en `renderLevel`.
- `FogRendererMixin`: `RenderSystem.clearColor` en `setupColor`.
- `EntityRendererMixin`: `shouldShowName` y `renderNameTag` en `EntityRenderer.render`.
- Mixins comunes (sección 2): mismos riesgos en sus puntos de inyección.
