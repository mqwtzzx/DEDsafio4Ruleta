package net.ruleta.mixin.client;

import net.minecraft.client.Camera;
import net.ruleta.compat.client.CamaraAngulos;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Añade a la cámara un setRotation con alabeo (roll), igual que el de NeoForge 1.21.1: el de vanilla
 * (Camera.setRotation(yRot, xRot)) hace lo mismo con alabeo 0.
 */
@Mixin(Camera.class)
public abstract class CameraMixin implements CamaraAngulos {
	@Shadow
	private float xRot;
	@Shadow
	private float yRot;
	@Shadow
	@Final
	private Quaternionf rotation;
	@Shadow
	@Final
	private Vector3f forwards;
	@Shadow
	@Final
	private Vector3f up;
	@Shadow
	@Final
	private Vector3f left;
	@Shadow
	@Final
	private static Vector3f FORWARDS;
	@Shadow
	@Final
	private static Vector3f UP;
	@Shadow
	@Final
	private static Vector3f LEFT;

	@Override
	public void ruleta$angulos(float yaw, float pitch, float roll) {
		this.xRot = pitch;
		this.yRot = yaw;
		this.rotation.rotationYXZ((float) Math.PI - yaw * ((float) Math.PI / 180.0F), -pitch * ((float) Math.PI / 180.0F),
				-roll * ((float) Math.PI / 180.0F));
		FORWARDS.rotate(this.rotation, this.forwards);
		UP.rotate(this.rotation, this.up);
		LEFT.rotate(this.rotation, this.left);
	}
}
