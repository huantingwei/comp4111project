package comp4111project.Model;

public class Book {
    int idBook;
    String title;
    String author;
    String publisher;
    int year;

    public Book(int idBook, String title, String author, String publisher, int year) {
        this.idBook = idBook;
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.year = year;
    }

    public int getIdBook() {
        return idBook;
    }

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
