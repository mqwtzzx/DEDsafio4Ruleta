package net.ruleta;

public enum RuletaColor {
	ROJO("rojo", 16263228),
	ROSA("rosa", 16070886),
	MORADO("morado", 10890726),
	AZUL("azul", 3352802),
	CIAN("cian", 976622),
	VERDE("verde", 3992378),
	AMARILLO("amarillo", 16445214),
	NARANJA("naranja", 16623398);

	public static final RuletaColor[] VALUES = values();
	public final String id;
	public final int rgb;

	RuletaColor(String id, int rgb) {
		this.id = id;
		this.rgb = rgb;
	}

	public static RuletaColor byIndex(int indice) {
		return VALUES[Math.floorMod(indice, VALUES.length)];
	}
}
