package com.yourcompany.plugins.rewriteprepare.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;
import com.yourcompany.plugins.rewriteprepare.model.MergeRecipeDefinition;
import com.yourcompany.plugins.rewriteprepare.model.MergeRules;
import com.yourcompany.plugins.rewriteprepare.model.RecipeDefinition;

/**
 * YAML 파일을 파싱하는 서비스 클래스
 * merge-rules.yml과 recipe YAML 파일들을 파싱합니다.
 */
public class YamlParser {
    private static final Logger logger = LoggerFactory.getLogger(YamlParser.class);
    private final ObjectMapper yamlMapper;

    public YamlParser() {
        // YAMLFactory 설정: 문자열을 따옴표로 감싸지 않도록 설정
        // MINIMIZE_QUOTES: 가능한 경우 따옴표를 최소화
        YAMLFactory yamlFactory = new YAMLFactory();
        yamlFactory.configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true);
        this.yamlMapper = new ObjectMapper(yamlFactory);
        // @JsonProperty(required = true) 필드가 없을 때 예외 발생하도록 설정
        this.yamlMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, true);
    }

    /**
     * merge-rules.yml 파일을 파싱합니다.
     *
     * @param mergeRuleFile 머지 규칙 파일
     * @return 파싱된 MergeRules 객체
     * @throws IOException 파일 읽기 오류
     */
    public MergeRules parseMergeRules(File mergeRuleFile) throws IOException {
        logger.debug("머지 규칙 파일 파싱 시작: {}", mergeRuleFile.getAbsolutePath());
        MergeRules mergeRules = yamlMapper.readValue(mergeRuleFile, MergeRules.class);
        logger.debug("머지 규칙 파일 파싱 완료: {} 개의 규칙 발견", 
                     mergeRules.getRules() != null ? mergeRules.getRules().size() : 0);
        return mergeRules;
    }

    /**
     * 여러 개의 recipe definition이 포함된 YAML 파일을 파싱합니다.
     * 문서 구분자 '---'로 분리된 각 recipe definition을 파싱합니다.
     * Jackson YAMLFactory의 YAMLParser를 사용하여 다중 문서를 처리합니다.
     * 
     * org.yourcompany.openrewrite/v1/merge 타입은 MergeRecipeDefinition으로 파싱하고,
     * 그 외의 타입은 RecipeDefinition으로 파싱합니다.
     *
     * @param recipeFile recipe YAML 파일
     * @return 파싱된 RecipeDefinition 리스트 (MergeRecipeDefinition은 제외)
     * @throws IOException 파일 읽기 오류
     */
    public List<RecipeDefinition> parseRecipeFile(File recipeFile) throws IOException {
        logger.debug("Recipe 파일 파싱 시작: {}", recipeFile.getAbsolutePath());
        List<RecipeDefinition> recipes = new ArrayList<>();
        List<MergeRecipeDefinition> mergeDefinitions = new ArrayList<>();

        YAMLFactory yamlFactory = new YAMLFactory();
        
        try (YAMLParser parser = yamlFactory.createParser(recipeFile)) {
            // YAML 파일의 각 문서를 순회
            while (parser.nextToken() != null) {
                try {
                    // 먼저 타입을 확인하기 위해 Map으로 파싱
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> rawDoc = yamlMapper.readValue(parser, java.util.Map.class);
                    
                    if (rawDoc != null) {
                        String type = (String) rawDoc.get("type");
                        
                        // org.yourcompany.openrewrite/v1/merge 타입인지 확인
                        if ("org.yourcompany.openrewrite/v1/merge".equals(type)) {
                            // MergeRecipeDefinition으로 파싱
                            MergeRecipeDefinition mergeDef = yamlMapper.convertValue(rawDoc, MergeRecipeDefinition.class);
                            if (mergeDef != null) {
                                mergeDefinitions.add(mergeDef);
                                logger.debug("MergeRecipeDefinition 파싱 완료: {}", mergeDef.getName());
                            }
                        } else {
                            // RecipeDefinition으로 파싱
                            RecipeDefinition recipe = yamlMapper.convertValue(rawDoc, RecipeDefinition.class);
                            if (recipe != null && recipe.getName() != null) {
                                recipes.add(recipe);
                                logger.debug("Recipe 파싱 완료: {}", recipe.getName());
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Recipe 파싱 중 오류 발생, 다음 문서로 진행: {}", e.getMessage());
                    // 오류가 발생해도 다음 문서로 계속 진행
                }
            }
        }

        logger.debug("Recipe 파일 파싱 완료: {} 개의 recipe, {} 개의 merge definition 발견", 
                     recipes.size(), mergeDefinitions.size());
        
        // MergeRecipeDefinition은 별도로 관리되므로 반환하지 않음
        // (나중에 RecipeMerger나 RewritePrepareMojo에서 처리)
        return recipes;
    }
    
    /**
     * 여러 개의 recipe definition이 포함된 YAML 파일을 파싱하고,
     * MergeRecipeDefinition과 RecipeDefinition을 분리하여 반환합니다.
     *
     * @param recipeFile recipe YAML 파일
     * @return 파싱 결과를 담은 RecipeParseResult 객체
     * @throws IOException 파일 읽기 오류
     */
    public RecipeParseResult parseRecipeFileWithMerge(File recipeFile) throws IOException {
        logger.debug("Recipe 파일 파싱 시작 (Merge 포함): {}", recipeFile.getAbsolutePath());
        List<RecipeDefinition> recipes = new ArrayList<>();
        List<MergeRecipeDefinition> mergeDefinitions = new ArrayList<>();

        YAMLFactory yamlFactory = new YAMLFactory();
        
        try (YAMLParser parser = yamlFactory.createParser(recipeFile)) {
            // YAML 파일의 각 문서를 순회
            while (parser.nextToken() != null) {
                try {
                    // 먼저 타입을 확인하기 위해 Map으로 파싱
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> rawDoc = yamlMapper.readValue(parser, java.util.Map.class);
                    
                    if (rawDoc != null) {
                        String type = (String) rawDoc.get("type");
                        
                        // org.yourcompany.openrewrite/v1/merge 타입인지 확인
                        if ("org.yourcompany.openrewrite/v1/merge".equals(type)) {
                            // MergeRecipeDefinition으로 파싱
                            MergeRecipeDefinition mergeDef = yamlMapper.convertValue(rawDoc, MergeRecipeDefinition.class);
                            if (mergeDef != null) {
                                mergeDefinitions.add(mergeDef);
                                logger.debug("MergeRecipeDefinition 파싱 완료: {}", mergeDef.getName());
                            }
                        } else {
                            // RecipeDefinition으로 파싱
                            RecipeDefinition recipe = yamlMapper.convertValue(rawDoc, RecipeDefinition.class);
                            if (recipe != null && recipe.getName() != null) {
                                recipes.add(recipe);
                                logger.debug("Recipe 파싱 완료: {}", recipe.getName());
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Recipe 파싱 중 오류 발생, 다음 문서로 진행: {}", e.getMessage());
                    // 오류가 발생해도 다음 문서로 계속 진행
                }
            }
        }

        logger.debug("Recipe 파일 파싱 완료: {} 개의 recipe, {} 개의 merge definition 발견", 
                     recipes.size(), mergeDefinitions.size());
        
        return new RecipeParseResult(recipes, mergeDefinitions);
    }
    
    /**
     * Recipe 파일 파싱 결과를 담는 클래스
     */
    public static class RecipeParseResult {
        private final List<RecipeDefinition> recipes;
        private final List<MergeRecipeDefinition> mergeDefinitions;
        
        public RecipeParseResult(List<RecipeDefinition> recipes, List<MergeRecipeDefinition> mergeDefinitions) {
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

    /**
     * RecipeDefinition 리스트를 YAML 파일로 저장합니다.
     * Jackson의 SequenceWriter를 사용하여 각 recipe를 '---' 구분자로 자동 분리하여 작성합니다.
     *
     * @param recipes 저장할 RecipeDefinition 리스트
     * @param outputFile 출력 파일
     * @throws IOException 파일 쓰기 오류
     */
    public void writeRecipes(List<RecipeDefinition> recipes, File outputFile) throws IOException {
        logger.debug("Recipe 파일 쓰기 시작: {} 개의 recipe", recipes.size());
        
        // 출력 디렉토리 생성
        if (outputFile.getParentFile() != null) {
            outputFile.getParentFile().mkdirs();
        }

        // 파일이 존재하면 삭제
        if (outputFile.exists()) {
            outputFile.delete();
        }

        // SequenceWriter를 사용하여 여러 문서를 자동으로 '---' 구분자와 함께 작성
        try (SequenceWriter writer = yamlMapper.writer().writeValues(outputFile)) {
            for (RecipeDefinition recipe : recipes) {
                writer.write(recipe);
                logger.debug("Recipe 작성 완료: {}", recipe.getName());
            }
        }

        logger.info("Recipe 파일 쓰기 완료: {}", outputFile.getAbsolutePath());
    }
}

