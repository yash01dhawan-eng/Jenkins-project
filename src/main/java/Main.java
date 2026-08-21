import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

class Student {
    String name;
    int age;

    Student(String name, int age) {
        this.name = name;
        this.age = age;
    }

    void display() {
        System.out.println("Student Name: " + name + ", Age: " + age);
    }
}

public class Main {

    public static void main(String[] args) throws IOException {

        Student student1 = new Student("Alice", 21);

        HttpServer server = HttpServer.create(
                new InetSocketAddress("0.0.0.0", 8081),
                0
        );

        server.createContext("/", (HttpExchange exchange) -> {

            String response = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <title>Jenkins Java App</title>
                    </head>
                    <body>
                        <h1>Hello from Jenkins + EC2 🚀</h1>
                        <p>Application is running successfully.</p>
                        <p>Student Name: %s</p>
                        <p>Student Age: %d</p>
                    </body>
                    </html>
                    """.formatted(student1.name, student1.age);

            exchange.getResponseHeaders().set(
                    "Content-Type",
                    "text/html; charset=UTF-8"
            );

            exchange.sendResponseHeaders(200, response.getBytes().length);

            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(response.getBytes());
            }
        });

        server.start();

        System.out.println("===== Application Started =====");
        System.out.println("Server running on port 8081");
        System.out.println("Open: http://<EC2-PUBLIC-IP>:8081");
    }
}
