package com.arsnyan.cloudstorageservice.validation.validator;

import org.apache.catalina.util.URLEncoder;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class URIPathConstraintValidatorTest {
    private final URLEncoder encoder = new URLEncoder();
    private final URIPathConstraintValidator validator = new URIPathConstraintValidator();

    @Test
    void isValid_IfInput_IsNull() {
        assertTrue(isValid(null));
    }

    @Test
    void isValid_IfInput_IsEmpty() {
        assertTrue(isValid(""));
    }

    @Test
    void isInvalid_IfInput_StartsWithSlash() {
        var input = "/folder/1/text.txt";

        var isValid = isValid(input);

        assertFalse(isValid);
    }

    @Test
    void isInvalid_IfInput_Contains_StandaloneDoubleDots_InBeginning() {
        var input = "../1/text.txt";

        var isValid = isValid(input);

        assertFalse(isValid);
    }

    @Test
    void isValid_IfInput_ContainsDoubleDots_InBetweenForFile() {
        var input = "folder/1/text..txt";

        var isValid = isValid(input);

        assertTrue(isValid);
    }

    @Test
    void isInvalid_IfInput_ContainsStandaloneDoubleDots_InEnd() {
        var input = "folder/1/..";

        var isValid = isValid(input);

        assertFalse(isValid);
    }

    @Test
    void isInvalid_IfInput_ContainsDoubleSlashes_InBetween() {
        var input = "folder//1/text.txt/";

        var isValid = isValid(input);

        assertFalse(isValid);
    }

    @Test
    void isInvalid_IfInput_ContainsDoubleSlashes_InEnd() {
        var input = "folder/1/text.txt//";

        var isValid = isValid(input);

        assertFalse(isValid);
    }

    @Test
    void isValid_IfInput_IsURLWithEncodedSpaces() {
        var input = encode("/folder/1/text+example.txt");

        var isValid = isValid(input);

        assertTrue(isValid);
    }

    @Test
    void isValid_IfInput_IsURLWithEncodedSpaces_AsStandalonePath() {
        var input = encode("text+example.txt");

        var isValid = isValid(input);

        assertTrue(isValid);
    }

    @Test
    void isValid_IfInput_IsURLWithEncodedSlashes() {
        var input = encode("/folder/1/text/");

        var isValid = isValid(input);

        assertTrue(isValid);
    }

    @Test
    void isValid_IfInput_HasATrailingSlash() {
        var input = "folder/1/text/";

        var isValid = isValid(input);

        assertTrue(isValid);
    }

    // use only to simulate encoding in browsers
    private String encode(String value) {
        return encoder.encode(value, StandardCharsets.UTF_8);
    }

    private boolean isValid(String input) {
        return validator.isValid(input, null);
    }
}