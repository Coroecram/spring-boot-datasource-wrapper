package datasourcewrapper;

public class ResourceNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1264654081166637015L;

  public ResourceNotFoundException(final Throwable cause) {
    super(cause);
  }

  public ResourceNotFoundException(final String message) {
    super(message);
  }
}
