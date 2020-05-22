package comp4111project.Model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategy.UpperCamelCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Book {
//    int idBook;
    private String title;
    private String author;
    private String publisher;
    private int year;

    public Book() {
    	super();
    }
    public Book(String title, String author, String publisher, int year) {
//        this.idBook = idBook;
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.year = year;
    }

//    public int getIdBook() {
//        return idBook;
//    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getPublisher() {
        return publisher;
    }

    public int getYear() {
        return year;
    }
}
