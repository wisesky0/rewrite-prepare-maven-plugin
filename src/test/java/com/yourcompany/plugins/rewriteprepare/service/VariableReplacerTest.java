package com.yourcompany.plugins.rewriteprepare.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * VariableReplacer 클래스의 테스트
 * 변수 치환 기능을 테스트합니다.
 */
class VariableReplacerTest {

    private VariableReplacer variableReplacer;

    @BeforeEach
    void setUp() {
        variableReplacer = new VariableReplacer();
    }

    @Test
    void testLoadVariableMap(@TempDir Path tempDir) throws IOException {
        File varMapFile = tempDir.resolve("var-map.properties").toFile();
        String propertiesContent = "app.name=MyApplication\n" +
                "app.version=1.0.0\n" +
                "database.url=jdbc:mysql://localhost:3306/mydb\n";

        try (FileWriter writer = new FileWriter(varMapFile)) {
            writer.write(propertiesContent);
        }

        variableReplacer.loadVariableMap(varMapFile);
        assertEquals(3, variableReplacer.getVariableCount());
    }

    @Test
    void testReplaceBraceVariables(@TempDir Path tempDir) throws IOException {
        // var-map.properties 생성
        File varMapFile = tempDir.resolve("var-map.properties").toFile();
        String propertiesContent = "app.name=MyApplication\n" +
                "app.version=1.0.0\n";
        try (FileWriter writer = new FileWriter(varMapFile)) {
            writer.write(propertiesContent);
        }

        variableReplacer.loadVariableMap(varMapFile);

        // recipe 파일 생성
        File recipeFile = tempDir.resolve("recipe.yml").toFile();
        String recipeContent = "name: ${app.name}\n" +
                "version: ${app.version}\n" +
                "description: Application ${app.name} version ${app.version}\n";

        try (FileWriter writer = new FileWriter(recipeFile)) {
            writer.write(recipeContent);
        }

        // 변수 치환 수행
        int replacedCount = variableReplacer.replaceVariablesInFile(recipeFile);

        // 결과 확인
        assertTrue(replacedCount > 0, "치환된 변수가 있어야 합니다. replacedCount: " + replacedCount);
        String result = new String(Files.readAllBytes(recipeFile.toPath()));
        assertTrue(result.contains("MyApplication"), "MyApplication이 포함되어야 합니다. result: " + result);
        assertTrue(result.contains("1.0.0"), "1.0.0이 포함되어야 합니다. result: " + result);
        assertTrue(!result.contains("${app.name}"), "${app.name}가 제거되어야 합니다. result: " + result);
        assertTrue(!result.contains("${app.version}"), "${app.version}가 제거되어야 합니다. result: " + result);
    }

    @Test
    void testReplaceSimpleVariables(@TempDir Path tempDir) throws IOException {
        // var-map.properties 생성
        File varMapFile = tempDir.resolve("var-map.properties").toFile();
        String propertiesContent = "app.name=MyApplication\n" +
                "app.version=1.0.0\n";
        try (FileWriter writer = new FileWriter(varMapFile)) {
            writer.write(propertiesContent);
        }

        variableReplacer.loadVariableMap(varMapFile);

        // recipe 파일 생성
        File recipeFile = tempDir.resolve("recipe.yml").toFile();
        String recipeContent = "name: $app.name\n" +
                "version: $app.version\n" +
                "description: Application $app.name version $app.version\n";

        try (FileWriter writer = new FileWriter(recipeFile)) {
            writer.write(recipeContent);
        }

        // 변수 치환 수행
        int replacedCount = variableReplacer.replaceVariablesInFile(recipeFile);

        // 결과 확인
        assertTrue(replacedCount > 0, "치환된 변수가 있어야 합니다. replacedCount: " + replacedCount);
        String result = new String(Files.readAllBytes(recipeFile.toPath()));
        assertTrue(result.contains("MyApplication"), "MyApplication이 포함되어야 합니다. result: " + result);
        assertTrue(result.contains("1.0.0"), "1.0.0이 포함되어야 합니다. result: " + result);
        assertTrue(!result.contains("$app.name"), "$app.name가 제거되어야 합니다. result: " + result);
        assertTrue(!result.contains("$app.version"), "$app.version가 제거되어야 합니다. result: " + result);
    }

    @Test
    void testReplaceMixedVariables(@TempDir Path tempDir) throws IOException {
        // var-map.properties 생성
        File varMapFile = tempDir.resolve("var-map.properties").toFile();
        String propertiesContent = "app.name=MyApplication\n" +
                "app.version=1.0.0\n";
        try (FileWriter writer = new FileWriter(varMapFile)) {
            writer.write(propertiesContent);
        }

        variableReplacer.loadVariableMap(varMapFile);

        // recipe 파일 생성 (${variable}와 $variable 혼합)
        File recipeFile = tempDir.resolve("recipe.yml").toFile();
        String recipeContent = "name: ${app.name}\n" +
                "version: $app.version\n" +
                "description: Application ${app.name} version $app.version\n";

        try (FileWriter writer = new FileWriter(recipeFile)) {
            writer.write(recipeContent);
        }

        // 변수 치환 수행
        int replacedCount = variableReplacer.replaceVariablesInFile(recipeFile);

        // 결과 확인
        assertTrue(replacedCount > 0, "치환된 변수가 있어야 합니다. replacedCount: " + replacedCount);
        String result = new String(Files.readAllBytes(recipeFile.toPath()));
        assertTrue(result.contains("MyApplication"), "MyApplication이 포함되어야 합니다. result: " + result);
        assertTrue(result.contains("1.0.0"), "1.0.0이 포함되어야 합니다. result: " + result);
        assertTrue(!result.contains("${app.name}"), "${app.name}가 제거되어야 합니다. result: " + result);
        assertTrue(!result.contains("$app.version"), "$app.version가 제거되어야 합니다. result: " + result);
    }

    @Test
    void testFindRecipeFiles(@TempDir Path tempDir) throws IOException {
        File recipeDir = tempDir.toFile();
        
        // 여러 recipe 파일 생성
        File file1 = new File(recipeDir, "recipe1.yml");
        try (FileWriter writer = new FileWriter(file1)) {
            writer.write("name: test1\n");
        }

        File file2 = new File(recipeDir, "recipe2.yaml");
        try (FileWriter writer = new FileWriter(file2)) {
            writer.write("name: test2\n");
        }

        File subDir = new File(recipeDir, "subdir");
        subDir.mkdirs();
        File file3 = new File(subDir, "recipe3.yml");
        try (FileWriter writer = new FileWriter(file3)) {
            writer.write("name: test3\n");
        }

        // recipe 파일 찾기
        List<File> recipeFiles = variableReplacer.findRecipeFiles(recipeDir);

        assertNotNull(recipeFiles);
        assertEquals(3, recipeFiles.size());
    }

    @Test
    void testUnknownVariable(@TempDir Path tempDir) throws IOException {
        // var-map.properties 생성
        File varMapFile = tempDir.resolve("var-map.properties").toFile();
        String propertiesContent = "app.name=MyApplication\n";
        try (FileWriter writer = new FileWriter(varMapFile)) {
            writer.write(propertiesContent);
        }

        variableReplacer.loadVariableMap(varMapFile);

        // recipe 파일 생성 (알 수 없는 변수 포함)
        File recipeFile = tempDir.resolve("recipe.yml").toFile();
        String recipeContent = "name: ${app.name}\n" +
                "version: ${unknown.var}\n";

        try (FileWriter writer = new FileWriter(recipeFile)) {
            writer.write(recipeContent);
        }

        // 변수 치환 수행
        int replacedCount = variableReplacer.replaceVariablesInFile(recipeFile);

        // 결과 확인: 알 수 없는 변수는 원본 유지
        String result = new String(Files.readAllBytes(recipeFile.toPath()));
        assertTrue(result.contains("MyApplication"), "MyApplication이 포함되어야 합니다. result: " + result);
        assertTrue(result.contains("${unknown.var}"), "${unknown.var}가 원본으로 유지되어야 합니다. result: " + result);
        assertTrue(!result.contains("${app.name}"), "${app.name}가 제거되어야 합니다. result: " + result);
    }
}

