package net.cloudruleta.compat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.cloudruleta.compat.Event;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * RenderLevelStageEvent de Forge 1.20.1. Lo publica el mixin de LevelRenderer.renderLevel en los mismos momentos.
 * Como en Forge 1.20.1, getPoseStack() trae ya la rotación de la cámara, y mientras se reparte el evento la
 * matriz de modelo-vista de RenderSystem es la identidad (en 1.21.1 a veces lleva la vista).
 */
public final class RenderLevelStageEvent extends Event {
	public enum Stage {
		AFTER_SKY, AFTER_TRANSLUCENT_BLOCKS, AFTER_PARTICLES, AFTER_LEVEL
	}

	private final Stage stage;
	private final LevelRenderer levelRenderer;
	private final PoseStack poseStack;
	private final Matrix4f projectionMatrix;
	private final int renderTick;
	private final float partialTick;
	private final Camera camera;
	@Nullable
	private final Frustum frustum;

	public RenderLevelStageEvent(Stage stage, LevelRenderer levelRenderer, PoseStack poseStack, Matrix4f projectionMatrix, int renderTick, float partialTick,
								 Camera camera, @Nullable Frustum frustum) {
		this.stage = stage;
		this.levelRenderer = levelRenderer;
		this.poseStack = poseStack;
		this.projectionMatrix = projectionMatrix;
		this.renderTick = renderTick;
		this.partialTick = partialTick;
		this.camera = camera;
		this.frustum = frustum;
	}

	public Stage getStage() {
		return stage;
	}

	public LevelRenderer getLevelRenderer() {
		return levelRenderer;
	}

	public PoseStack getPoseStack() {
		return poseStack;
	}

	public Matrix4f getProjectionMatrix() {
		return projectionMatrix;
	}

	public int getRenderTick() {
		return renderTick;
	}

	public float getPartialTick() {
		return partialTick;
	}

	public Camera getCamera() {
		return camera;
	}

	@Nullable
	public Frustum getFrustum() {
		return frustum;
	}
}
