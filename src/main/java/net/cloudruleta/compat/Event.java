package net.cloudruleta.compat;

/** Base de los eventos del puente Forge → Fabric. Solo los eventos cancelables usan {@link #setCanceled}. */
public abstract class Event {
	/** Resultado de tres estados de Forge (Event.Result). */
	public enum Result {
		DENY, DEFAULT, ALLOW
	}

	private boolean canceled;
	private Result result = Result.DEFAULT;

	public boolean isCancelable() {
		return false;
	}

	public boolean isCanceled() {
		return canceled;
	}

	public void setCanceled(boolean cancel) {
		if (cancel && !isCancelable())
			throw new UnsupportedOperationException("El evento " + getClass().getSimpleName() + " no se puede cancelar");
		this.canceled = cancel;
	}

	public Result getResult() {
		return result;
	}

	public void setResult(Result value) {
		this.result = value;
	}
}
