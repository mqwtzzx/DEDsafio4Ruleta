package net.cloudruleta.compat.client;

/**
 * Lo implementa la cámara (mixin.client.CameraMixin): fija guiñada, inclinación y alabeo como hacía Forge tras
 * ViewportEvent.ComputeCameraAngles.
 */
public interface CamaraAngulos {
	void cloudruleta$angulos(float yaw, float pitch, float roll);
}
