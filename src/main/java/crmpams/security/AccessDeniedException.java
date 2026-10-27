package crmpams.security;

/** Thrown by a service when the logged-in user's role may not perform the action. */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }
}
