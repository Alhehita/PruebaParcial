package com.programacion.distribuida.books.rest;

import com.programacion.distribuida.books.clients.AuthorRestClient;
import com.programacion.distribuida.books.db.Book;
import com.programacion.distribuida.books.dtos.AuthorDto;
import com.programacion.distribuida.books.dtos.BookDto;
import com.programacion.distribuida.books.repo.BooksRepository;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.stork.Stork;
import io.smallrye.stork.api.Service;
import io.smallrye.stork.api.ServiceInstance;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.Map;

@Path("/books")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class BookRest {

    @Inject
    @ConfigProperty(name = "quarkus.http.port")
    Integer httpPort;

    @Inject
    BooksRepository booksRepository;

    @Inject
    ModelMapper mapper;

    @Inject
    @RestClient
    private AuthorRestClient client;

    @GET
    @Path("/{isbn}")
    public Uni<Response> findByIsbn(@PathParam("isbn") String isbn) {
        // Buscar el libro de forma reactiva
        return Uni.createFrom().item(() -> booksRepository.findByIdOptional(isbn))
                .onItem().transformToUni(optional -> {
                    if (optional.isEmpty()) {
                        return Uni.createFrom().item(Response.status(Response.Status.NOT_FOUND).build());
                    }

                    BookDto ret = new BookDto();
                    mapper.map(optional.get(), ret);

                    // Llamar al cliente para obtener los autores
                    return Uni.createFrom().item(() -> client.findByBook(isbn))
                            .onItem().transform(authors -> {
                                List<String> names = authors.stream()
                                        .map(AuthorDto::getName)
                                        .toList();

                                ret.setAuthors(names);
                                return Response.ok(ret).build();
                            });
                });
    }


    @GET
    public List<BookDto> findAll() {

        return booksRepository.streamAll()
                .map(book -> {
                    var dto = new BookDto();
                    mapper.map(book, dto);
                    return dto;
                })
                .map(book -> {
                    var authors = client.findByBook(book.getIsbn())
                            .stream()
                            .map(AuthorDto::getName)
                            .toList();
                    book.setAuthors(authors);
                    return book;
                })
                .toList();
    }

   @POST
    public Response insert(Book book) {
        booksRepository.persist(book);
        return Response.status(Response.Status.CREATED).entity(book).build();
    }

    // Update an existing book
    @PUT
    @Path("/{isbn}")
    public Response update(@PathParam("isbn") String isbn, Book book) {
        var existing = booksRepository.findByIdOptional(isbn);
        if (existing.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        booksRepository.update(isbn, book);
        return Response.ok(book).build();
    }

    // Delete a book by ISBN
    @DELETE
    @Path("/{isbn}")
    public Response delete(@PathParam("isbn") String isbn) {
        var existing = booksRepository.findByIdOptional(isbn);
        if (existing.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        booksRepository.delete(existing.get());
        return Response.noContent().build();
    }
}

