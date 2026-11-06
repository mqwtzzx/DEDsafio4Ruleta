package net.ruleta.bunker;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.ruleta.Efectos;
import net.ruleta.Red;
import net.ruleta.compat.Event;
import net.ruleta.compat.EventPriority;
import net.ruleta.compat.SubscribeEvent;
import net.ruleta.compat.event.BlockEvent;
import net.ruleta.compat.event.EntityEvents.EntityJoinLevelEvent;
import net.ruleta.compat.event.ExplosionEvent;
import net.ruleta.compat.event.FillBucketEvent;
import net.ruleta.compat.event.PlayerEvent;
import net.ruleta.compat.event.PlayerInteractEvent;
import net.ruleta.compat.event.RegisterCommandsEvent;
import net.ruleta.compat.event.ServerLifecycle;
import net.ruleta.compat.event.TickEvent;
import net.ruleta.puerta.BloquesBunker;
import net.ruleta.puerta.PuertaBunker;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/** Búnkeres: /bunker set|build|list|delete, protección de sus bloques y zona segura dentro. */
public final class Bunkers {
	public static final int SX = 96;
	public static final int SY = 34;
	public static final int SZ = 99;
	public static final String NOMBRE_DEFECTO = "Bunker de Eón";
	private static final ResourceLocation ESTRUCTURA = ResourceLocation.fromNamespaceAndPath("ruleta", "bunker");
	private static final Map<UUID, Pendiente> PENDIENTES = new HashMap<>();
	private static BitSet interior;
	private static BitSet ocupado;
	public static final BlockPos[] PUERTAS = {new BlockPos(5, 3, 54), new BlockPos(45, 3, 7), new BlockPos(52, 3, 94), new BlockPos(92, 3, 47)};
	private static final List<Object[]> OBRAS = new ArrayList<>();
	@Nullable
	private static Bunker visible;

	/** Un búnker construido: nombre, dimensión y esquina (ex, ey, ez). */
	public record Bunker(String nombre, String dimension, int ex, int ey, int ez) {
		public double mx() {
			return (double) this.ex + 48.0D;
		}

		public double my() {
			return this.ey + SY + 3;
		}

		public double mz() {
			return (double) this.ez + 49.5D;
		}

		public String clave() {
			return this.dimension + "|" + this.ex + "|" + this.ey + "|" + this.ez;
		}

		boolean contiene(BlockPos p) {
			int x = p.getX() - this.ex;
			int y = p.getY() - this.ey;
			int z = p.getZ() - this.ez;
			return x >= 0 && y >= 0 && z >= 0 && x < SX && y < SY && z < SZ;
		}
	}

	private record Pendiente(String nombre, ResourceKey<Level> dimension, BlockPos esquina) {
	}

	private Bunkers() {
	}

	private static int i(int x, int y, int z) {
		return (y * SZ + z) * SX + x;
	}

	@SubscribeEvent
	public static void alRegistrar(RegisterCommandsEvent e) {
		CommandDispatcher<CommandSourceStack> d = e.getDispatcher();
		d.register(Commands.literal("bunker").requires(s -> s.hasPermission(2))
				.then(Commands.literal("set").executes(ctx -> set(ctx, NOMBRE_DEFECTO))
						.then(Commands.argument("nombre", StringArgumentType.greedyString()).executes(ctx -> set(ctx, StringArgumentType.getString(ctx, "nombre")))))
				.then(Commands.literal("build").executes(Bunkers::build))
				.then(Commands.literal("list").executes(Bunkers::list))
				.then(Commands.literal("delete").then(Commands.argument("nombre", StringArgumentType.greedyString())
						.suggests((ctx, b) -> SharedSuggestionProvider.suggest(datos(ctx.getSource().getServer()).lista.stream().map(Bunker::nombre).toList(), b))
						.executes(Bunkers::delete))));
	}

	private static String limpiar(String s) {
		String r = s.trim();
		while (r.length() >= 2 && (r.startsWith("\"") && r.endsWith("\"") || r.startsWith("'") && r.endsWith("'")))
			r = r.substring(1, r.length() - 1).trim();
		return r;
	}

	private static int set(CommandContext<CommandSourceStack> ctx, String crudo) throws CommandSyntaxException {
		ServerPlayer p = ctx.getSource().getPlayerOrException();
		String nombre = limpiar(crudo);
		if (nombre.isEmpty() || nombre.length() > 32) {
			ctx.getSource().sendFailure(Component.literal("El nombre tiene que tener entre 1 y 32 letras."));
			return 0;
		}
		BlockPos pie = p.blockPosition();
		BlockPos esquina = pie.offset(-48, 0, -49);
		PENDIENTES.put(p.getUUID(), new Pendiente(nombre, p.level().dimension(), esquina));
		BlockPos otra = esquina.offset(SX - 1, SY - 1, SZ - 1);
		ctx.getSource().sendSuccess(() -> Component.literal("«" + nombre + "» apuntado aquí. Ocupará de " + texto(esquina) + " a " + texto(otra) + ". Usa ")
				.withStyle(ChatFormatting.YELLOW)
				.append(Component.literal("/bunker build").withStyle(s -> s.withColor(ChatFormatting.GOLD).withUnderlined(Boolean.TRUE)
						.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bunker build"))))
				.append(Component.literal(" para construirlo.").withStyle(ChatFormatting.YELLOW)), false);
		return 1;
	}

	private static int build(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer p = ctx.getSource().getPlayerOrException();
		Pendiente pe = PENDIENTES.get(p.getUUID());
		if (pe == null) {
			ctx.getSource().sendFailure(Component.literal("Primero apunta dónde va con /bunker set [nombre]."));
			return 0;
		}
		MinecraftServer server = p.getServer();
		ServerLevel nivel = server.getLevel(pe.dimension());
		if (nivel == null)
			return 0;
		Optional<StructureTemplate> t = nivel.getStructureManager().get(ESTRUCTURA);
		if (t.isEmpty() || !mascaras(nivel)) {
			// en 1.21.1 la carpeta es data/ruleta/structure/ (en 1.20.1 era structures/)
			ctx.getSource().sendFailure(Component.literal("No se encontró la estructura del bunker (data/ruleta/structure/bunker.nbt)."));
			return 0;
		}
		ctx.getSource().sendSuccess(() -> Component.literal("Construyendo «" + pe.nombre() + "» y preparando el terreno... (unos segundos)").withStyle(ChatFormatting.GRAY), false);
		CommandSourceStack fuente = ctx.getSource();
		Bunker b = construir(nivel, pe.esquina(), pe.nombre(), hecho -> {
			if (hecho != null)
				fuente.sendSuccess(() -> Component.literal("«" + hecho.nombre() + "» construido.").withStyle(ChatFormatting.GREEN), true);
			else
				fuente.sendFailure(Component.literal("No se pudo construir el bunker ahí."));
		});
		if (b == null) {
			ctx.getSource().sendFailure(Component.literal("No se pudo construir el bunker ahí."));
			return 0;
		}
		PENDIENTES.remove(p.getUUID());
		return 1;
	}

	public static BlockPos puerta(Bunker b, int i) {
		return new BlockPos(b.ex(), b.ey(), b.ez()).offset(PUERTAS[i]);
	}

	@Nullable
	public static ServerLevel nivel(MinecraftServer server, Bunker b) {
		return server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(b.dimension())));
	}

	@Nullable
	public static Bunker elegirOConstruir(ServerPlayer admin) {
		MinecraftServer server = admin.getServer();
		List<Bunker> l = datos(server).lista;
		if (!l.isEmpty())
			return l.get(admin.getRandom().nextInt(l.size()));
		ServerLevel nivel = server.overworld();
		if (!mascaras(nivel))
			return null;
		double ang = admin.getRandom().nextDouble() * Math.PI * 2.0D;
		int cx = (int) Math.floor(admin.getX() + Math.cos(ang) * 1000.0D);
		int cz = (int) Math.floor(admin.getZ() + Math.sin(ang) * 1000.0D);
		int cy = nivel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz);
		String nombre = NOMBRE_DEFECTO;
		BlockPos esquina = new BlockPos(cx - 48, Math.max(cy, nivel.getSeaLevel()), cz - 49);
		return construir(nivel, esquina, nombre, null);
	}

	@Nullable
	static Bunker construir(ServerLevel nivel, BlockPos esquina, String nombre, @Nullable Consumer<Bunker> alTerminar) {
		if (nivel.getStructureManager().get(ESTRUCTURA).isEmpty() || !mascaras(nivel))
			return null;
		Bunker b = new Bunker(nombre, nivel.dimension().location().toString(), esquina.getX(), esquina.getY(), esquina.getZ());
		OBRAS.add(new Object[]{new Terreno.Obra(nivel, esquina, ocupado), b, alTerminar});
		return b;
	}

	@SubscribeEvent
	public static void alTickServidor(TickEvent.ServerTickEvent e) {
		if (e.phase != TickEvent.Phase.END || OBRAS.isEmpty())
			return;
		Object[] o = OBRAS.get(0);
		Terreno.Obra obra = (Terreno.Obra) o[0];
		if (!obra.paso())
			return;
		OBRAS.remove(0);
		Bunker b = (Bunker) o[1];
		@SuppressWarnings("unchecked")
		Consumer<Bunker> fin = (Consumer<Bunker>) o[2];
		ServerLevel nivel = obra.nivel();
		Optional<StructureTemplate> t = nivel.getStructureManager().get(ESTRUCTURA);
		BlockPos esquina = new BlockPos(b.ex(), b.ey(), b.ez());
		StructurePlaceSettings ajustes = new StructurePlaceSettings().setIgnoreEntities(false);
		boolean ok = t.isPresent() && t.get().placeInWorld(nivel, esquina, esquina, ajustes, nivel.getRandom(), 18);
		if (ok) {
			Datos d = datos(nivel.getServer());
			d.lista.add(b);
			d.setDirty();
			sincronizar(nivel.getServer());
		}
		if (fin != null)
			fin.accept(ok ? b : null);
	}

	private static int list(CommandContext<CommandSourceStack> ctx) {
		List<Bunker> l = datos(ctx.getSource().getServer()).lista;
		if (l.isEmpty()) {
			ctx.getSource().sendSuccess(() -> Component.literal("No hay ningún bunker construido.").withStyle(ChatFormatting.GRAY), false);
			return 0;
		}
		ctx.getSource().sendSuccess(() -> Component.literal("Bunkers (" + l.size() + "):").withStyle(ChatFormatting.GOLD), false);
		for (Bunker b : l) {
			int x = (int) Math.floor(b.mx());
			int y = b.ey() + 1;
			int z = (int) Math.floor(b.mz());
			String tp = "/execute in " + b.dimension() + " run tp @s " + x + " " + (b.ey() + SY + 1) + " " + z;
			Component linea = Component.literal(" • ").withStyle(ChatFormatting.GRAY)
					.append(Component.literal(b.nombre()).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
					.append(Component.literal("  " + x + ", " + y + ", " + z + "  (" + dim(b.dimension()) + ")").withStyle(ChatFormatting.GRAY))
					.withStyle(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, tp)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Clic para ir"))));
			ctx.getSource().sendSuccess(() -> linea, false);
		}
		return l.size();
	}

	private static int delete(CommandContext<CommandSourceStack> ctx) {
		String nombre = limpiar(StringArgumentType.getString(ctx, "nombre"));
		MinecraftServer server = ctx.getSource().getServer();
		Bunker b = null;
		double mejor = Double.MAX_VALUE;
		Vec3 desde = ctx.getSource().getPosition();
		for (Bunker k : datos(server).lista) {
			if (k.nombre().equalsIgnoreCase(nombre)) {
				double d = desde.distanceToSqr(k.mx(), k.my(), k.mz());
				if (d < mejor) {
					mejor = d;
					b = k;
				}
			}
		}
		if (b == null) {
			ctx.getSource().sendFailure(Component.literal("No hay ningún bunker llamado «" + nombre + "»."));
			return 0;
		}
		Datos d = datos(server);
		d.lista.remove(b);
		d.setDirty();
		sincronizar(server);
		Bunker borrado = b;
		ctx.getSource().sendSuccess(() -> Component.literal("«" + borrado.nombre() + "» borrado de la lista (su marcador ya no sale).").withStyle(ChatFormatting.YELLOW), true);
		return 1;
	}

	@Nullable
	public static Bunker porClave(MinecraftServer server, String clave) {
		if (clave == null || clave.isEmpty())
			return null;
		for (Bunker b : datos(server).lista)
			if (b.clave().equals(clave))
				return b;
		return null;
	}

	private static Bunker buscar(MinecraftServer server, String nombre) {
		for (Bunker b : datos(server).lista)
			if (b.nombre().equalsIgnoreCase(nombre))
				return b;
		return null;
	}

	private static String texto(BlockPos p) {
		return p.getX() + " " + p.getY() + " " + p.getZ();
	}

	private static String dim(String id) {
		return switch (id) {
			case "minecraft:overworld" -> "Overworld";
			case "minecraft:the_nether" -> "Nether";
			case "minecraft:the_end" -> "End";
			default -> id;
		};
	}

	/** Máscaras de la estructura: celdas de aire interiores y celdas con bloques propios del búnker. */
	private static boolean mascaras(ServerLevel nivel) {
		if (interior != null)
			return true;
		Optional<StructureTemplate> t = nivel.getStructureManager().get(ESTRUCTURA);
		if (t.isEmpty())
			return false;
		BitSet in = new BitSet(SX * SY * SZ);
		BitSet oc = new BitSet(SX * SY * SZ);
		StructurePlaceSettings s = new StructurePlaceSettings();
		for (StructureTemplate.StructureBlockInfo info : t.get().filterBlocks(BlockPos.ZERO, s, Blocks.AIR))
			marcar(in, info.pos());
		Block[] propios = {BloquesBunker.LADRILLO.get(), BloquesBunker.ESCALERA.get(), BloquesBunker.PISO.get(), BloquesBunker.PELIGRO.get(),
				PuertaBunker.BLOQUE.get(), PuertaBunker.BLOQUE_HOJA.get()};
		for (Block bl : propios)
			for (StructureTemplate.StructureBlockInfo info : t.get().filterBlocks(BlockPos.ZERO, s, bl))
				marcar(oc, info.pos());
		interior = in;
		ocupado = oc;
		return true;
	}

	private static void marcar(BitSet b, BlockPos p) {
		if (p.getX() >= 0 && p.getY() >= 0 && p.getZ() >= 0 && p.getX() < SX && p.getY() < SY && p.getZ() < SZ)
			b.set(i(p.getX(), p.getY(), p.getZ()));
	}

	@Nullable
	public static Bunker interiorDe(ServerLevel nivel, BlockPos p) {
		if (!mascaras(nivel))
			return null;
		String dim = nivel.dimension().location().toString();
		for (Bunker b : datos(nivel.getServer()).lista)
			if (b.dimension().equals(dim) && b.contiene(p) && interior.get(i(p.getX() - b.ex(), p.getY() - b.ey(), p.getZ() - b.ez())))
				return b;
		return null;
	}

	public static void sacar(ServerPlayer p, Bunker b) {
		double x = p.getX();
		double z = p.getZ();
		double aOeste = x - (double) b.ex();
		double aEste = (double) (b.ex() + SX) - x;
		double aNorte = z - (double) b.ez();
		double aSur = (double) (b.ez() + SZ) - z;
		double min = Math.min(Math.min(aOeste, aEste), Math.min(aNorte, aSur));
		if (min == aOeste)
			x = (double) b.ex() - 4.5D;
		else if (min == aEste)
			x = (double) (b.ex() + SX) + 3.5D;
		else if (min == aNorte)
			z = (double) b.ez() - 4.5D;
		else
			z = (double) (b.ez() + SZ) + 3.5D;
		ServerLevel nivel = p.serverLevel();
		BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos(x, b.ey(), z);
		for (int i = 0; i < 64 && (!nivel.getBlockState(q).isAir() || !nivel.getBlockState(q.above()).isAir()); i++)
			q.move(0, 1, 0);
		p.teleportTo(nivel, x, q.getY(), z, p.getYRot(), p.getXRot());
		p.displayClientMessage(Component.literal("El bunker se ha cerrado."), true);
	}

	public static boolean enCaja(Level nivel, BlockPos p) {
		if (!(nivel instanceof ServerLevel sl))
			return false;
		String dim = sl.dimension().location().toString();
		for (Bunker b : datos(sl.getServer()).lista)
			if (b.dimension().equals(dim) && b.contiene(p))
				return true;
		return false;
	}

	public static boolean protegido(Level nivel, BlockPos p, BlockState s) {
		return esBloqueDeBunker(s) && enCaja(nivel, p);
	}

	private static boolean esBloqueDeBunker(BlockState s) {
		Block b = s.getBlock();
		return b == BloquesBunker.LADRILLO.get() || b == BloquesBunker.ESCALERA.get() || b == BloquesBunker.PISO.get()
				|| b == BloquesBunker.PELIGRO.get() || b == PuertaBunker.BLOQUE.get() || b == PuertaBunker.BLOQUE_HOJA.get();
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void alRomper(BlockEvent.BreakEvent e) {
		if (esBloqueDeBunker(e.getState()) && e.getLevel() instanceof Level nivel && enCaja(nivel, e.getPos()))
			e.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void alPicar(PlayerInteractEvent.LeftClickBlock e) {
		if (esBloqueDeBunker(e.getLevel().getBlockState(e.getPos())))
			e.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void alUsarBloque(PlayerInteractEvent.RightClickBlock e) {
		Level nivel = e.getLevel();
		BlockPos donde = e.getPos().relative(e.getFace() == null ? Direction.UP : e.getFace());
		if (enCaja(nivel, e.getPos()) || enCaja(nivel, donde))
			e.setUseItem(Event.Result.DENY);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void alCubo(FillBucketEvent e) {
		if (e.getTarget() instanceof BlockHitResult hit && hit.getType() != HitResult.Type.MISS) {
			Level nivel = e.getLevel();
			if (enCaja(nivel, hit.getBlockPos()) || enCaja(nivel, hit.getBlockPos().relative(hit.getDirection())) || enCaja(nivel, e.getEntity().blockPosition()))
				e.setCanceled(true);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void alColocar(BlockEvent.EntityPlaceEvent e) {
		if (e.getLevel() instanceof Level nivel && enCaja(nivel, e.getPos()))
			e.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void alFluir(BlockEvent.FluidPlaceBlockEvent e) {
		if (e.getLevel() instanceof Level nivel && enCaja(nivel, e.getPos()))
			e.setCanceled(true);
	}

	@SubscribeEvent
	public static void alExplotar(ExplosionEvent.Detonate e) {
		Level nivel = e.getLevel();
		e.getAffectedBlocks().removeIf(p -> esBloqueDeBunker(nivel.getBlockState(p)) && enCaja(nivel, p));
	}

	@SubscribeEvent
	public static void alAparecer(EntityJoinLevelEvent e) {
		if (e.loadedFromDisk() || !(e.getEntity() instanceof Mob) || !(e.getLevel() instanceof ServerLevel nivel))
			return;
		if (interiorDe(nivel, e.getEntity().blockPosition()) != null)
			e.setCanceled(true);
	}

	@SubscribeEvent
	public static void alTick(TickEvent.PlayerTickEvent e) {
		if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p) || p.tickCount % 10 != 0)
			return;
		boolean dentro = interiorDe(p.serverLevel(), p.blockPosition()) != null || interiorDe(p.serverLevel(), BlockPos.containing(p.getEyePosition())) != null;
		boolean tiene = p.hasEffect(Efectos.ZONA_SEGURA.holder());
		if (dentro && !tiene)
			p.addEffect(new MobEffectInstance(Efectos.ZONA_SEGURA.holder(), -1, 0, false, false, true));
		else if (!dentro && tiene)
			p.removeEffect(Efectos.ZONA_SEGURA.holder());
	}

	public static void mostrarMarcador(MinecraftServer server, Bunker b) {
		visible = b;
		sincronizar(server);
	}

	private static BunkerPayload marcadores() {
		return new BunkerPayload(visible == null ? new ArrayList<>() : new ArrayList<>(List.of(visible)), false);
	}

	private static BunkerPayload todos(MinecraftServer server) {
		return new BunkerPayload(new ArrayList<>(datos(server).lista), true);
	}

	static void sincronizar(MinecraftServer server) {
		Red.enviarATodos(server, marcadores());
		Red.enviarATodos(server, todos(server));
	}

	@SubscribeEvent
	public static void alEntrar(PlayerEvent.PlayerLoggedInEvent e) {
		if (e.getEntity() instanceof ServerPlayer p && p.getServer() != null) {
			Red.enviar(p, marcadores());
			Red.enviar(p, todos(p.getServer()));
		}
	}

	@SubscribeEvent
	public static void alApagar(ServerLifecycle.ServerStoppedEvent e) {
		PENDIENTES.clear();
		OBRAS.clear();
		visible = null;
		interior = null;
		ocupado = null;
	}

	private static Datos datos(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Datos.FACTORY, "ruleta_bunkers");
	}

	/** Lista de búnkeres del mundo (data/ruleta_bunkers.dat). */
	static final class Datos extends SavedData {
		static final SavedData.Factory<Datos> FACTORY = new SavedData.Factory<>(Datos::new, Datos::cargar, null);
		final List<Bunker> lista = new ArrayList<>();

		Datos() {
		}

		static Datos cargar(CompoundTag tag, HolderLookup.Provider registros) {
			Datos d = new Datos();
			ListTag l = tag.getList("bunkers", 10);
			for (int k = 0; k < l.size(); k++) {
				CompoundTag c = l.getCompound(k);
				if (c.contains("ex"))
					d.lista.add(new Bunker(c.getString("nombre"), c.getString("dim"), c.getInt("ex"), c.getInt("ey"), c.getInt("ez")));
				else
					d.lista.add(new Bunker(c.getString("nombre"), c.getString("dim"), c.getInt("x") - 48, c.getInt("y") - SY - 1, c.getInt("z") - 49));
			}
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			ListTag l = new ListTag();
			for (Bunker b : this.lista) {
				CompoundTag c = new CompoundTag();
				c.putString("nombre", b.nombre());
				c.putString("dim", b.dimension());
				c.putInt("ex", b.ex());
				c.putInt("ey", b.ey());
				c.putInt("ez", b.ez());
				l.add(c);
			}
			tag.put("bunkers", l);
			return tag;
		}
	}
}
