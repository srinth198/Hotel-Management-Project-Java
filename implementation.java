import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class Book {
    private String title;

    public Book(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        return "Book{" +
                "title='" + title + '\'' +
                '}';
    }
}

class Library<T> extends Observable {
    private final List<T> items = Collections.synchronizedList(new ArrayList<>());

    public synchronized void addItem(T item) {
        items.add(item);
        setChanged();
        notifyObservers("Added: " + item);
    }

    public synchronized void removeItem(T item) {
        items.remove(item);
        setChanged();
        notifyObservers("Removed: " + item);
    }

    public synchronized List<T> listItems() {
        return new ArrayList<>(items);
    }
}

class Reader implements Runnable, Observer {
    private final Library<Book> library;

    public Reader(Library<Book> library) {
        this.library = library;
        library.addObserver(this);
    }

    @Override
    public void run() {
        while (true) {
            synchronized (library) {
                List<Book> books = library.listItems();
                System.out.println(Thread.currentThread().getName() + " is reading: " + books);
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        System.out.println(Thread.currentThread().getName() + " notified: " + arg);
    }
}

class Writer implements Runnable {
    private final Library<Book> library;
    private final Book book;

    public Writer(Library<Book> library, Book book) {
        this.library = library;
        this.book = book;
    }

    @Override
    public void run() {
        synchronized (library) {
            library.addItem(book);
            System.out.println(Thread.currentThread().getName() + " added: " + book);
        }
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

public class Main {
    public static void main(String[] args) {
        Library<Book> library = new Library<>();
        ExecutorService executor = Executors.newFixedThreadPool(110);

        for (int i = 0; i < 100; i++) {
            executor.submit(new Reader(library));
        }

        for (int i = 0; i < 10; i++) {
            executor.submit(new Writer(library, new Book("Book " + i)));
        }

        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
