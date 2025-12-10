package com.yourcompany.plugins.rewriteprepare.service;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yourcompany.plugins.rewriteprepare.model.MergeRecipeDefinition;
import com.yourcompany.plugins.rewriteprepare.model.RecipeDefinition;

/**
 * Recipe 파일들을 병합하는 서비스 클래스
 * 여러 recipe YAML 파일을 하나로 병합합니다.
 */
public class RecipeMerger {
    private static final Logger logger = LoggerFactory.getLogger(RecipeMerger.class);
    private final YamlParser yamlParser;

    public RecipeMerger(YamlParser yamlParser) {
        this.yamlParser = yamlParser;
    }

    /**
     * 여러 recipe 파일을 병합합니다.
     * 중복된 파일은 skip합니다.
     *
     * @param recipeDirectory recipe 파일들이 있는 디렉토리
     * @param mergeFiles 병합할 파일 목록 (상대 경로)
     * @return 병합된 RecipeDefinition 리스트
     * @throws Exception 파일 읽기 오류
     */
    public List<RecipeDefinition> mergeFiles(File recipeDirectory, List<String> mergeFiles) throws Exception {
        logger.info("Recipe 파일 병합 시작: {} 개의 파일", mergeFiles.size());
        
        List<RecipeDefinition> mergedRecipes = new ArrayList<>();
        Map<String, Boolean> processedFiles = new LinkedHashMap<>();

        for (String mergeFile : mergeFiles) {
            File file = new File(recipeDirectory, mergeFile);
            
            if (processedFiles.containsKey(file.getAbsolutePath())) {
                logger.info("파일 스킵 (중복): {}", mergeFile);
                continue;
            }

            if (!file.exists()) {
                logger.warn("파일이 존재하지 않음: {}", mergeFile);
                continue;
            }

            try {
                List<RecipeDefinition> recipes = yamlParser.parseRecipeFile(file);
                mergedRecipes.addAll(recipes);
                processedFiles.put(file.getAbsolutePath(), true);
                logger.info("파일 병합 완료: {} ({} 개의 recipe)", mergeFile, recipes.size());
            } catch (Exception e) {
                logger.error("파일 병합 중 오류 발생: {}", mergeFile, e);
                throw e;
            }
        }

        logger.info("Recipe 파일 병합 완료: 총 {} 개의 recipe", mergedRecipes.size());
        return mergedRecipes;
    }
    
    /**
     * 여러 recipe 파일을 병합하고 MergeRecipeDefinition도 함께 반환합니다.
     * 중복된 파일은 skip합니다.
     *
     * @param recipeDirectory recipe 파일들이 있는 디렉토리
     * @param mergeFiles 병합할 파일 목록 (상대 경로)
     * @return 병합 결과를 담은 MergeResult 객체
     * @throws Exception 파일 읽기 오류
     */
    public MergeResult mergeFilesWithMerge(File recipeDirectory, List<String> mergeFiles) throws Exception {
        logger.info("Recipe 파일 병합 시작 (Merge 포함): {} 개의 파일", mergeFiles.size());
        
        List<RecipeDefinition> mergedRecipes = new ArrayList<>();
        List<MergeRecipeDefinition> mergedMergeDefinitions = new ArrayList<>();
        Map<String, Boolean> processedFiles = new LinkedHashMap<>();

        for (String mergeFile : mergeFiles) {
            File file = new File(recipeDirectory, mergeFile);
            
            if (processedFiles.containsKey(file.getAbsolutePath())) {
                logger.info("파일 스킵 (중복): {}", mergeFile);
                continue;
            }

            if (!file.exists()) {
                logger.warn("파일이 존재하지 않음: {}", mergeFile);
                continue;
            }

            try {
                YamlParser.RecipeParseResult result = yamlParser.parseRecipeFileWithMerge(file);
                mergedRecipes.addAll(result.getRecipes());
                mergedMergeDefinitions.addAll(result.getMergeDefinitions());
                processedFiles.put(file.getAbsolutePath(), true);
                logger.info("파일 병합 완료: {} ({} 개의 recipe, {} 개의 merge definition)", 
                           mergeFile, result.getRecipes().size(), result.getMergeDefinitions().size());
            } catch (Exception e) {
                logger.error("파일 병합 중 오류 발생: {}", mergeFile, e);
                throw e;
            }
        }

        logger.info("Recipe 파일 병합 완료: 총 {} 개의 recipe, {} 개의 merge definition", 
                   mergedRecipes.size(), mergedMergeDefinitions.size());
        return new MergeResult(mergedRecipes, mergedMergeDefinitions);
    }
    
    /**
     * 병합 결과를 담는 클래스
     */
    public static class MergeResult {
        private final List<RecipeDefinition> recipes;
        private final List<MergeRecipeDefinition> mergeDefinitions;
        
        public MergeResult(List<RecipeDefinition> recipes, List<MergeRecipeDefinition> mergeDefinitions) {
            this.recipes = recipes != null ? new ArrayList<>(recipes) : new ArrayList<>();
            this.mergeDefinitions = mergeDefinitions != null ? new ArrayList<>(mergeDefinitions) : new ArrayList<>();
        }
        
        public List<RecipeDefinition> getRecipes() {
            return recipes;
        }
        
        public List<MergeRecipeDefinition> getMergeDefinitions() {
            return mergeDefinitions;
        }
    }
}

