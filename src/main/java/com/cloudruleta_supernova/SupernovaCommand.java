package com.cloudruleta_supernova;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.cloudruleta.Tiempo;
import net.cloudruleta.compat.SubscribeEvent;
import net.cloudruleta.compat.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /cloudruleta_supernova romper on|off · explotar · estrella [pos] [tamaño N] [tiempo T] · tamaño N [tiempo T] · tiempo T. */
public class SupernovaCommand {
	private interface Ejecutor {
		int run(CommandContext<CommandSourceStack> c, Float tam, String tiempo);
	}

	private static final class Valores {
		Float tam;
		Integer ticks;
		String error;

		private Valores() {
		}
	}

	@SubscribeEvent
	public static void onRegister(RegisterCommandsEvent e) {
		LiteralArgumentBuilder<CommandSourceStack> raiz = Commands.literal("cloudruleta_supernova").requires(src -> src.hasPermission(2));
		raiz.then(Commands.literal("romper")
				.then(Commands.literal("on").executes(ctx -> {
					SupernovaRomper.activo = true;
					ctx.getSource().sendSuccess(() -> Component.literal("Romper: ON. Al explotar, la cloudruleta_supernova romperá todo su círculo hasta el vacío."), true);
					return 1;
				}))
				.then(Commands.literal("off").executes(ctx -> {
					SupernovaRomper.activo = false;
					ctx.getSource().sendSuccess(() -> Component.literal("Romper: OFF. La cloudruleta_supernova ya no rompe el terreno."), true);
					return 1;
				})));
		raiz.then(Commands.literal("explotar").executes(ctx -> {
			ServerLevel level = ctx.getSource().getLevel();
			List<? extends SupernovaEntity> idle = level.getEntities(SupernovaMod.SUPERNOVA.get(), n -> !n.isDetonating());
			if (idle.isEmpty()) {
				ctx.getSource().sendFailure(Component.literal("No hay ninguna cloudruleta_supernova sin detonar en esta dimensión."));
				return 0;
			}
			int count = 0;
			for (SupernovaEntity n : idle)
				if (n.detonate())
					count++;
			int total = count;
			ctx.getSource().sendSuccess(() -> Component.literal("Cuenta atrás iniciada (" + total + " cloudruleta_supernova" + (total == 1 ? "" : "s") + ")."), true);
			return total;
		}));
		Ejecutor sinPos = (c, tam, tiempo) -> crear(c, c.getSource().getPosition(), tam, tiempo);
		Ejecutor conPos = (c, tam, tiempo) -> crear(c, Vec3Argument.getVec3(c, "pos"), tam, tiempo);
		LiteralArgumentBuilder<CommandSourceStack> estrella = Commands.literal("estrella").executes(c -> sinPos.run(c, null, null));
		for (LiteralArgumentBuilder<CommandSourceStack> n : opciones(sinPos))
			estrella.then(n);
		RequiredArgumentBuilder<CommandSourceStack, Coordinates> pos = Commands.argument("pos", Vec3Argument.vec3()).executes(c -> conPos.run(c, null, null));
		for (LiteralArgumentBuilder<CommandSourceStack> n : opciones(conPos))
			pos.then(n);
		estrella.then(pos);
		raiz.then(estrella);
		for (LiteralArgumentBuilder<CommandSourceStack> n : opciones(SupernovaCommand::ajustar))
			raiz.then(n);
		e.getDispatcher().register(raiz);
	}

	private static List<LiteralArgumentBuilder<CommandSourceStack>> opciones(Ejecutor ej) {
		List<LiteralArgumentBuilder<CommandSourceStack>> out = new ArrayList<>();
		for (String lit : new String[]{"tamaño", "tamano"})
			out.add(Commands.literal(lit).then(Commands.argument("tam", FloatArgumentType.floatArg())
					.executes(c -> ej.run(c, FloatArgumentType.getFloat(c, "tam"), null))
					.then(Commands.literal("tiempo").then(Commands.argument("t", StringArgumentType.greedyString())
							.executes(c -> ej.run(c, FloatArgumentType.getFloat(c, "tam"), StringArgumentType.getString(c, "t")))))));
		out.add(Commands.literal("tiempo").then(Commands.argument("t", StringArgumentType.greedyString())
				.executes(c -> ej.run(c, null, StringArgumentType.getString(c, "t")))));
		return out;
	}

	private static Valores leer(Float tamArg, String tiempoTxt) {
		Valores v = new Valores();
		v.tam = tamArg;
		if (tiempoTxt != null) {
			String[] p = tiempoTxt.trim().split("\\s+");
			int seg = Tiempo.leer(p[0]);
			if (seg < 0 || seg > SupernovaAjustes.TIEMPO_MAX_SEGUNDOS) {
				v.error = "Tiempo no válido: " + p[0] + ". Usa 1 (minuto), 30s, 1:30 (mm:ss) o 1:00:00 (hh:mm:ss), hasta 24 h.";
				return v;
			}
			v.ticks = seg * 20;
			if (p.length == 3 && (p[1].equalsIgnoreCase("tamaño") || p[1].equalsIgnoreCase("tamano"))) {
				if (tamArg != null) {
					v.error = "El tamaño está repetido.";
					return v;
				}
				try {
					v.tam = Float.parseFloat(p[2].replace(',', '.'));
				} catch (NumberFormatException ex) {
					v.error = "Tamaño no válido: " + p[2] + ".";
					return v;
				}
			} else if (p.length != 1) {
				v.error = "No entiendo \"" + tiempoTxt.trim() + "\". Ejemplo: tamaño 15 tiempo 1:30";
				return v;
			}
		}
		if (v.tam != null && (v.tam.isNaN() || v.tam < SupernovaAjustes.TAMANO_MIN || v.tam > SupernovaAjustes.TAMANO_MAX))
			v.error = "El tamaño tiene que estar entre " + fmtNum(SupernovaAjustes.TAMANO_MIN) + " y " + fmtNum(SupernovaAjustes.TAMANO_MAX) + " (el máximo es "
					+ fmtNum(SupernovaAjustes.TAMANO_MAX) + ").";
		return v;
	}

	private static String fmtNum(float f) {
		return (double) f == Math.rint(f) ? Long.toString((long) f) : String.format(Locale.ROOT, "%.1f", f);
	}

	private static String fmtTiempo(int segundos) {
		if (segundos <= 0)
			return "nada extra";
		int h = segundos / 3600;
		int m = segundos % 3600 / 60;
		int s = segundos % 60;
		return h > 0 ? String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s) : String.format(Locale.ROOT, "%d:%02d", m, s);
	}

	private static int crear(CommandContext<CommandSourceStack> ctx, Vec3 pos, Float tam, String tiempoTxt) {
		Valores v = leer(tam, tiempoTxt);
		if (v.error != null) {
			ctx.getSource().sendFailure(Component.literal(v.error));
			return 0;
		}
		ServerLevel level = ctx.getSource().getLevel();
		SupernovaEntity n = SupernovaMod.SUPERNOVA.get().create(level);
		if (n == null) {
			ctx.getSource().sendFailure(Component.literal("No se pudo crear la estrella."));
			return 0;
		}
		n.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
		if (v.tam != null)
			n.setMaxRadius(v.tam);
		if (v.ticks != null)
			n.setHoldTicks(v.ticks);
		level.addFreshEntity(n);
		float radio = n.getMaxRadius();
		int seg = n.getHoldTicks() / 20;
		ctx.getSource().sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
				"Estrella creada en %.1f %.1f %.1f (tamaño %s, tiempo tras explotar: %s). /cloudruleta_supernova explotar para detonarla.", pos.x, pos.y, pos.z, fmtNum(radio),
				fmtTiempo(seg))), true);
		return 1;
	}

	private static int ajustar(CommandContext<CommandSourceStack> ctx, Float tam, String tiempoTxt) {
		Valores v = leer(tam, tiempoTxt);
		if (v.error != null) {
			ctx.getSource().sendFailure(Component.literal(v.error));
			return 0;
		}
		if (v.tam != null)
			SupernovaAjustes.tamano = v.tam;
		if (v.ticks != null)
			SupernovaAjustes.tiempoTicks = v.ticks;
		ServerLevel level = ctx.getSource().getLevel();
		List<? extends SupernovaEntity> idle = level.getEntities(SupernovaMod.SUPERNOVA.get(), n -> !n.isDetonating());
		for (SupernovaEntity n : idle) {
			if (v.tam != null)
				n.setMaxRadius(v.tam);
			if (v.ticks != null)
				n.setHoldTicks(v.ticks);
			SupernovaNet.broadcast(n, false);
		}
		int cuantas = idle.size();
		StringBuilder sb = new StringBuilder("Supernova:");
		if (v.tam != null)
			sb.append(" tamaño ").append(fmtNum(v.tam));
		if (v.ticks != null)
			sb.append(" tiempo tras explotar ").append(fmtTiempo(v.ticks / 20));
		sb.append(cuantas == 0 ? " (para las estrellas nuevas)." : " (estrellas nuevas y " + cuantas + " en reposo).");
		ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), true);
		return 1;
	}
}
