package io.curity.identityserver.plugins.jwt;

/**
 * Exception thrown when a JWT validation fails.
 */
public final class JwtValidationException extends RuntimeException
{
    /**
     * Constructs a new JwtValidationException with the cause of the exception.
     *
     * @param e the cause of the exception
     */
    public JwtValidationException(Exception e)
    {
       super(e);
    }
}
