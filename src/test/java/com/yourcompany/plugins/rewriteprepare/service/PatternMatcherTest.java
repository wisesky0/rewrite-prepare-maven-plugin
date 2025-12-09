package com.yourcompany.plugins.rewriteprepare.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PatternMatcher 클래스의 테스트
 * Glob 패턴 매칭 기능을 테스트합니다.
 */
class PatternMatcherTest {

    @Test
    void testExactMatch() {
        assertTrue(PatternMatcher.matches("my-service", "my-service"));
        assertFalse(PatternMatcher.matches("my-service", "other-service"));
    }

    @Test
    void testWildcardMatch() {
        assertTrue(PatternMatcher.matches("*-service", "my-service"));
        assertTrue(PatternMatcher.matches("*-service", "your-service"));
        assertFalse(PatternMatcher.matches("*-service", "my-app"));
    }

    @Test
    void testNullPattern() {
        assertTrue(PatternMatcher.matches(null, "any-value"));
        assertTrue(PatternMatcher.matches(null, null));
    }

    @Test
    void testStarPattern() {
        assertTrue(PatternMatcher.matches("*", "any-value"));
        assertTrue(PatternMatcher.matches("*", ""));
    }

    @Test
    void testNullValue() {
        assertFalse(PatternMatcher.matches("my-service", null));
    }

    @Test
    void testComplexPattern() {
        assertTrue(PatternMatcher.matches("my-*-svc", "my-app-svc"));
        assertTrue(PatternMatcher.matches("my-*-svc", "my-service-svc"));
        assertFalse(PatternMatcher.matches("my-*-svc", "other-app-svc"));
    }

    @Test
    void testDotInPattern() {
        assertTrue(PatternMatcher.matches("com.example.*", "com.example.service"));
        assertFalse(PatternMatcher.matches("com.example.*", "com.other.service"));
    }
}

