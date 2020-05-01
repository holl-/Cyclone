package audio;

public class UnsupportedMediaFormatException extends Exception {
	private static final long serialVersionUID = 6049017518614148592L;

	public UnsupportedMediaFormatException() {}

	public UnsupportedMediaFormatException(String arg0) {
		super(arg0);
	}

	public UnsupportedMediaFormatException(Throwable arg0) {
		super(arg0);
	}

	public UnsupportedMediaFormatException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

}
