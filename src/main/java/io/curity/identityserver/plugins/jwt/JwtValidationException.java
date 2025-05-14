package io.curity.identityserver.plugins.jwt;

public final class JwtValidationException extends RuntimeException
{
    public JwtValidationException(String message)
    {
        super(message);
    }

    public JwtValidationException(Exception e)
    {
       super(e);
    }
}
