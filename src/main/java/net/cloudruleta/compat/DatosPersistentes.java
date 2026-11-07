package net.cloudruleta.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

/**
 * Sustituto de Entity#getPersistentData() de Forge: un CompoundTag por entidad que se guarda con ella bajo la
 * clave "ForgeData" (la misma que usaba Forge, así los datos de un mundo de la versión Forge se conservan).
 * Lo implementa el mixin EntityDatosMixin.
 */
public interface DatosPersistentes {
	String CLAVE = "ForgeData";

	CompoundTag cloudruleta$datosPersistentes();

	static CompoundTag de(Entity entidad) {
		return ((DatosPersistentes) entidad).cloudruleta$datosPersistentes();
	}
}
