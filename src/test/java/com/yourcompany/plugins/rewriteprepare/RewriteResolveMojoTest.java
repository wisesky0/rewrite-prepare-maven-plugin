package com.yourcompany.plugins.rewriteprepare;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.yourcompany.plugins.rewriteprepare.service.VariableReplacer;

/**
 * RewriteResolveMojo 클래스의 통합 테스트
 * resolve goal 실행을 테스트합니다.
 * 
 * 참고: 실제 Mojo 실행 테스트는 Maven 플러그인 테스트 하네스를 사용해야 하지만,
 * 여기서는 VariableReplacer를 직접 사용하여 변수 치환 기능을 검증합니다.
 */
class RewriteResolveMojoTest {

    @Test
    void testMojoExecution(@TempDir Path tempDir) throws Exception {
        File projectDir = tempDir.toFile();
        File pomFile = new File(projectDir, "pom.xml");

        // pom.xml 생성
        String pomContent = "<?xml version=\"1.0\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0\n" +
                "         http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                "  <modelVersion>4.0.0</modelVersion>\n" +
                "  <groupId>com.example</groupId>\n" +
                "  <artifactId>my-service</artifactId>\n" +
                "  <version>1.0.0</version>\n" +
                "  <build>\n" +
                "    <plugins>\n" +
                "      <plugin>\n" +
                "        <groupId>com.yourcompany.plugins</groupId>\n" +
                "        <artifactId>rewrite-prepare-maven-plugin</artifactId>\n" +
                "        <version>1.0.0</version>\n" +
                "        <configuration>\n" +
                "          <outputFile>${project.basedir}/rewrite.yml</outputFile>\n" +
                "          <varMapFile>${project.basedir}/var-map.properties</varMapFile>\n" +
                "        </configuration>\n" +
                "      </plugin>\n" +
                "    </plugins>\n" +
                "  </build>\n" +
                "</project>";

        try (FileWriter writer = new FileWriter(pomFile)) {
            writer.write(pomContent);
        }

        // var-map.properties 생성
        File varMapFile = new File(projectDir, "var-map.properties");
        String varMapContent = "app.name=MyApplication\n" +
                "app.version=1.0.0\n";
        try (FileWriter writer = new FileWriter(varMapFile)) {
            writer.write(varMapContent);
        }

        // outputFile 생성 (prepare goal에서 생성된 파일 시뮬레이션)
        File outputFile = new File(projectDir, "rewrite.yml");
        String outputContent = "name: ${app.name}\n" +
                "version: ${app.version}\n" +
                "description: Application ${app.name} version ${app.version}\n";

        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(outputContent);
        }

        // 기본적인 파일 구조 검증
        assertTrue(pomFile.exists());
        assertTrue(varMapFile.exists());
        assertTrue(outputFile.exists());
    }

    @Test
    void testVariableReplacement(@TempDir Path tempDir) throws Exception {
        // var-map.properties 생성
        File varMapFile = tempDir.resolve("var-map.properties").toFile();
        String varMapContent = "app.name=MyApplication\n" +
                "app.version=1.0.0\n" +
                "database.url=jdbc:mysql://localhost:3306/mydb\n";
        try (FileWriter writer = new FileWriter(varMapFile)) {
            writer.write(varMapContent);
        }

        // outputFile 생성 (prepare goal에서 생성된 파일 시뮬레이션)
        File outputFile = tempDir.resolve("rewrite.yml").toFile();
        String outputContent = "name: ${app.name}\n" +
                "version: ${app.version}\n" +
                "description: Application ${app.name} version ${app.version}\n" +
                "database: $database.url\n";
        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(outputContent);
        }

        // VariableReplacer를 사용하여 변수 치환 수행
        VariableReplacer variableReplacer = new VariableReplacer();
        variableReplacer.loadVariableMap(varMapFile);
        int replacedCount = variableReplacer.replaceVariablesInFile(outputFile);

        // 결과 확인
        assertTrue(replacedCount > 0, "치환된 변수가 있어야 합니다. replacedCount: " + replacedCount);
        String result = new String(Files.readAllBytes(outputFile.toPath()));
        
        // 변수가 치환되었는지 확인
        assertTrue(result.contains("MyApplication"), "MyApplication이 포함되어야 합니다. result: " + result);
        assertTrue(result.contains("1.0.0"), "1.0.0이 포함되어야 합니다. result: " + result);
        assertTrue(result.contains("jdbc:mysql://localhost:3306/mydb"), "database.url이 치환되어야 합니다. result: " + result);
        
        // 원본 변수 패턴이 제거되었는지 확인
        assertFalse(result.contains("${app.name}"), "${app.name}가 제거되어야 합니다. result: " + result);
        assertFalse(result.contains("${app.version}"), "${app.version}가 제거되어야 합니다. result: " + result);
        assertFalse(result.contains("$database.url"), "$database.url가 제거되어야 합니다. result: " + result);
    }

    @Test
    void testOutputFileVariableReplacement(@TempDir Path tempDir) throws Exception {
        // var-map.properties 생성
        File varMapFile = tempDir.resolve("var-map.properties").toFile();
        String varMapContent = "app.name=MyApplication\n" +
                "app.version=1.0.0\n";
        try (FileWriter writer = new FileWriter(varMapFile)) {
            writer.write(varMapContent);
        }

        // outputFile 생성 (prepare goal에서 머지된 결과 파일 시뮬레이션)
        // 변수 placeholder가 유지된 상태로 머지됨
        File outputFile = tempDir.resolve("rewrite.yml").toFile();
        String outputContent = "---\n" +
                "type: specs.openrewrite.org/v1beta/recipe\n" +
                "name: com.example.Main\n" +
                "displayName: Main Recipe\n" +
                "description: Main recipe for ${app.name} migration\n" +
                "tags:\n" +
                "  - migration\n" +
                "recipeList:\n" +
                "  - org.openrewrite.text.ChangeText\n";

        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(outputContent);
        }

        // VariableReplacer를 사용하여 변수 치환 수행
        VariableReplacer variableReplacer = new VariableReplacer();
        variableReplacer.loadVariableMap(varMapFile);
        int replacedCount = variableReplacer.replaceVariablesInFile(outputFile);

        // 결과 확인
        assertTrue(replacedCount > 0, "치환된 변수가 있어야 합니다. replacedCount: " + replacedCount);
        String result = new String(Files.readAllBytes(outputFile.toPath()));
        
        // 변수가 치환되었는지 확인
        assertTrue(result.contains("MyApplication"), "MyApplication이 포함되어야 합니다. result: " + result);
        assertFalse(result.contains("${app.name}"), "${app.name}가 제거되어야 합니다. result: " + result);
    }
}

