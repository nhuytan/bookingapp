package com.example.booking;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
public class Api {

    @GET @Path("/staff")
    public Response staff() throws Exception {
        List<Map<String,Object>> out = new ArrayList<>();
        try (Connection c=Database.pool().getConnection();
             PreparedStatement p=c.prepareStatement("SELECT id,display_name FROM staff WHERE active=true ORDER BY id");
             ResultSet r=p.executeQuery()) {
            while(r.next()) out.add(Map.of("id",r.getLong(1),"displayName",r.getString(2)));
        }
        return Response.ok(out).build();
    }

    @GET @Path("/staff/{id}/slots")
    public Response publicSlots(@PathParam("id") long staffId,@QueryParam("date") String date) throws Exception {
        LocalDate d = LocalDate.parse(date);
        return Response.ok(readSlots(staffId,d,"status='open'")).build();
    }

    @POST @Path("/bookings")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response book(BookingRequest req) throws Exception {
        if(req==null || req.slotId()<=0 || blank(req.customerName()) || blank(req.customerPhone()))
            return Response.status(400).entity("slotId, customerName and customerPhone are required").build();

        try(Connection c=Database.pool().getConnection()) {
            c.setAutoCommit(false);
            try(PreparedStatement p=c.prepareStatement(
                    "SELECT id,status FROM schedule_slot WHERE id=? FOR UPDATE")) {
                p.setLong(1,req.slotId());
                try(ResultSet r=p.executeQuery()) {
                    if(!r.next()) { c.rollback(); return Response.status(404).entity("Slot not found").build(); }
                    if(!"open".equals(r.getString("status"))) {
                        c.rollback(); return Response.status(409).entity("Slot is already booked").build();
                    }
                }
            }
            long bookingId;
            try(PreparedStatement p=c.prepareStatement(
                    "UPDATE schedule_slot SET status='booked',customer_name=?,customer_phone=?,booked_at=CURRENT_TIMESTAMP WHERE id=?",
                    Statement.RETURN_GENERATED_KEYS)) {
                p.setString(1,req.customerName().trim());
                p.setString(2,req.customerPhone().trim());
                p.setLong(3,req.slotId());
                p.executeUpdate();
                bookingId=req.slotId();
            }
            c.commit();
            return Response.status(201).entity(Map.of("bookingId",bookingId,"message","Booking confirmed")).build();
        }
    }

    @GET @Path("/staff/me")
    public Response me(@Context HttpHeaders headers) throws Exception {
        Auth.Staff s=Auth.require(headers);
        return Response.ok(s).build();
    }

    @GET @Path("/staff/me/slots")
    public Response mySlots(@Context HttpHeaders headers,@QueryParam("date") String date) throws Exception {
        Auth.Staff s=Auth.require(headers);
        LocalDate d=LocalDate.parse(date);
        return Response.ok(readSlots(s.id(),d,null)).build();
    }

    @POST @Path("/staff/slots")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addSlot(@Context HttpHeaders headers, SlotRequest req) throws Exception {
        Auth.Staff s=Auth.require(headers);
        if(req==null || req.slotDate()==null || blank(req.startTime()) || blank(req.endTime()))
            return Response.status(400).entity("slotDate, startTime and endTime are required").build();
        try(Connection c=Database.pool().getConnection();
            PreparedStatement p=c.prepareStatement(
                "INSERT INTO schedule_slot(staff_id,slot_date,start_time,end_time) VALUES(?,?,?,?) RETURNING id")) {
            p.setLong(1,s.id()); p.setDate(2,java.sql.Date.valueOf(req.slotDate()));
            p.setTime(3,Time.valueOf(req.startTime())); p.setTime(4,Time.valueOf(req.endTime()));
            try(ResultSet r=p.executeQuery()){r.next(); return Response.status(201).entity(Map.of("id",r.getLong(1))).build();}
        } catch(SQLException e) {
            if("23505".equals(e.getSQLState())) return Response.status(409).entity("That time slot already exists").build();
            throw e;
        }
    }

    @DELETE @Path("/staff/slots/{id}")
    public Response removeSlot(@Context HttpHeaders headers,@PathParam("id") long id) throws Exception {
        Auth.Staff s=Auth.require(headers);
        try(Connection c=Database.pool().getConnection();
            PreparedStatement p=c.prepareStatement(
                "DELETE FROM schedule_slot WHERE id=? AND staff_id=? AND status='open'")) {
            p.setLong(1,id); p.setLong(2,s.id());
            if(p.executeUpdate()==0) return Response.status(409).entity("Slot not found, not yours, or already booked").build();
            return Response.noContent().build();
        }
    }

    @POST @Path("/staff/bookings/{id}/cancel")
    public Response cancel(@Context HttpHeaders headers,@PathParam("id") long id) throws Exception {
        Auth.Staff s=Auth.require(headers);
        try(Connection c=Database.pool().getConnection();
            PreparedStatement p=c.prepareStatement(
                "UPDATE schedule_slot SET status='open',customer_name=NULL,customer_phone=NULL,booked_at=NULL " +
                "WHERE id=? AND staff_id=? AND status='booked'")) {
            p.setLong(1,id); p.setLong(2,s.id());
            if(p.executeUpdate()==0) return Response.status(404).entity("Booked slot not found").build();
            return Response.ok(Map.of("message","Booking cancelled; slot is open again")).build();
        }
    }

    private List<Map<String,Object>> readSlots(long staffId,LocalDate date,String extra) throws Exception {
        List<Map<String,Object>> out=new ArrayList<>();
        String sql="SELECT id,start_time,end_time,status,customer_name,customer_phone FROM schedule_slot WHERE staff_id=? AND slot_date=?"+
                (extra==null?"":" AND "+extra)+" ORDER BY start_time";
        try(Connection c=Database.pool().getConnection(); PreparedStatement p=c.prepareStatement(sql)){
            p.setLong(1,staffId);p.setDate(2,java.sql.Date.valueOf(date));
            try(ResultSet r=p.executeQuery()){
                while(r.next()){
                    Map<String,Object> m=new LinkedHashMap<>();
                    m.put("id",r.getLong("id"));m.put("startTime",r.getTime("start_time").toString());
                    m.put("endTime",r.getTime("end_time").toString());m.put("status",r.getString("status"));
                    m.put("customerName",r.getString("customer_name"));m.put("customerPhone",r.getString("customer_phone"));
                    out.add(m);
                }
            }
        }
        return out;
    }
    private static boolean blank(String s){return s==null||s.isBlank();}

    public record BookingRequest(long slotId,String customerName,String customerPhone){}
    public record SlotRequest(LocalDate slotDate,String startTime,String endTime){}
}
