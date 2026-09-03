package com.example.booking;

import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class Errors implements ExceptionMapper<Auth.UnauthorizedException> {
    public Response toResponse(Auth.UnauthorizedException e) {
        return Response.status(Response.Status.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Staff\"")
                .entity("Authentication required").type(MediaType.TEXT_PLAIN).build();
    }
}
