package net.ruleta.compat.client;

/**
 * Lo implementa la cámara (mixin.client.CameraMixin): fija guiñada, inclinación y alabeo como hacía Forge tras
 * ViewportEvent.ComputeCameraAngles.
 */
public interface CamaraAngulos {
	void ruleta$angulos(float yaw, float pitch, float roll);
}
