package net.cloudruleta;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.cloudruleta.compat.SubscribeEvent;
import net.cloudruleta.compat.event.RegisterCommandsEvent;

/** /temporizador <tiempo> | detener — temporizador en pantalla para todos. */
public final class ComandoTemporizador {
	private ComandoTemporizador() {
	}

	@SubscribeEvent
	public static void alRegistrar(RegisterCommandsEvent e) {
		e.getDispatcher().register(Commands.literal("temporizador")
				.requires(s -> s.hasPermission(2))
				.then(Commands.literal("detener").executes(ctx -> {
					if (Mensajes.detenerTemporizador(ctx.getSource().getServer())) {
						ctx.getSource().sendSuccess(() -> Component.literal("Temporizador detenido."), true);
						return 1;
					}
					ctx.getSource().sendFailure(Component.literal("No hay ningún temporizador en marcha."));
					return 0;
				}))
				.then(Commands.argument("tiempo", StringArgumentType.greedyString()).executes(ctx -> {
					String txt = StringArgumentType.getString(ctx, "tiempo");
					int segundos = Tiempo.leer(txt);
					if (segundos <= 0 || segundos > 86400) {
						ctx.getSource().sendFailure(Component.literal("Tiempo no válido: " + txt + ". Usa 59:59, hasta 24:00:00 o solo minutos (5)."));
						return 0;
					}
					Mensajes.temporizadorComando(ctx.getSource().getServer(), segundos);
					ctx.getSource().sendSuccess(() -> Component.literal("Temporizador puesto: " + txt + "."), true);
					return 1;
				})));
	}
}
