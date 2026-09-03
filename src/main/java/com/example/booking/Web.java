package com.example.booking;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Path("/")
public class Web {
    @GET @Produces(MediaType.TEXT_HTML)
    public Response index(){ return page("/public/index.html"); }

    @GET @Path("staff")
    @Produces(MediaType.TEXT_HTML)
    public Response staff(){ return page("/public/staff.html"); }

    @GET @Path("admin")
    @Produces(MediaType.TEXT_HTML)
    public Response admin(){ return page("/public/admin.html"); }

    private Response page(String path){
        try(InputStream in=Web.class.getResourceAsStream(path)){
            if(in==null)return Response.status(404).build();
            return Response.ok(new String(in.readAllBytes(), StandardCharsets.UTF_8)).build();
        }catch(Exception e){return Response.serverError().entity("Page error").build();}
    }
}
