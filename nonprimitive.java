// A custom class (Non-primitive)
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
    public static void main(String[] args) {
        // 1. String: A built-in non-primitive type
        String greeting = "Hello, Java!";
        System.out.println(greeting.toUpperCase()); // Non-primitives can call methods

        // 2. Array: Stores a collection of values
        int[] numbers = {10, 20, 30, 40, 50};
        System.out.println("First element of array: " + numbers[0]);

        // 3. Object: Created from a custom class
        Student student1 = new Student("Alice", 21);
        student1.display();

        // Non-primitives can be null
        String emptyText = null; 
        System.out.println("Null reference: " + emptyText);
    }
}
