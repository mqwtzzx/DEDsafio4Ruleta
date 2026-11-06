package net.ruleta.creeperes;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

/** Creeper diminuto con la mecha a 1 tick: explota nada más ver al jugador. */
public class CreeperCespedEntity extends Creeper {
	public CreeperCespedEntity(EntityType<? extends Creeper> tipo, Level nivel) {
		super(tipo, nivel);
	}

	/** En 1.21.1 finalizeSpawn ya no recibe el CompoundTag (siempre era null aquí). */
	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor nivel, DifficultyInstance dificultad, MobSpawnType razon, @Nullable SpawnGroupData datos) {
		SpawnGroupData r = super.finalizeSpawn(nivel, dificultad, razon, datos);
		mechaInstantanea(this);
		return r;
	}

	static void mechaInstantanea(Creeper c) {
		CompoundTag t = new CompoundTag();
		c.addAdditionalSaveData(t);
		t.putShort("Fuse", (short) 1);
		c.readAdditionalSaveData(t);
	}
}
