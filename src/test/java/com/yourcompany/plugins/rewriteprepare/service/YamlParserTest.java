package com.yourcompany.plugins.rewriteprepare.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.yourcompany.plugins.rewriteprepare.model.MergeRules;
import com.yourcompany.plugins.rewriteprepare.model.Recipe;
import com.yourcompany.plugins.rewriteprepare.model.RecipeDefinition;

/**
 * YamlParser 클래스의 테스트
 * YAML 파일 파싱 기능을 테스트합니다.
 */
class YamlParserTest {

    private YamlParser yamlParser;

    @BeforeEach
    void setUp() {
        yamlParser = new YamlParser();
    }

    @Test
    void testParseMergeRules(@TempDir Path tempDir) throws IOException {
        File mergeRuleFile = tempDir.resolve("merge-rules.yml").toFile();
        String yamlContent = "rules:\n" +
                "  - artifactId: my-service\n" +
                "    groupId: com.example\n" +
                "    mergeFiles:\n" +
                "      - base.yml\n" +
                "      - service.yml\n";

        try (FileWriter writer = new FileWriter(mergeRuleFile)) {
            writer.write(yamlContent);
        }

        MergeRules mergeRules = yamlParser.parseMergeRules(mergeRuleFile);
        assertNotNull(mergeRules);
        assertNotNull(mergeRules.getRules());
        assertEquals(1, mergeRules.getRules().size());
        assertEquals("my-service", mergeRules.getRules().get(0).getArtifactId());
        assertEquals("com.example", mergeRules.getRules().get(0).getGroupId());
    }

    @Test
    void testParseRecipeFile(@TempDir Path tempDir) throws IOException {
        File recipeFile = tempDir.resolve("recipe.yml").toFile();
        String yamlContent = "---\n" +
                "type: specs.openrewrite.org/v1beta/recipe\n" +
                "name: com.example.RecipeA\n" +
                "displayName: Recipe A\n" +
                "recipeList:\n" +
                "  - com.example.RecipeB\n" +
                "  - com.example.RecipeC\n";

        try (FileWriter writer = new FileWriter(recipeFile)) {
            writer.write(yamlContent);
        }

        List<RecipeDefinition> recipes = yamlParser.parseRecipeFile(recipeFile);
        assertNotNull(recipes);
        assertEquals(1, recipes.size());
        assertEquals("com.example.RecipeA", recipes.get(0).getName());
        assertNotNull(recipes.get(0).getRecipeList());
        assertEquals(2, recipes.get(0).getRecipeList().size());
    }

    @Test
    void testWriteRecipes(@TempDir Path tempDir) throws IOException {
        File outputFile = tempDir.resolve("output.yml").toFile();

        RecipeDefinition recipe = new RecipeDefinition();
        recipe.setType("specs.openrewrite.org/v1beta/recipe");
        recipe.setName("com.example.TestRecipe");
        recipe.setDisplayName("Test Recipe");

        List<RecipeDefinition> recipes = Arrays.asList(recipe);
        yamlParser.writeRecipes(recipes, outputFile);

        assertTrue(outputFile.exists());
        assertTrue(outputFile.length() > 0);
    }

    @Test
    void testParseMultipleRecipeDefinitions(@TempDir Path tempDir) throws IOException {
        // requirements.md의 39-69 라인 형식을 여러 개 포함한 YAML 파일 생성
        File recipeFile = tempDir.resolve("multiple-recipes.yml").toFile();
        String yamlContent = "---\n" +
                "type: specs.openrewrite.org/v1beta/recipe\n" +
                "name: com.yourorg.RecipeA\n" +
                "displayName: Recipe A\n" +
                "description: Applies Recipe A.\n" +
                "tags:\n" +
                "  - tag1\n" +
                "  - tag2\n" +
                "estimatedEffortPerOccurrence: PT15M\n" +
                "causesAnotherCycle: false\n" +
                "preconditions:\n" +
                "  - org.openrewrite.text.Find:\n" +
                "      find: 1\n" +
                "exclusions:\n" +
                "  - org.openrewrite.text.Find:\n" +
                "      find: 1\n" +
                "recipeList:\n" +
                "  - org.openrewrite.text.ChangeText:\n" +
                "      toText: 2\n" +
                "  - com.yourorg.RecipeB:\n" +
                "      exampleConfig1: foo\n" +
                "      exampleConfig2: bar\n" +
                "  - com.yourorg.RecipeC\n" +
                "---\n" +
                "type: specs.openrewrite.org/v1beta/recipe\n" +
                "name: com.yourorg.RecipeD\n" +
                "displayName: Recipe D\n" +
                "description: Applies Recipe D.\n" +
                "tags:\n" +
                "  - tag3\n" +
                "  - tag4\n" +
                "estimatedEffortPerOccurrence: PT20M\n" +
                "causesAnotherCycle: true\n" +
                "recipeList:\n" +
                "  - com.yourorg.RecipeE\n" +
                "  - com.yourorg.RecipeF:\n" +
                "      config1: value1\n" +
                "---\n" +
                "type: specs.openrewrite.org/v1beta/recipe\n" +
                "name: com.yourorg.RecipeG\n" +
                "displayName: Recipe G\n" +
                "recipeList:\n" +
                "  - com.yourorg.RecipeH\n";

        try (FileWriter writer = new FileWriter(recipeFile)) {
            writer.write(yamlContent);
        }

        // 여러 개의 recipe definition 파싱 테스트
        List<RecipeDefinition> recipes = yamlParser.parseRecipeFile(recipeFile);
        
        assertNotNull(recipes);
        assertEquals(3, recipes.size(), "3개의 recipe definition이 파싱되어야 합니다.");
        
        // 첫 번째 recipe 검증
        RecipeDefinition recipeA = recipes.get(0);
        assertEquals("com.yourorg.RecipeA", recipeA.getName());
        assertEquals("Recipe A", recipeA.getDisplayName());
        assertEquals("Applies Recipe A.", recipeA.getDescription());
        assertNotNull(recipeA.getTags());
        assertEquals(2, recipeA.getTags().size());
        assertEquals("tag1", recipeA.getTags().get(0));
        assertEquals("tag2", recipeA.getTags().get(1));
        assertEquals("PT15M", recipeA.getEstimatedEffortPerOccurrence());
        assertEquals(false, recipeA.getCausesAnotherCycle());
        assertNotNull(recipeA.getPreconditions());
        assertEquals(1, recipeA.getPreconditions().size());
        assertNotNull(recipeA.getExclusions());
        assertEquals(1, recipeA.getExclusions().size());
        assertNotNull(recipeA.getRecipeList());
        assertEquals(3, recipeA.getRecipeList().size());
        
        // recipeList의 각 항목 검증
        // 1. org.openrewrite.text.ChangeText: { toText: 2 }
        Recipe recipe1 = recipeA.getRecipeList().get(0);
        assertEquals("org.openrewrite.text.ChangeText", recipe1.getName());
        assertEquals(2, recipe1.getAttributes().get("toText"));
        
        // 2. com.yourorg.RecipeB: { exampleConfig1: foo, exampleConfig2: bar }
        Recipe recipe2 = recipeA.getRecipeList().get(1);
        assertEquals("com.yourorg.RecipeB", recipe2.getName());
        assertEquals("foo", recipe2.getAttributes().get("exampleConfig1"));
        assertEquals("bar", recipe2.getAttributes().get("exampleConfig2"));
        
        // 3. com.yourorg.RecipeC (단순 문자열 -> Recipe{name:"com.yourorg.RecipeC", attributes: {}}로 변환)
        Recipe recipe3 = recipeA.getRecipeList().get(2);
        assertEquals("com.yourorg.RecipeC", recipe3.getName());
        assertTrue(recipe3.getAttributes().isEmpty(), "단순 문자열은 빈 attributes로 변환되어야 합니다.");
        
        // 두 번째 recipe 검증
        RecipeDefinition recipeD = recipes.get(1);
        assertEquals("com.yourorg.RecipeD", recipeD.getName());
        assertEquals("Recipe D", recipeD.getDisplayName());
        assertEquals("Applies Recipe D.", recipeD.getDescription());
        assertNotNull(recipeD.getTags());
        assertEquals(2, recipeD.getTags().size());
        assertEquals("tag3", recipeD.getTags().get(0));
        assertEquals("tag4", recipeD.getTags().get(1));
        assertEquals("PT20M", recipeD.getEstimatedEffortPerOccurrence());
        assertEquals(true, recipeD.getCausesAnotherCycle());
        assertNotNull(recipeD.getRecipeList());
        assertEquals(2, recipeD.getRecipeList().size());
        
        // 세 번째 recipe 검증
        RecipeDefinition recipeG = recipes.get(2);
        assertEquals("com.yourorg.RecipeG", recipeG.getName());
        assertEquals("Recipe G", recipeG.getDisplayName());
        assertNotNull(recipeG.getRecipeList());
        assertEquals(1, recipeG.getRecipeList().size());
    }

    @Test
    void testParseAndWriteMultipleRecipes(@TempDir Path tempDir) throws IOException {
        // 여러 개의 recipe definition을 파싱하고 다시 쓰는 테스트
        File inputFile = tempDir.resolve("input-recipes.yml").toFile();
        File outputFile = tempDir.resolve("output-recipes.yml").toFile();
        
        String yamlContent = "---\n" +
                "type: specs.openrewrite.org/v1beta/recipe\n" +
                "name: com.example.Recipe1\n" +
                "displayName: Recipe 1\n" +
                "recipeList:\n" +
                "  - com.example.Recipe1A\n" +
                "---\n" +
                "type: specs.openrewrite.org/v1beta/recipe\n" +
                "name: com.example.Recipe2\n" +
                "displayName: Recipe 2\n" +
                "tags:\n" +
                "  - test\n" +
                "recipeList:\n" +
                "  - com.example.Recipe2A\n" +
                "  - com.example.Recipe2B\n";

        try (FileWriter writer = new FileWriter(inputFile)) {
            writer.write(yamlContent);
        }

        // 파싱
        List<RecipeDefinition> recipes = yamlParser.parseRecipeFile(inputFile);
        assertEquals(2, recipes.size());
        
        // 다시 쓰기
        yamlParser.writeRecipes(recipes, outputFile);
        
        assertTrue(outputFile.exists());
        assertTrue(outputFile.length() > 0);
        
        // 다시 파싱하여 검증
        List<RecipeDefinition> parsedAgain = yamlParser.parseRecipeFile(outputFile);
        assertEquals(2, parsedAgain.size());
        assertEquals("com.example.Recipe1", parsedAgain.get(0).getName());
        assertEquals("com.example.Recipe2", parsedAgain.get(1).getName());
        
        // 생성된 파일에 '---' 구분자가 포함되어 있는지 확인
        String fileContent = new String(java.nio.file.Files.readAllBytes(outputFile.toPath()));
        assertTrue(fileContent.contains("---"), "생성된 파일에 '---' 구분자가 포함되어야 합니다.");
        // 첫 번째 문서는 '---'로 시작하지 않을 수 있지만, 두 번째 문서는 '---'로 시작해야 함
        assertTrue(fileContent.contains("\n---\n") || fileContent.startsWith("---\n"), 
                   "여러 문서 사이에 '---' 구분자가 있어야 합니다.");
    }

    @Test
    void testSerializeOmitsNullAndEmptyValues(@TempDir Path tempDir) throws IOException {
        // requirements.md에 따르면 optional 필드는 "값이 없으면 출력하지 않습니다"
        // null 또는 empty 값이 serialize 시 생략되는지 테스트
        File outputFile = tempDir.resolve("output-omit-null.yml").toFile();

        RecipeDefinition recipe = new RecipeDefinition();
        recipe.setType("specs.openrewrite.org/v1beta/recipe");
        recipe.setName("com.example.TestRecipe");
        // displayName, description, tags, estimatedEffortPerOccurrence, 
        // causesAnotherCycle, preconditions, exclusions, recipeList는 null로 설정

        List<RecipeDefinition> recipes = Arrays.asList(recipe);
        yamlParser.writeRecipes(recipes, outputFile);

        assertTrue(outputFile.exists());
        
        // 생성된 파일 내용 확인
        String fileContent = new String(java.nio.file.Files.readAllBytes(outputFile.toPath()));
        
        // 필수 필드는 포함되어야 함
        assertTrue(fileContent.contains("type:") && fileContent.contains("specs.openrewrite.org/v1beta/recipe"), 
                   "type 필드는 출력되어야 합니다.");
        assertTrue(fileContent.contains("name:") && fileContent.contains("com.example.TestRecipe"), 
                   "name 필드는 출력되어야 합니다.");
        
        // null/empty 값은 생략되어야 함 (requirements.md: "값이 없으면 출력하지 않습니다")
        // Jackson의 기본 동작은 null을 출력할 수 있으므로, 실제로 생략되는지 확인
        assertTrue(!fileContent.contains("displayName:"), 
                   "displayName이 null이면 출력되지 않아야 합니다.");
        assertTrue(!fileContent.contains("description:"), 
                   "description이 null이면 출력되지 않아야 합니다.");
        assertTrue(!fileContent.contains("tags:"), 
                   "tags가 null이면 출력되지 않아야 합니다.");
        assertTrue(!fileContent.contains("estimatedEffortPerOccurrence:"), 
                   "estimatedEffortPerOccurrence가 null이면 출력되지 않아야 합니다.");
        assertTrue(!fileContent.contains("causesAnotherCycle:"), 
                   "causesAnotherCycle이 null이면 출력되지 않아야 합니다.");
        assertTrue(!fileContent.contains("preconditions:"), 
                   "preconditions가 null이면 출력되지 않아야 합니다.");
        assertTrue(!fileContent.contains("exclusions:"), 
                   "exclusions가 null이면 출력되지 않아야 합니다.");
        assertTrue(!fileContent.contains("recipeList:"), 
                   "recipeList가 null이면 출력되지 않아야 합니다.");
    }

    @Test
    void testSerializeOmitsEmptyLists(@TempDir Path tempDir) throws IOException {
        // empty list가 serialize 시 생략되는지 테스트
        File outputFile = tempDir.resolve("output-omit-empty.yml").toFile();

        RecipeDefinition recipe = new RecipeDefinition();
        recipe.setType("specs.openrewrite.org/v1beta/recipe");
        recipe.setName("com.example.TestRecipe");
        recipe.setDisplayName("Test Recipe");
        // empty list 설정
        recipe.setTags(new java.util.ArrayList<>());
        recipe.setPreconditions(new java.util.ArrayList<>());
        recipe.setExclusions(new java.util.ArrayList<>());
        recipe.setRecipeList(new java.util.ArrayList<>());

        List<RecipeDefinition> recipes = Arrays.asList(recipe);
        yamlParser.writeRecipes(recipes, outputFile);

        assertTrue(outputFile.exists());
        
        // 생성된 파일 내용 확인
        String fileContent = new String(java.nio.file.Files.readAllBytes(outputFile.toPath()));
        
        // 필수 필드와 설정된 필드는 포함되어야 함
        assertTrue(fileContent.contains("type:") && fileContent.contains("specs.openrewrite.org/v1beta/recipe"));
        assertTrue(fileContent.contains("name:") && fileContent.contains("com.example.TestRecipe"));
        assertTrue(fileContent.contains("displayName:") && fileContent.contains("Test Recipe"));
        
        // empty list는 생략되어야 함 (requirements.md: "값이 없으면 출력하지 않습니다")
        // Jackson의 기본 동작은 빈 배열을 출력할 수 있으므로, 실제로 생략되는지 확인
        assertTrue(!fileContent.contains("tags:"), 
                   "tags가 empty list이면 출력되지 않아야 합니다.");
        assertTrue(!fileContent.contains("preconditions:"), 
                   "preconditions가 empty list이면 출력되지 않아야 합니다.");
        assertTrue(!fileContent.contains("exclusions:"), 
                   "exclusions가 empty list이면 출력되지 않아야 합니다.");
        assertTrue(!fileContent.contains("recipeList:"), 
                   "recipeList가 empty list이면 출력되지 않아야 합니다.");
    }
}

