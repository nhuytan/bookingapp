package com.example.booking;

import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ForbiddenMapper implements ExceptionMapper<Auth.ForbiddenException> {
    public Response toResponse(Auth.ForbiddenException e) {
        return Response.status(Response.Status.FORBIDDEN)
                .entity("Admin access required").type(MediaType.TEXT_PLAIN).build();
    }
}
