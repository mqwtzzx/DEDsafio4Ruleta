package net.cloudruleta.creeperrojo;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;

public class CreeperRojoEntity extends Creeper {
	public CreeperRojoEntity(EntityType<? extends Creeper> tipo, Level nivel) {
		super(tipo, nivel);
	}
}
