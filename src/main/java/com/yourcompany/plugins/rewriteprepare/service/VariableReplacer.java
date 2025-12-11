package com.yourcompany.plugins.rewriteprepare.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 변수 치환을 수행하는 서비스 클래스
 * 
 * recipe 파일에서 ${variable} 또는 $variable 형식의 변수를
 * var-map.properties 파일에서 찾아서 값을 치환합니다.
 */
public class VariableReplacer {
    private static final Logger logger = LoggerFactory.getLogger(VariableReplacer.class);

    // ${variable} 패턴: ${로 시작하고 }로 끝나는 단어 (점 포함 가능)
    // 예: ${app.name}, ${database.url}
    private static final Pattern BRACE_VARIABLE_PATTERN = Pattern.compile("\\$\\{([a-zA-Z_][a-zA-Z0-9_.]*)\\}");
    
    // $variable 패턴: $로 시작하고 단어 경계까지 (점 포함 가능)
    // 예: $app.name, $database.url
    private static final Pattern SIMPLE_VARIABLE_PATTERN = Pattern.compile("\\$([a-zA-Z_][a-zA-Z0-9_.]*)\\b");

    private Map<String, String> variableMap;

    public VariableReplacer() {
        this.variableMap = new HashMap<>();
    }

    /**
     * var-map.properties 파일을 읽어서 변수 맵을 로드합니다.
     * 
     * @param varMapFile 변수 맵 파일
     * @throws IOException 파일 읽기 오류
     */
    public void loadVariableMap(File varMapFile) throws IOException {
        logger.debug("변수 맵 파일 로드 시작: {}", varMapFile.getAbsolutePath());
        
        Properties properties = new Properties();
        try (InputStream inputStream = new FileInputStream(varMapFile)) {
            properties.load(inputStream);
        }

        variableMap.clear();
        for (String key : properties.stringPropertyNames()) {
            String value = properties.getProperty(key);
            if (value != null) {
                variableMap.put(key.trim(), value.trim());
                logger.debug("변수 로드: {} = {}", key.trim(), value.trim());
            } else {
                variableMap.put(key.trim(), "");
                logger.debug("변수 로드: {} = (빈 값)", key.trim());
            }
        }

        logger.debug("변수 맵 로드 완료: {} 개의 변수", variableMap.size());
        if (logger.isDebugEnabled() && !variableMap.isEmpty()) {
            logger.debug("로드된 변수 목록: {}", variableMap.keySet());
        }
    }

    /**
     * 변수 맵에 있는 변수의 개수를 반환합니다.
     * 
     * @return 변수 개수
     */
    public int getVariableCount() {
        return variableMap.size();
    }

    /**
     * recipeDirectory에서 모든 recipe 파일(.yml, .yaml)을 찾습니다.
     * 
     * @param recipeDirectory recipe 파일들이 있는 디렉토리
     * @return recipe 파일 목록
     */
    public List<File> findRecipeFiles(File recipeDirectory) {
        List<File> recipeFiles = new ArrayList<>();
        
        if (!recipeDirectory.exists() || !recipeDirectory.isDirectory()) {
            return recipeFiles;
        }

        findRecipeFilesRecursive(recipeDirectory, recipeFiles);
        return recipeFiles;
    }

    /**
     * 재귀적으로 recipe 파일을 찾습니다.
     * 
     * @param directory 검색할 디렉토리
     * @param recipeFiles 결과를 담을 리스트
     */
    private void findRecipeFilesRecursive(File directory, List<File> recipeFiles) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                findRecipeFilesRecursive(file, recipeFiles);
            } else if (file.isFile()) {
                String fileName = file.getName().toLowerCase();
                if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) {
                    recipeFiles.add(file);
                }
            }
        }
    }

    /**
     * 파일에서 변수를 치환합니다.
     * 
     * @param file 처리할 파일
     * @return 치환된 변수의 개수
     * @throws IOException 파일 읽기/쓰기 오류
     */
    public int replaceVariablesInFile(File file) throws IOException {
        logger.debug("파일 변수 치환 시작: {}", file.getAbsolutePath());

        // 파일 내용 읽기
        String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        String originalContent = content;

        // ${variable} 패턴 치환
        content = replaceBraceVariables(content);
        
        // $variable 패턴 치환 (${variable}로 치환되지 않은 것만)
        content = replaceSimpleVariables(content);

        // 변경사항이 있으면 파일에 쓰기
        if (!content.equals(originalContent)) {
            try (Writer writer = new OutputStreamWriter(
                    new FileOutputStream(file), StandardCharsets.UTF_8)) {
                writer.write(content);
            }
            
            int replacedCount = countReplacements(originalContent, content);
            logger.debug("파일 변수 치환 완료: {} ({} 개의 변수 치환)", file.getName(), replacedCount);
            return replacedCount;
        }

        logger.debug("파일 변수 치환 완료: {} (변경사항 없음)", file.getName());
        return 0;
    }

    /**
     * ${variable} 형식의 변수를 치환합니다.
     * 
     * @param content 원본 내용
     * @return 치환된 내용
     */
    private String replaceBraceVariables(String content) {
        Matcher matcher = BRACE_VARIABLE_PATTERN.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String variableName = matcher.group(1);
            String replacement = variableMap.get(variableName);
            
            if (replacement != null) {
                // $와 \를 이스케이프 처리
                String escapedReplacement = Matcher.quoteReplacement(replacement);
                matcher.appendReplacement(result, escapedReplacement);
                logger.debug("변수 치환: ${} -> {}", variableName, replacement);
            } else {
                logger.warn("변수를 찾을 수 없습니다: ${}", variableName);
                // 변수를 찾을 수 없으면 원본 유지
                String originalMatch = matcher.group(0);
                matcher.appendReplacement(result, Matcher.quoteReplacement(originalMatch));
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * $variable 형식의 변수를 치환합니다.
     * 
     * @param content 원본 내용
     * @return 치환된 내용
     */
    private String replaceSimpleVariables(String content) {
        Matcher matcher = SIMPLE_VARIABLE_PATTERN.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String variableName = matcher.group(1);
            String replacement = variableMap.get(variableName);
            
            if (replacement != null) {
                // $와 \를 이스케이프 처리
                String escapedReplacement = Matcher.quoteReplacement(replacement);
                matcher.appendReplacement(result, escapedReplacement);
                logger.debug("변수 치환: ${} -> {}", variableName, replacement);
            } else {
                logger.warn("변수를 찾을 수 없습니다: ${}", variableName);
                // 변수를 찾을 수 없으면 원본 유지
                String originalMatch = matcher.group(0);
                matcher.appendReplacement(result, Matcher.quoteReplacement(originalMatch));
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 치환된 변수의 개수를 계산합니다.
     * 
     * @param original 원본 내용
     * @param replaced 치환된 내용
     * @return 치환된 변수의 개수
     */
    private int countReplacements(String original, String replaced) {
        if (original.equals(replaced)) {
            return 0;
        }

        // 정확한 개수를 세기 위해 원본에서 치환 가능한 변수 개수 계산
        int count = 0;
        
        // ${variable} 패턴 개수
        Matcher braceMatcher = BRACE_VARIABLE_PATTERN.matcher(original);
        while (braceMatcher.find()) {
            String variableName = braceMatcher.group(1);
            if (variableMap.containsKey(variableName)) {
                count++;
            }
        }
        
        // $variable 패턴 개수 (${variable} 패턴과 겹치지 않는 것만)
        // 원본에서 ${variable} 패턴을 임시로 마킹한 후 $variable 패턴 찾기
        String marked = original.replaceAll("\\$\\{[^}]+\\}", "___BRACE_PATTERN___");
        Matcher simpleMatcher = SIMPLE_VARIABLE_PATTERN.matcher(marked);
        while (simpleMatcher.find()) {
            String variableName = simpleMatcher.group(1);
            if (variableMap.containsKey(variableName)) {
                count++;
            }
        }

        return count;
    }
}

