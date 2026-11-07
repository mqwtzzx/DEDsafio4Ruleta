package net.cloudruleta.nuclear.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class TotemItem extends Item {
	private final Kind kind;

	public TotemItem(Item.Properties properties, Kind kind) {
		super(properties);
		this.kind = kind;
	}

	public Kind getKind() {
		return this.kind;
	}

	@Override
	public Component getName(ItemStack stack) {
		return Component.literal(this.kind.displayName).withStyle(ChatFormatting.YELLOW);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext contexto, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.literal(this.kind.description).withStyle(ChatFormatting.GRAY));
	}

	public enum Kind {
		GLEBANOIDE(true, "Tótem Glebanoide", "Te salva de la muerte si lo llevas en una mano."),
		NUTRITOTEM(true, "Nutritótem", "Te salva de la muerte si lo llevas en una mano."),
		EON(true, "Tótem de Eón", "Te salva desde la mano o desde el inventario (en el inventario, solo si no tienes ningún otro tótem)."),
		YUNQUE(true, "Tótem Yunque", "Te salva de la muerte y repara toda tu armadura."),
		GOLEM(true, "Tótem Golem", "Te salva de la muerte y, mientras lo lleves en una mano, te da 2 corazones extra.");

		public final boolean revives;
		public final String displayName;
		public final String description;

		Kind(boolean revives, String displayName, String description) {
			this.revives = revives;
			this.displayName = displayName;
			this.description = description;
		}
	}
}
