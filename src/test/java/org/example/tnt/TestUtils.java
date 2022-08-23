package org.example.tnt;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public abstract class TestUtils {

    public interface RunnableEx {
        void run() throws Exception;
    }


    public static void expectException(RunnableEx dangerousCode, Class<? extends Throwable> expectedExceptionClass, String... expectedMessages) {
        assertNotNull("Runnable cannot be null", dangerousCode);
        assertNotNull("Expected Exception Class cannot be null", expectedExceptionClass);
        assertNotNull("Expected Exception Messages cannot be null", expectedMessages);
        assertTrue("Expected Exception Messages cannot be empty", expectedMessages.length > 0);

        try {
            dangerousCode.run();
        } catch (Throwable e) {
            if (e.getClass().isAssignableFrom(expectedExceptionClass)) {
                String errorMessage = e.getMessage();
                if (expectedMessages.length == 1) {
                    assertEquals(expectedMessages[0], errorMessage);
                } else {
                    for (String substring : expectedMessages) {
                        assertTrue("Error message has no expected substring", errorMessage.contains(substring));
                    }
                }
                return;
            }
            fail("Wrong exception class! Expected " + expectedExceptionClass.getSimpleName() + " actual " + e.getClass().getSimpleName());
        }
        fail("No any errors appears, but we are expecting " + expectedExceptionClass.getSimpleName());
    }


    public static void expectIllegalArgumentException(RunnableEx dangerousCode, String... expectedMessages) {
        expectException(dangerousCode, IllegalArgumentException.class, expectedMessages);
    }

}
