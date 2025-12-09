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
     * @param recipeFile recipe YAML 파일
     * @return 파싱된 RecipeDefinition 리스트
     * @throws IOException 파일 읽기 오류
     */
    public List<RecipeDefinition> parseRecipeFile(File recipeFile) throws IOException {
        logger.debug("Recipe 파일 파싱 시작: {}", recipeFile.getAbsolutePath());
        List<RecipeDefinition> recipes = new ArrayList<>();

        YAMLFactory yamlFactory = new YAMLFactory();
        
        try (YAMLParser parser = yamlFactory.createParser(recipeFile)) {
            // YAML 파일의 각 문서를 순회
            while (parser.nextToken() != null) {
                try {
                    // 각 문서를 RecipeDefinition으로 파싱
                    // setRecipeListRaw가 자동으로 정규화를 수행합니다.
                    RecipeDefinition recipe = yamlMapper.readValue(parser, RecipeDefinition.class);
                    if (recipe != null && recipe.getName() != null) {
                        recipes.add(recipe);
                        logger.debug("Recipe 파싱 완료: {}", recipe.getName());
                    }
                } catch (Exception e) {
                    logger.warn("Recipe 파싱 중 오류 발생, 다음 문서로 진행: {}", e.getMessage());
                    // 오류가 발생해도 다음 문서로 계속 진행
                }
            }
        }

        logger.debug("Recipe 파일 파싱 완료: {} 개의 recipe 발견", recipes.size());
        return recipes;
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

