package com.yourcompany.plugins.rewriteprepare.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.yourcompany.plugins.rewriteprepare.model.Recipe;
import com.yourcompany.plugins.rewriteprepare.model.RecipeDefinition;
import com.yourcompany.plugins.rewriteprepare.model.UpdateEntry;
import com.yourcompany.plugins.rewriteprepare.model.UpdateRecipeList;

/**
 * RecipeListUpdater 클래스의 테스트
 * RecipeList 업데이트 기능을 테스트합니다.
 */
class RecipeListUpdaterTest {

    private RecipeListUpdater recipeListUpdater;

    @BeforeEach
    void setUp() {
        recipeListUpdater = new RecipeListUpdater();
    }

    /**
     * 문자열 리스트를 Recipe 리스트로 변환합니다.
     */
    private List<Recipe> createRecipeListFromStrings(String... recipeNames) {
        List<Recipe> recipeList = new ArrayList<>();
        for (String recipeName : recipeNames) {
            recipeList.add(new Recipe(recipeName));
        }
        return recipeList;
    }

    /**
     * recipeList에서 특정 인덱스의 recipe 이름을 반환합니다.
     */
    private String getRecipeNameAt(List<Recipe> recipeList, int index) {
        return recipeList.get(index).getName();
    }

    @Test
    void testUpdateRecipeListWithFirst() {
        RecipeDefinition targetRecipe = createTestRecipe("com.example.Main");
        targetRecipe.setRecipeList(createRecipeListFromStrings("com.example.Recipe1", "com.example.Recipe2"));

        UpdateRecipeList updateRecipeList = new UpdateRecipeList();
        updateRecipeList.setName("com.example.Main");
        UpdateRecipeList.UpdateOrder updateOrder = new UpdateRecipeList.UpdateOrder();

        UpdateEntry firstEntry = new UpdateEntry();
        firstEntry.setValue("com.example.Recipe0");
        updateOrder.setFirst(Arrays.asList(firstEntry));
        updateRecipeList.setUpdateOrder(updateOrder);

        List<RecipeDefinition> recipes = Arrays.asList(targetRecipe);
        List<RecipeDefinition> updated = recipeListUpdater.updateRecipeList(recipes, updateRecipeList);

        assertNotNull(updated);
        assertEquals(1, updated.size());
        List<Recipe> recipeList = updated.get(0).getRecipeList();
        assertEquals(3, recipeList.size());
        assertEquals("com.example.Recipe0", recipeList.get(0).getName());
    }

    @Test
    void testUpdateRecipeListWithLast() {
        RecipeDefinition targetRecipe = createTestRecipe("com.example.Main");
        targetRecipe.setRecipeList(createRecipeListFromStrings("com.example.Recipe1", "com.example.Recipe2"));

        UpdateRecipeList updateRecipeList = new UpdateRecipeList();
        updateRecipeList.setName("com.example.Main");
        UpdateRecipeList.UpdateOrder updateOrder = new UpdateRecipeList.UpdateOrder();

        UpdateEntry lastEntry = new UpdateEntry();
        lastEntry.setValue("com.example.Recipe3");
        updateOrder.setLast(Arrays.asList(lastEntry));
        updateRecipeList.setUpdateOrder(updateOrder);

        List<RecipeDefinition> recipes = Arrays.asList(targetRecipe);
        List<RecipeDefinition> updated = recipeListUpdater.updateRecipeList(recipes, updateRecipeList);

        assertNotNull(updated);
        List<Recipe> recipeList = updated.get(0).getRecipeList();
        assertEquals(3, recipeList.size());
        assertEquals("com.example.Recipe3", getRecipeNameAt(recipeList, 2));
    }

    @Test
    void testUpdateRecipeListWithBefore() {
        RecipeDefinition targetRecipe = createTestRecipe("com.example.Main");
        targetRecipe.setRecipeList(createRecipeListFromStrings("com.example.Recipe1", "com.example.Recipe2"));

        UpdateRecipeList updateRecipeList = new UpdateRecipeList();
        updateRecipeList.setName("com.example.Main");
        UpdateRecipeList.UpdateOrder updateOrder = new UpdateRecipeList.UpdateOrder();

        UpdateEntry beforeEntry = new UpdateEntry();
        Map<String, Object> beforeValue = new LinkedHashMap<>();
        beforeValue.put("com.example.Recipe2", Arrays.asList("com.example.Recipe1.5"));
        beforeEntry.setValue(beforeValue);
        updateOrder.setBefore(Arrays.asList(beforeEntry));
        updateRecipeList.setUpdateOrder(updateOrder);

        List<RecipeDefinition> recipes = Arrays.asList(targetRecipe);
        List<RecipeDefinition> updated = recipeListUpdater.updateRecipeList(recipes, updateRecipeList);

        assertNotNull(updated);
        List<Recipe> recipeList = updated.get(0).getRecipeList();
        assertEquals(3, recipeList.size());
        assertEquals("com.example.Recipe1.5", getRecipeNameAt(recipeList, 1));
        assertEquals("com.example.Recipe2", getRecipeNameAt(recipeList, 2));
    }

    @Test
    void testUpdateRecipeListWithAfter() {
        RecipeDefinition targetRecipe = createTestRecipe("com.example.Main");
        targetRecipe.setRecipeList(createRecipeListFromStrings("com.example.Recipe1", "com.example.Recipe2"));

        UpdateRecipeList updateRecipeList = new UpdateRecipeList();
        updateRecipeList.setName("com.example.Main");
        UpdateRecipeList.UpdateOrder updateOrder = new UpdateRecipeList.UpdateOrder();

        UpdateEntry afterEntry = new UpdateEntry();
        Map<String, Object> afterValue = new LinkedHashMap<>();
        afterValue.put("com.example.Recipe1", Arrays.asList("com.example.Recipe1.5"));
        afterEntry.setValue(afterValue);
        updateOrder.setAfter(Arrays.asList(afterEntry));
        updateRecipeList.setUpdateOrder(updateOrder);

        List<RecipeDefinition> recipes = Arrays.asList(targetRecipe);
        List<RecipeDefinition> updated = recipeListUpdater.updateRecipeList(recipes, updateRecipeList);

        assertNotNull(updated);
        List<Recipe> recipeList = updated.get(0).getRecipeList();
        assertEquals(3, recipeList.size());
        assertEquals("com.example.Recipe1", getRecipeNameAt(recipeList, 0));
        assertEquals("com.example.Recipe1.5", getRecipeNameAt(recipeList, 1));
        assertEquals("com.example.Recipe2", getRecipeNameAt(recipeList, 2));
    }

    @Test
    void testUpdateRecipeListWithDuplicateRecipe() {
        RecipeDefinition targetRecipe = createTestRecipe("com.example.Main");
        targetRecipe.setRecipeList(createRecipeListFromStrings("com.example.Recipe1", "com.example.Recipe2"));

        UpdateRecipeList updateRecipeList = new UpdateRecipeList();
        updateRecipeList.setName("com.example.Main");
        UpdateRecipeList.UpdateOrder updateOrder = new UpdateRecipeList.UpdateOrder();

        UpdateEntry firstEntry = new UpdateEntry();
        firstEntry.setValue("com.example.Recipe1");
        updateOrder.setFirst(Arrays.asList(firstEntry));
        updateRecipeList.setUpdateOrder(updateOrder);

        List<RecipeDefinition> recipes = Arrays.asList(targetRecipe);
        List<RecipeDefinition> updated = recipeListUpdater.updateRecipeList(recipes, updateRecipeList);

        assertNotNull(updated);
        // 중복은 skip되므로 크기는 변하지 않음
        List<Recipe> recipeList = updated.get(0).getRecipeList();
        assertEquals(2, recipeList.size());
    }

    @Test
    void testUpdateRecipeListWithNonExistentTarget() {
        RecipeDefinition targetRecipe = createTestRecipe("com.example.Other");
        targetRecipe.setRecipeList(createRecipeListFromStrings("com.example.Recipe1"));

        UpdateRecipeList updateRecipeList = new UpdateRecipeList();
        updateRecipeList.setName("com.example.Main"); // 존재하지 않는 이름
        UpdateRecipeList.UpdateOrder updateOrder = new UpdateRecipeList.UpdateOrder();

        UpdateEntry firstEntry = new UpdateEntry();
        firstEntry.setValue("com.example.Recipe0");
        updateOrder.setFirst(Arrays.asList(firstEntry));
        updateRecipeList.setUpdateOrder(updateOrder);

        List<RecipeDefinition> recipes = Arrays.asList(targetRecipe);
        List<RecipeDefinition> updated = recipeListUpdater.updateRecipeList(recipes, updateRecipeList);

        assertNotNull(updated);
        // 대상 recipe가 없으므로 변경되지 않음
        assertEquals(1, updated.get(0).getRecipeList().size());
    }

    private RecipeDefinition createTestRecipe(String name) {
        RecipeDefinition recipe = new RecipeDefinition();
        recipe.setType("specs.openrewrite.org/v1beta/recipe");
        recipe.setName(name);
        recipe.setDisplayName(name);
        return recipe;
    }
}

