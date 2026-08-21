// Custom class (Non-primitive)
class Student {
    String name;
    int age;

    // Constructor
    Student(String name, int age) {
        this.name = name;
        this.age = age;
    }

    // Method
    void display() {
        System.out.println("Student Name: " + name + ", Age: " + age);
    }
}

// Main class (entry point)
public class Main {
    public static void main(String[] args) {

        // 1. String (Non-primitive)
        String greeting = "Hello, Java!";
        System.out.println(greeting.toUpperCase());

        // 2. Array (Non-primitive)
        int[] numbers = {10, 20, 30, 40, 50};
        System.out.println("First element: " + numbers[0]);

        // 3. Object (Custom class ka object)
        Student student1 = new Student("Alice", 21);
        student1.display();

        // 4. Null reference
        String emptyText = null;
        System.out.println("Null reference: " + emptyText);
    }
}
