package net.ruleta.compat.client;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

/**
 * Formatos de vértice que 1.20.1 traía en DefaultVertexFormat y 1.21.1 ya no. POSITION_COLOR_TEX lo usan los shaders
 * propios del mod (phora, supernova, supernova_add, supernova_mul): mismos elementos y mismo orden que en 1.20.1.
 */
public final class Formatos {
	public static final VertexFormat POSITION_COLOR_TEX = VertexFormat.builder()
			.add("Position", VertexFormatElement.POSITION)
			.add("Color", VertexFormatElement.COLOR)
			.add("UV0", VertexFormatElement.UV0)
			.build();

	private Formatos() {
	}
}
