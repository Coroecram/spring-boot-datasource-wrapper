package datasourcewrapper;

public class AuthorizationException extends RuntimeException {

  private static final long serialVersionUID = 432672448886023060L;

  public AuthorizationException(final Throwable cause) {
    super(cause);
  }

}
