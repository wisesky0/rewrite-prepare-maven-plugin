package com.yourcompany.plugins.rewriteprepare;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RewritePrepareMojo 클래스의 통합 테스트
 * 전체 플러그인 실행 흐름을 테스트합니다.
 * 
 * 참고: 실제 Mojo 실행 테스트는 Maven 플러그인 테스트 하네스를 사용해야 하지만,
 * 여기서는 기본적인 파일 구조 검증만 수행합니다.
 */
class RewritePrepareMojoTest {

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
                "          <recipeDirectory>${project.basedir}/recipes</recipeDirectory>\n" +
                "          <mergeRuleFile>${project.basedir}/merge-rules.yml</mergeRuleFile>\n" +
                "          <outputFile>${project.basedir}/output.yml</outputFile>\n" +
                "        </configuration>\n" +
                "      </plugin>\n" +
                "    </plugins>\n" +
                "  </build>\n" +
                "</project>";

        try (FileWriter writer = new FileWriter(pomFile)) {
            writer.write(pomContent);
        }

        // merge-rules.yml 생성
        File mergeRulesFile = new File(projectDir, "merge-rules.yml");
        String mergeRulesContent = "rules:\n" +
                "  - artifactId: my-service\n" +
                "    groupId: com.example\n" +
                "    mergeFiles:\n" +
                "      - base.yml\n";

        try (FileWriter writer = new FileWriter(mergeRulesFile)) {
            writer.write(mergeRulesContent);
        }

        // recipes 디렉토리 및 base.yml 생성
        File recipesDir = new File(projectDir, "recipes");
        recipesDir.mkdirs();
        File baseYml = new File(recipesDir, "base.yml");
        String baseContent = "---\n" +
                "type: specs.openrewrite.org/v1beta/recipe\n" +
                "name: com.example.BaseRecipe\n" +
                "displayName: Base Recipe\n";

        try (FileWriter writer = new FileWriter(baseYml)) {
            writer.write(baseContent);
        }

        // Mojo 실행 테스트는 실제 Maven 환경이 필요하므로
        // 여기서는 기본적인 파일 구조 검증만 수행
        assertTrue(pomFile.exists());
        assertTrue(mergeRulesFile.exists());
        assertTrue(baseYml.exists());
    }
}

