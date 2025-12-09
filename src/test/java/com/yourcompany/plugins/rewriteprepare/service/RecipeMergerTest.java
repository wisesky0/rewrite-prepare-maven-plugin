package com.yourcompany.plugins.rewriteprepare.service;

import com.yourcompany.plugins.rewriteprepare.model.RecipeDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RecipeMerger 클래스의 테스트
 * Recipe 파일 병합 기능을 테스트합니다.
 */
class RecipeMergerTest {

    private RecipeMerger recipeMerger;
    private YamlParser yamlParser;

    @BeforeEach
    void setUp() {
        yamlParser = new YamlParser();
        recipeMerger = new RecipeMerger(yamlParser);
    }

    @Test
    void testMergeFiles(@TempDir Path tempDir) throws Exception {
        File recipeDir = tempDir.toFile();

        // 첫 번째 recipe 파일 생성
        File file1 = new File(recipeDir, "base.yml");
        String yaml1 = "---\n" +
                "type: specs.openrewrite.org/v1beta/recipe\n" +
                "name: com.example.RecipeA\n" +
                "displayName: Recipe A\n";
        try (FileWriter writer = new FileWriter(file1)) {
            writer.write(yaml1);
        }

        // 두 번째 recipe 파일 생성
        File file2 = new File(recipeDir, "service.yml");
        String yaml2 = "---\n" +
                "type: specs.openrewrite.org/v1beta/recipe\n" +
                "name: com.example.RecipeB\n" +
                "displayName: Recipe B\n";
        try (FileWriter writer = new FileWriter(file2)) {
            writer.write(yaml2);
        }

        List<String> mergeFiles = Arrays.asList("base.yml", "service.yml");
        List<RecipeDefinition> merged = recipeMerger.mergeFiles(recipeDir, mergeFiles);

        assertNotNull(merged);
        assertEquals(2, merged.size());
        assertEquals("com.example.RecipeA", merged.get(0).getName());
        assertEquals("com.example.RecipeB", merged.get(1).getName());
    }

    @Test
    void testMergeFilesWithDuplicate(@TempDir Path tempDir) throws Exception {
        File recipeDir = tempDir.toFile();

        File file1 = new File(recipeDir, "base.yml");
        String yaml1 = "---\n" +
                "type: specs.openrewrite.org/v1beta/recipe\n" +
                "name: com.example.RecipeA\n";
        try (FileWriter writer = new FileWriter(file1)) {
            writer.write(yaml1);
        }

        // 중복된 파일 목록
        List<String> mergeFiles = Arrays.asList("base.yml", "base.yml");
        List<RecipeDefinition> merged = recipeMerger.mergeFiles(recipeDir, mergeFiles);

        assertNotNull(merged);
        // 중복은 skip되므로 1개만 있어야 함
        assertEquals(1, merged.size());
    }

    @Test
    void testMergeFilesWithNonExistentFile(@TempDir Path tempDir) throws Exception {
        File recipeDir = tempDir.toFile();

        File file1 = new File(recipeDir, "base.yml");
        String yaml1 = "---\n" +
                "type: specs.openrewrite.org/v1beta/recipe\n" +
                "name: com.example.RecipeA\n";
        try (FileWriter writer = new FileWriter(file1)) {
            writer.write(yaml1);
        }

        // 존재하지 않는 파일 포함
        List<String> mergeFiles = Arrays.asList("base.yml", "nonexistent.yml");
        List<RecipeDefinition> merged = recipeMerger.mergeFiles(recipeDir, mergeFiles);

        assertNotNull(merged);
        // 존재하는 파일만 병합되므로 1개
        assertEquals(1, merged.size());
    }
}

