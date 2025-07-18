package com.programacion.distribuida.customers.rest;

import com.programacion.distribuida.customers.clients.BookRestClient;
import com.programacion.distribuida.customers.db.PurchaseOrder;
import com.programacion.distribuida.customers.dto.PurchaseOrderDto;
import com.programacion.distribuida.customers.repo.PurchaseOrderRepository;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.modelmapper.ModelMapper;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/orders")
@Transactional
public class PurchaseOrderRest {

    @Inject
    ModelMapper mapper;

    @Inject
    PurchaseOrderRepository repository;

    @Inject
    @RestClient
    BookRestClient bookRestClient;

    private PurchaseOrderDto map(PurchaseOrder source) {
        var dto = mapper.map(source, PurchaseOrderDto.class);

        dto.getLineItems()
                .stream()
                .forEach(item -> {
                    var book = bookRestClient.findByBook(item.getIsbn());

                    item.setIsbn(book.getIsbn());
                    item.setTitle(book.getTitle());
                    item.setPrice(book.getPrice());

                    item.setAuthors(book.getAuthors());
                });

        return dto;
    }

    @GET
    @Path("/customer/{customerId}")
    public List<PurchaseOrderDto> orders(@PathParam("customerId") Integer customerId) {

        return repository.findByCustomerId(customerId)
                .stream()
                .map(this::map)
                .toList();
    }

    @GET
    @Path("/{orderId}")
    public Response orderDetail(@PathParam("orderId") Integer orderId) {

        return repository.findByIdOptional(orderId)
                .map(this::map)
                .map(Response::ok)
                .orElse(Response.status(Response.Status.NOT_FOUND))
                .build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createOrder(PurchaseOrderDto orderDto) {
        PurchaseOrder entity = mapper.map(orderDto, PurchaseOrder.class);
        repository.persist(entity);
        return Response.status(Response.Status.CREATED).entity(map(entity)).build();
    }

    @PUT
    @Path("/{orderId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateOrder(@PathParam("orderId") Integer orderId, PurchaseOrderDto orderDto) {
        return repository.findByIdOptional(orderId)
                .map(existing -> {
                    mapper.map(orderDto, existing);
                    return Response.ok(map(existing)).build();
                })
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @DELETE
    @Path("/{orderId}")
    public Response deleteOrder(@PathParam("orderId") Integer orderId) {
        return repository.findByIdOptional(orderId)
                .map(order -> {
                    repository.delete(order);
                    return Response.noContent().build();
                })
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<PurchaseOrderDto> getAllOrders() {
        return repository.listAll()
                .stream()
                .map(this::map)
                .toList();
    }
}
