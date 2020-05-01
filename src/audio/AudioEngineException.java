package audio;

public class AudioEngineException extends Exception {
	private static final long serialVersionUID = 7865578110903735245L;

	public AudioEngineException() {
	}

	public AudioEngineException(String message) {
		super(message);
	}

	public AudioEngineException(Throwable cause) {
		super(cause);
	}

	public AudioEngineException(String message, Throwable cause) {
		super(message, cause);
	}

}
