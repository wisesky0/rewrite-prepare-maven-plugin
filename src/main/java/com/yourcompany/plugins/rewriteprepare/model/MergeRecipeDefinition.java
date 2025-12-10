package com.yourcompany.plugins.rewriteprepare.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * org.yourcompany.openrewrite/v1/merge 타입의 Recipe Definition을 표현하는 모델 클래스
 * 
 * 이 타입은 recipeList 업데이트 규칙을 정의하며, 병합된 결과 파일에 포함되지 않습니다.
 * 각 Recipe Definition 파일에는 이 타입이 기본으로 포함되어 있으며,
 * 파싱 후 트리거 recipe(대상 recipe)의 recipeList에 자신의 레시피가 어떻게 호출되는지를 정의합니다.
 * 
 * YAML 형식:
 * ---
 * type: org.yourcompany.openrewrite/v1/merge
 * name: 마이그레이션 레시피 머지 규칙
 * rules:
 *   - updateRecipeList:
 *       name: com.example.Main
 *       updateOrder:
 *         - first:
 *           - com.example.Recipe1
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MergeRecipeDefinition {
    private static final String MERGE_TYPE = "org.yourcompany.openrewrite/v1/merge";
    
    @JsonProperty(required = true)
    private String type;
    
    private String name;
    
    @JsonProperty(required = true)
    private List<UpdateRecipeList> rules;

    public MergeRecipeDefinition() {
        this.type = MERGE_TYPE;
        this.rules = new ArrayList<>();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<UpdateRecipeList> getRules() {
        return rules;
    }

    public void setRules(List<UpdateRecipeList> rules) {
        this.rules = rules != null ? new ArrayList<>(rules) : new ArrayList<>();
    }

    /**
     * rules 필드를 설정합니다 (YAML 파싱 시 사용).
     * YAML에서 리스트로 파싱된 rules를 처리합니다.
     * 각 항목은 "updateRecipeList" 키를 가진 Map입니다.
     * 
     * @param rulesRaw 파싱된 원본 rules (List<Object>)
     */
    @JsonSetter("rules")
    @SuppressWarnings("unchecked")
    public void setRulesRaw(List<Object> rulesRaw) {
        if (rulesRaw == null) {
            this.rules = new ArrayList<>();
            return;
        }

        List<UpdateRecipeList> parsedRules = new ArrayList<>();
        for (Object item : rulesRaw) {
            if (item instanceof Map) {
                Map<String, Object> ruleMap = (Map<String, Object>) item;
                // "updateRecipeList" 키를 찾아서 UpdateRecipeList 객체로 변환
                if (ruleMap.containsKey("updateRecipeList")) {
                    Object updateRecipeListObj = ruleMap.get("updateRecipeList");
                    if (updateRecipeListObj instanceof Map) {
                        Map<String, Object> updateRecipeListMap = (Map<String, Object>) updateRecipeListObj;
                        UpdateRecipeList updateRecipeList = new UpdateRecipeList();
                        
                        // name 필드 설정
                        if (updateRecipeListMap.containsKey("name")) {
                            updateRecipeList.setName((String) updateRecipeListMap.get("name"));
                        }
                        
                        // updateOrder 필드 설정
                        if (updateRecipeListMap.containsKey("updateOrder")) {
                            Object updateOrderObj = updateRecipeListMap.get("updateOrder");
                            if (updateOrderObj instanceof List) {
                                updateRecipeList.setUpdateOrderRaw((List<Object>) updateOrderObj);
                            }
                        }
                        
                        parsedRules.add(updateRecipeList);
                    }
                }
            }
        }
        this.rules = parsedRules;
    }

    /**
     * 이 정의가 org.yourcompany.openrewrite/v1/merge 타입인지 확인합니다.
     */
    public boolean isMergeType() {
        return MERGE_TYPE.equals(this.type);
    }

    /**
     * RecipeDefinition에서 MergeRecipeDefinition으로 변환 가능한지 확인합니다.
     */
    public static boolean isMergeType(RecipeDefinition recipeDefinition) {
        return recipeDefinition != null && MERGE_TYPE.equals(recipeDefinition.getType());
    }
}

