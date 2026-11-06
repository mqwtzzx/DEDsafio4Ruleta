package net.ruleta;

public final class RuletaTiming {
	public static final float SPIN_START = 0.75F;
	public static final float STOP_TIME = 9.45F;
	public static final float FLASH_TIME = 10.12F;
	public static final float REVEAL_START = 10.12F;
	public static final float FILL_START = 10.86F;
	public static final float FILL_STEP = 0.058F;
	public static final float FILL_PAIR = 0.19F;
	public static final float FILL_DURATION = 0.035F;
	public static final float OUTRO_START = 13.0F;
	public static final float TOTAL = 13.93F;

	public static int revealTicks() {
		return Math.round(REVEAL_START * 20.0F);
	}

	private RuletaTiming() {
	}
}
