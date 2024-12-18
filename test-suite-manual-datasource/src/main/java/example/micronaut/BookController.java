package example.micronaut;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

import java.util.ArrayList;
import java.util.List;

@Controller("/books")
class BookController {
    private final MicronautBookRepository micronautBookRepository;
    private final GrailsBookRepository grailsBookRepository;

    BookController(MicronautBookRepository micronautBookRepository,
                   GrailsBookRepository grailsBookRepository) {
        this.micronautBookRepository = micronautBookRepository;
        this.grailsBookRepository = grailsBookRepository;
    }

    @Get
    List<Book> all() {
        List<Book> all = new ArrayList<>();
        all.addAll(micronaut());
        all.addAll(grails());
        return all;
    }

    @Get("/micronaut")
    List<Book> micronaut() {
        return micronautBookRepository.findAll();
    }

    @Get("/grails")
    List<Book> grails() {
        return grailsBookRepository.findAll();
    }
}
