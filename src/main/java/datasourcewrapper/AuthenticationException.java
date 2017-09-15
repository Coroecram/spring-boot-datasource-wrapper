package datasourcewrapper;

public class AuthenticationException extends RuntimeException {

  private static final long serialVersionUID = -7962108894437178198L;

  public AuthenticationException(final Throwable cause) {
    super(cause);
  }

  @Override
  public String getMessage() {
    return "The username or password is incorrect";
  }
}
