package net.cloudruleta.compat.client;

import net.minecraft.client.Camera;
import net.minecraft.world.level.material.FogType;
import net.cloudruleta.compat.Event;

/** ViewportEvent de Forge: ángulos de cámara, color de la niebla y distancia de la niebla. */
public abstract class ViewportEvent extends Event {
	private final Camera camera;
	private final double partialTick;

	protected ViewportEvent(Camera camera, double partialTick) {
		this.camera = camera;
		this.partialTick = partialTick;
	}

	public Camera getCamera() {
		return camera;
	}

	public double getPartialTick() {
		return partialTick;
	}

	/** Tras Camera.setup en GameRenderer.renderLevel: guiñada, cabeceo y alabeo de la vista. */
	public static final class ComputeCameraAngles extends ViewportEvent {
		private float yaw;
		private float pitch;
		private float roll;

		public ComputeCameraAngles(Camera camera, double partialTick, float yaw, float pitch, float roll) {
			super(camera, partialTick);
			this.yaw = yaw;
			this.pitch = pitch;
			this.roll = roll;
		}

		public float getYaw() {
			return yaw;
		}

		public void setYaw(float yaw) {
			this.yaw = yaw;
		}

		public float getPitch() {
			return pitch;
		}

		public void setPitch(float pitch) {
			this.pitch = pitch;
		}

		public float getRoll() {
			return roll;
		}

		public void setRoll(float roll) {
			this.roll = roll;
		}
	}

	/** Color de la niebla/cielo (FogRenderer.setupColor), en 0..1. */
	public static final class ComputeFogColor extends ViewportEvent {
		private float red;
		private float green;
		private float blue;

		public ComputeFogColor(Camera camera, double partialTick, float red, float green, float blue) {
			super(camera, partialTick);
			this.red = red;
			this.green = green;
			this.blue = blue;
		}

		public float getRed() {
			return red;
		}

		public void setRed(float red) {
			this.red = red;
		}

		public float getGreen() {
			return green;
		}

		public void setGreen(float green) {
			this.green = green;
		}

		public float getBlue() {
			return blue;
		}

		public void setBlue(float blue) {
			this.blue = blue;
		}
	}

	/** Distancias de la niebla (FogRenderer.setupFog). Cancelado = se usan los valores puestos. */
	public static final class RenderFog extends ViewportEvent {
		private final net.minecraft.client.renderer.FogRenderer.FogMode mode;
		private final FogType type;
		private float farPlaneDistance;
		private float nearPlaneDistance;
		private com.mojang.blaze3d.shaders.FogShape fogShape;

		public RenderFog(net.minecraft.client.renderer.FogRenderer.FogMode mode, FogType type, Camera camera, float partialTick,
						 float nearPlaneDistance, float farPlaneDistance, com.mojang.blaze3d.shaders.FogShape fogShape) {
			super(camera, partialTick);
			this.mode = mode;
			this.type = type;
			this.nearPlaneDistance = nearPlaneDistance;
			this.farPlaneDistance = farPlaneDistance;
			this.fogShape = fogShape;
		}

		@Override
		public boolean isCancelable() {
			return true;
		}

		public net.minecraft.client.renderer.FogRenderer.FogMode getMode() {
			return mode;
		}

		public FogType getType() {
			return type;
		}

		public float getFarPlaneDistance() {
			return farPlaneDistance;
		}

		public void setFarPlaneDistance(float d) {
			this.farPlaneDistance = d;
		}

		public float getNearPlaneDistance() {
			return nearPlaneDistance;
		}

		public void setNearPlaneDistance(float d) {
			this.nearPlaneDistance = d;
		}

		public com.mojang.blaze3d.shaders.FogShape getFogShape() {
			return fogShape;
		}

		public void setFogShape(com.mojang.blaze3d.shaders.FogShape shape) {
			this.fogShape = shape;
		}

		public void scaleFarPlaneDistance(float k) {
			this.farPlaneDistance *= k;
		}

		public void scaleNearPlaneDistance(float k) {
			this.nearPlaneDistance *= k;
		}
	}
}
