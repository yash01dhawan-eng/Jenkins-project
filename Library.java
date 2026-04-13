// Custom class (Non-primitive)
class Book {
    String title;
    double price;

    // Constructor
    Book(String title, double price) {
        this.title = title;
        this.price = price;
    }

    void showDetails() {
        System.out.println("Book: " + title + " | Price: ₹" + price);
    }
}

public class Library { // File name must be Library.java
    public static void main(String[] args) {
        // 1. Object (Custom Reference Type)
        Book myBook = new Book("Java Programming", 599.0);
        myBook.showDetails();

        // 2. Array of Objects (Non-primitive)
        Book[] collection = new Book[2];
        collection[0] = new Book("Python Basics", 450.0);
        collection[1] = new Book("C++ Guide", 300.0);

        System.out.println("Collection size: " + collection.length);
        collection[0].showDetails();
    }
}
