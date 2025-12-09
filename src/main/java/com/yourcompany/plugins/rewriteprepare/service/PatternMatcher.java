package com.yourcompany.plugins.rewriteprepare.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Glob 패턴 매칭을 수행하는 유틸리티 클래스
 * artifactId와 groupId 패턴 매칭에 사용됩니다.
 */
public class PatternMatcher {
    private static final Logger logger = LoggerFactory.getLogger(PatternMatcher.class);

    /**
     * Glob 패턴을 정규표현식으로 변환합니다.
     *
     * @param globPattern Glob 패턴 (예: "*-svc", "my-service")
     * @return 정규표현식 Pattern 객체
     */
    private static Pattern convertGlobToRegex(String globPattern) {
        if (globPattern == null || "*".equals(globPattern)) {
            return Pattern.compile(".*");
        }

        // Glob 패턴을 정규표현식으로 변환
        String regex = globPattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".");

        return Pattern.compile("^" + regex + "$");
    }

    /**
     * 주어진 문자열이 Glob 패턴과 일치하는지 확인합니다.
     *
     * @param pattern Glob 패턴 (null이거나 "*"인 경우 모든 문자열과 일치)
     * @param value 매칭할 문자열
     * @return 일치하면 true, 아니면 false
     */
    public static boolean matches(String pattern, String value) {
        if (pattern == null || "*".equals(pattern)) {
            return true;
        }

        if (value == null) {
            return false;
        }

        Pattern regexPattern = convertGlobToRegex(pattern);
        boolean matches = regexPattern.matcher(value).matches();
        
        logger.debug("패턴 매칭: pattern={}, value={}, result={}", pattern, value, matches);
        return matches;
    }
}

