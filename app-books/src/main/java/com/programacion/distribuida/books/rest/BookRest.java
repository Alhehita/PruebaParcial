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
    public Response findByIsbn(@PathParam("isbn") String isbn) {
        var stork = Stork.getInstance();

        //--listar servicios
        Map<String, Service> services = stork.getServices();

        services.entrySet()
                .stream()
                .forEach(it -> {
                    System.out.println(it.getKey());

                    Multi<ServiceInstance> instances = it.getValue()
                            .getInstances()
                            .onItem()
                            .transformToMulti(items -> Multi.createFrom().iterable(items));

                    instances.subscribe()
                            .with(item->{
                                System.out.println("  " + item.getHost() + ":" + item.getPort());
                            });
                });

        //--seleccionar una instancia
        Service service = stork.getService("authors-api");
        Uni<ServiceInstance> instance = service.selectInstance();
        instance
                .subscribe()
                .with(inst -> {
                    System.out.println("**Instancia seleccionada: " + inst.getHost() + ":" + inst.getPort());
                });


        BookDto ret = new BookDto();

        //1. buscar el libro
        var obj = booksRepository.findByIdOptional(isbn);
        if (obj.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .build();
        }
        mapper.map(obj.get(), ret);

        var authors = client.findByBook(isbn)
                .stream()
                .map(AuthorDto::getName)
                .toList();

        ret.setAuthors(authors);

        return Response.ok(ret)
                .build();
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
    public void insert(Book book) {
        booksRepository.persist(book);
    }

    @PUT
    @Path("/{isbn}")
    public void update(@PathParam("isbn") String isbn, Book book) {
        booksRepository.update(isbn, book);
    }
}

