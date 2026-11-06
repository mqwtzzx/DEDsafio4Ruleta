package net.ruleta.compat;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionResult;
import net.ruleta.compat.event.BlockEvent;
import net.ruleta.compat.event.EntityEvents;
import net.ruleta.compat.event.PlayerEvent;
import net.ruleta.compat.event.PlayerInteractEvent;
import net.ruleta.compat.event.RegisterCommandsEvent;
import net.ruleta.compat.event.ServerChatEvent;
import net.ruleta.compat.event.ServerLifecycle;
import net.ruleta.compat.event.TickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Puente entre los callbacks de Fabric API y el bus de eventos estilo Forge del mod. Lo que Fabric API no ofrece
 * (muerte, daño, explosiones, aparición de criaturas, perlas, cubos…) lo publican los mixins de net.ruleta.mixin
 * en el mismo punto donde Forge lanzaba el evento.
 */
public final class Puente {
	@Nullable
	private static volatile MinecraftServer servidor;

	private Puente() {
	}

	/** Sustituto de ServerLifecycleHooks.getCurrentServer(). */
	@Nullable
	public static MinecraftServer servidor() {
		return servidor;
	}

	public static void registrarComun() {
		ServerLifecycleEvents.SERVER_STARTING.register(s -> servidor = s);
		ServerLifecycleEvents.SERVER_STOPPING.register(s -> Bus.post(new ServerLifecycle.ServerStoppingEvent(s)));
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> {
			Bus.post(new ServerLifecycle.ServerStoppedEvent(s));
			servidor = null;
		});

		// TickEvent.ServerTickEvent: START al empezar MinecraftServer.tickServer, END al acabar (igual que Forge)
		ServerTickEvents.START_SERVER_TICK.register(s -> Bus.post(new TickEvent.ServerTickEvent(TickEvent.Phase.START, s)));
		ServerTickEvents.END_SERVER_TICK.register(s -> Bus.post(new TickEvent.ServerTickEvent(TickEvent.Phase.END, s)));

		CommandRegistrationCallback.EVENT.register((dispatcher, contexto, entorno) -> Bus.post(new RegisterCommandsEvent(dispatcher, contexto)));

		// PlayerLoggedIn al final de PlayerList.placeNewPlayer; PlayerLoggedOut al principio de PlayerList.remove
		ServerPlayerEvents.JOIN.register(p -> Bus.post(new PlayerEvent.PlayerLoggedInEvent(p)));
		ServerPlayerEvents.LEAVE.register(p -> Bus.post(new PlayerEvent.PlayerLoggedOutEvent(p)));
		// Clone en ServerPlayer.restoreFrom; Respawn al terminar PlayerList.respawn («alive» = vuelve del End)
		ServerPlayerEvents.COPY_FROM.register((viejo, nuevo, vivo) -> Bus.post(new PlayerEvent.Clone(nuevo, viejo, !vivo)));
		ServerPlayerEvents.AFTER_RESPAWN.register((viejo, nuevo, vivo) -> Bus.post(new PlayerEvent.PlayerRespawnEvent(nuevo, vivo)));
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((p, desde, hasta) -> Bus.post(new PlayerEvent.PlayerChangedDimensionEvent(p)));

		ServerEntityEvents.ENTITY_UNLOAD.register((entidad, nivel) -> Bus.post(new EntityEvents.EntityLeaveLevelEvent(entidad, nivel)));

		// BlockEvent.BreakEvent: antes de romper un bloque un jugador (cancelable)
		PlayerBlockBreakEvents.BEFORE.register((nivel, jugador, pos, estado, be) -> !Bus.post(new BlockEvent.BreakEvent(nivel, pos, estado, jugador)));

		// PlayerInteractEvent.LeftClickBlock: al empezar a picar un bloque (cliente y servidor)
		AttackBlockCallback.EVENT.register((jugador, nivel, mano, pos, cara) -> {
			PlayerInteractEvent.LeftClickBlock e = new PlayerInteractEvent.LeftClickBlock(jugador, pos, cara);
			return Bus.post(e) ? InteractionResult.FAIL : InteractionResult.PASS;
		});

		// EntityInteractSpecific (con punto de impacto) y EntityInteract (sin él), en cliente y servidor
		UseEntityCallback.EVENT.register((jugador, nivel, mano, entidad, impacto) -> {
			PlayerInteractEvent e = impacto != null ? new PlayerInteractEvent.EntityInteractSpecific(jugador, mano, entidad)
					: new PlayerInteractEvent.EntityInteract(jugador, mano, entidad);
			if (Bus.post(e)) {
				InteractionResult r = e.getCancellationResult();
				return r == null || r == InteractionResult.PASS ? InteractionResult.FAIL : r;
			}
			return InteractionResult.PASS;
		});

		// ServerChatEvent: antes de difundir un mensaje de chat (cancelable)
		ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((mensaje, jugador, parametros) ->
				!Bus.post(new ServerChatEvent(jugador, mensaje.signedContent())));
	}
}
