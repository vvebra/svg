package lt.uhealth.aipi.svg.model;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RestErrorTest {

    @Test
    void givenTooEarlyOrTooLateErrorString_whenFromErrorString_thenCorrect() {
        //given
        String errorString = """
                {
                	"issues": [
                		{
                			"code": "custom",
                			"message": "Request was made too early or too late.",
                			"params": {
                				"expected": {
                					"after": 1886,
                					"before": 3643
                				},
                				"actual": 1881
                			},
                			"path": []
                		}
                	],
                	"name": "AIπ.Co Error",
                	"message": "Validation problems. Check issues."
                }
                """;

        //when
        RestError restError = RestError.fromErrorString(errorString);

        //then
        assertEquals("AIπ.Co Error", restError.name());
        assertEquals("Validation problems. Check issues.", restError.message());
        assertEquals(1, restError.issues().size());

        Issue issue = restError.issues().getFirst();
        assertEquals("custom", issue.code());
        assertEquals("Request was made too early or too late.", issue.message());
        assertEquals(1886, issue.params().expected().after());
        assertEquals(3643, issue.params().expected().before());
        assertEquals(1881, issue.params().actual());

        assertTrue(restError.isTooEarly());
        assertFalse(restError.isTooLate());
        assertEquals(5, restError.tooEarlyByMillis());
    }

    @Test
    void givenUnavailableErrorString_whenFromErrorString_thenCorrect() {
        //given
        String errorString = """
                {
                 	"message": "AIπ.Co is currently unavailable. Please try again later.",
                 	"name": "AIπ.Co Error"
                 }
                """;

        //when
        RestError restError = RestError.fromErrorString(errorString);

        //then
        assertEquals("AIπ.Co Error", restError.name());
        assertEquals("AIπ.Co is currently unavailable. Please try again later.", restError.message());

        assertFalse(restError.isTooEarly());
        assertFalse(restError.isTooLate());
        assertNull(restError.tooEarlyByMillis());
    }

    @Test
    void givenMissingObjectErrorString_whenFromErrorString_thenCorrect(){
        //given
        String errorString = """
                {
                	"issues": [
                		{
                			"code": "invalid_type",
                			"expected": "object",
                			"received": "null",
                			"path": [
                				"responses"
                			],
                			"message": "Expected object, received null"
                		}
                	],
                	"name": "AIπ.Co Error",
                	"message": "Validation problems. Check issues."
                }
                """;

        //when
        RestError restError = RestError.fromErrorString(errorString);

        //then
        assertEquals("AIπ.Co Error", restError.name());
        assertEquals("Validation problems. Check issues.", restError.message());
        assertEquals(1, restError.issues().size());

        Issue issue = restError.issues().getFirst();
        assertEquals("invalid_type", issue.code());
        assertEquals("Expected object, received null", issue.message());
        assertEquals("object", issue.expected());
        assertEquals("null", issue.received());
        assertEquals(1, issue.path().size());
        assertEquals("responses", issue.path().getFirst());

        assertFalse(restError.isTooEarly());
        assertFalse(restError.isTooLate());
        assertNull(restError.tooEarlyByMillis());
    }

    @Test
    void givenMissingDepErrorString_whenFromErrorString_thenCorrect(){
        //given
        String errorString = """
                {
                 	"issues": [
                 		{
                 			"code": "invalid_type",
                 			"expected": "string",
                 			"received": "undefined",
                 			"path": [
                 				"responses",
                 				"27"
                 			],
                 			"message": "Required"
                 		},
                        {
                            "code": "invalid_type",
                            "expected": "string",
                            "received": "undefined",
                            "path": [
                                "responses",
                                "36"
                            ],
                            "message": "Required"
                        }
                 	],
                 	"name": "AIπ.Co Error",
                 	"message": "Validation problems. Check issues."
                }
                """;

        //when
        RestError restError = RestError.fromErrorString(errorString);

        //then
        assertEquals("AIπ.Co Error", restError.name());
        assertEquals("Validation problems. Check issues.", restError.message());
        assertEquals(2, restError.issues().size());

        Issue issue = restError.issues().getFirst();
        assertEquals("invalid_type", issue.code());
        assertEquals("Required", issue.message());
        assertEquals("string", issue.expected());
        assertEquals("undefined", issue.received());
        assertEquals(2, issue.path().size());
        assertEquals("responses", issue.path().getFirst());
        assertEquals("27", issue.path().get(1));

        assertTrue(restError.isMissingDependencies());
        Set<Integer> deps = restError.getMissingDependencies();
        assertEquals(2, deps.size());
        assertTrue(deps.contains(27));
        assertTrue(deps.contains(36));
    }
}