package com.yourcompany.plugins.rewriteprepare.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yourcompany.plugins.rewriteprepare.model.Recipe;
import com.yourcompany.plugins.rewriteprepare.model.RecipeDefinition;
import com.yourcompany.plugins.rewriteprepare.model.UpdateEntry;
import com.yourcompany.plugins.rewriteprepare.model.UpdateRecipeList;

/**
 * RecipeDefinition의 recipeList를 업데이트하는 서비스 클래스
 * first, last, before, after 순서에 따라 recipe를 추가합니다.
 */
public class RecipeListUpdater {
    private static final Logger logger = LoggerFactory.getLogger(RecipeListUpdater.class);

    /**
     * 지정된 이름의 RecipeDefinition을 찾아 recipeList를 업데이트합니다.
     *
     * @param recipes 업데이트할 RecipeDefinition 리스트
     * @param updateRecipeList 업데이트 정보
     * @return 업데이트된 RecipeDefinition 리스트
     */
    public List<RecipeDefinition> updateRecipeList(
            List<RecipeDefinition> recipes, 
            UpdateRecipeList updateRecipeList) {
        
        // updateRecipeList와 name은 RewritePrepareMojo에서 이미 검증되었으므로,
        // 여기서는 null 체크만 수행합니다.
        if (updateRecipeList == null || updateRecipeList.getName() == null) {
            logger.warn("updateRecipeList 또는 name이 null입니다. 업데이트를 건너뜁니다.");
            return recipes;
        }

        String targetRecipeName = updateRecipeList.getName();
        RecipeDefinition targetRecipe = findRecipeByName(recipes, targetRecipeName);

        if (targetRecipe == null) {
            logger.warn("대상 recipe를 찾을 수 없습니다: {}", targetRecipeName);
            return recipes;
        }

        logger.info("RecipeList 업데이트 시작: {}", targetRecipeName);

        // recipeList가 없으면 생성
        if (targetRecipe.getRecipeList() == null) {
            targetRecipe.setRecipeList(new ArrayList<>());
        }

        UpdateRecipeList.UpdateOrder updateOrder = updateRecipeList.getUpdateOrder();
        if (updateOrder == null) {
            logger.warn("updateOrder가 null입니다. 업데이트를 건너뜁니다.");
            return recipes;
        }

        // first: 가장 앞에 추가
        if (updateOrder.getFirst() != null) {
            for (UpdateEntry entry : updateOrder.getFirst()) {
                if (entry.getValues() != null) {
                    addRecipesToFirst(targetRecipe, entry.getValues());
                }
            }
        }

        // last: 가장 마지막에 추가
        if (updateOrder.getLast() != null) {
            for (UpdateEntry entry : updateOrder.getLast()) {
                if (entry.getValues() != null) {
                    addRecipesToLast(targetRecipe, entry.getValues());
                }
            }
        }

        // before: 지정된 recipe 앞에 추가
        if (updateOrder.getBefore() != null) {
            for (UpdateEntry entry : updateOrder.getBefore()) {
                if (entry.getKey() != null && entry.getValues() != null) {
                    addRecipesBefore(targetRecipe, entry.getKey(), entry.getValues());
                }
            }
        }

        // after: 지정된 recipe 뒤에 추가
        if (updateOrder.getAfter() != null) {
            for (UpdateEntry entry : updateOrder.getAfter()) {
                if (entry.getKey() != null && entry.getValues() != null) {
                    addRecipesAfter(targetRecipe, entry.getKey(), entry.getValues());
                }
            }
        }

        logger.info("RecipeList 업데이트 완료: {}", targetRecipeName);
        return recipes;
    }

    /**
     * 이름으로 RecipeDefinition을 찾습니다.
     */
    private RecipeDefinition findRecipeByName(List<RecipeDefinition> recipes, String name) {
        return recipes.stream()
                .filter(r -> name.equals(r.getName()))
                .findFirst()
                .orElse(null);
    }

    /**
     * recipeList의 가장 앞에 recipe들을 추가합니다.
     */
    private void addRecipesToFirst(RecipeDefinition targetRecipe, List<String> recipeNames) {
        List<Recipe> recipeList = targetRecipe.getRecipeList();
        List<Recipe> newRecipes = new ArrayList<>();

        for (String recipeName : recipeNames) {
            if (isRecipeAlreadyInList(recipeList, recipeName)) {
                logger.warn("Recipe가 이미 recipeList에 존재합니다. 스킵: {}", recipeName);
                continue;
            }
            Recipe recipe = new Recipe(recipeName);
            newRecipes.add(recipe);
            logger.info("Recipe 추가 (first): {}", recipeName);
        }

        // 기존 리스트 앞에 새 recipe들 추가
        newRecipes.addAll(recipeList);
        targetRecipe.setRecipeList(newRecipes);
    }

    /**
     * recipeList의 가장 마지막에 recipe들을 추가합니다.
     */
    private void addRecipesToLast(RecipeDefinition targetRecipe, List<String> recipeNames) {
        List<Recipe> recipeList = targetRecipe.getRecipeList();

        for (String recipeName : recipeNames) {
            if (isRecipeAlreadyInList(recipeList, recipeName)) {
                logger.warn("Recipe가 이미 recipeList에 존재합니다. 스킵: {}", recipeName);
                continue;
            }
            Recipe recipe = new Recipe(recipeName);
            recipeList.add(recipe);
            logger.info("Recipe 추가 (last): {}", recipeName);
        }
    }

    /**
     * 지정된 recipe 이름 앞에 recipe들을 추가합니다.
     */
    private void addRecipesBefore(RecipeDefinition targetRecipe, String targetName, List<String> recipeNames) {
        List<Recipe> recipeList = targetRecipe.getRecipeList();
        int targetIndex = findRecipeIndex(recipeList, targetName);

        if (targetIndex == -1) {
            logger.warn("대상 recipe를 recipeList에서 찾을 수 없습니다. 스킵: {}", targetName);
            return;
        }

        // 역순으로 추가하여 순서 유지
        for (int i = recipeNames.size() - 1; i >= 0; i--) {
            String recipeName = recipeNames.get(i);
            if (isRecipeAlreadyInList(recipeList, recipeName)) {
                logger.warn("Recipe가 이미 recipeList에 존재합니다. 스킵: {}", recipeName);
                continue;
            }
            Recipe recipe = new Recipe(recipeName);
            recipeList.add(targetIndex, recipe);
            logger.info("Recipe 추가 (before {}): {}", targetName, recipeName);
        }
    }

    /**
     * 지정된 recipe 이름 뒤에 recipe들을 추가합니다.
     */
    private void addRecipesAfter(RecipeDefinition targetRecipe, String targetName, List<String> recipeNames) {
        List<Recipe> recipeList = targetRecipe.getRecipeList();
        int targetIndex = findRecipeIndex(recipeList, targetName);

        if (targetIndex == -1) {
            logger.warn("대상 recipe를 recipeList에서 찾을 수 없습니다. 스킵: {}", targetName);
            return;
        }

        // targetIndex 다음 위치에 추가
        int insertIndex = targetIndex + 1;
        for (String recipeName : recipeNames) {
            if (isRecipeAlreadyInList(recipeList, recipeName)) {
                logger.warn("Recipe가 이미 recipeList에 존재합니다. 스킵: {}", recipeName);
                continue;
            }
            Recipe recipe = new Recipe(recipeName);
            recipeList.add(insertIndex, recipe);
            logger.info("Recipe 추가 (after {}): {}", targetName, recipeName);
            insertIndex++;
        }
    }

    /**
     * recipeList에서 recipe 이름의 인덱스를 찾습니다.
     */
    private int findRecipeIndex(List<Recipe> recipeList, String recipeName) {
        for (int i = 0; i < recipeList.size(); i++) {
            Recipe recipe = recipeList.get(i);
            if (recipeName.equals(recipe.getName())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * recipeList에 이미 recipe가 있는지 확인합니다.
     */
    private boolean isRecipeAlreadyInList(List<Recipe> recipeList, String recipeName) {
        return findRecipeIndex(recipeList, recipeName) != -1;
    }
}

