import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HelloWorldTest {

    @Test
    void shouldReturnCorrectMessage() {

        HelloWorld app = new HelloWorld();

        String result = app.getMessage();

        assertEquals("Parisha test - merged!", result);
    }
}
