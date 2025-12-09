package com.yourcompany.plugins.rewriteprepare.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.yourcompany.plugins.rewriteprepare.serializer.UnquotedStringSerializer;

/**
 * OpenRewrite Recipe Definition을 표현하는 모델 클래스
 * YAML 파일의 각 recipe definition을 표현합니다.
 * 
 * recipeList는 List<Recipe> 형식으로 표현됩니다.
 * 각 Recipe는 name과 attributes를 가지며, YAML에서는 다음과 같이 표현됩니다:
 * 
 * - org.openrewrite.text.ChangeText:  <---> Recipe{name:"org.openrewrite.text.ChangeText", attributes: {"toText": 2}}
 *     toText: 2
 * - com.yourorg.RecipeB:  <---> Recipe{name:"com.yourorg.RecipeB", attributes: {"exampleConfig1": "foo","exampleConfig2": "bar"}}
 *     exampleConfig1: foo
 *     exampleConfig2: bar
 * - com.yourorg.RecipeC  <---> Recipe{name:"com.yourorg.RecipeC", attributes: {}}
 * 
 * 단순 문자열 형식(- com.yourorg.RecipeC)은 Recipe{name:"com.yourorg.RecipeC", attributes: {}}로 변환됩니다.
 * 
 * requirements.md에 따르면 optional 필드는 "값이 없으면 출력하지 않습니다"이므로,
 * null 값과 empty collection은 serialize 시 생략됩니다.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RecipeDefinition {
    @JsonSerialize(using = UnquotedStringSerializer.class)
    private String type;
    
    @JsonSerialize(using = UnquotedStringSerializer.class)
    private String name;
    
    @JsonSerialize(using = UnquotedStringSerializer.class)
    private String displayName;
    
    @JsonSerialize(using = UnquotedStringSerializer.class)
    private String description;
    
    private List<String> tags;
    
    @JsonSerialize(using = UnquotedStringSerializer.class)
    private String estimatedEffortPerOccurrence;
    
    private Boolean causesAnotherCycle;
    private List<Object> preconditions;
    private List<Object> exclusions;
    private List<Recipe> recipeList;

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

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getEstimatedEffortPerOccurrence() {
        return estimatedEffortPerOccurrence;
    }

    public void setEstimatedEffortPerOccurrence(String estimatedEffortPerOccurrence) {
        this.estimatedEffortPerOccurrence = estimatedEffortPerOccurrence;
    }

    public Boolean getCausesAnotherCycle() {
        return causesAnotherCycle;
    }

    public void setCausesAnotherCycle(Boolean causesAnotherCycle) {
        this.causesAnotherCycle = causesAnotherCycle;
    }

    public List<Object> getPreconditions() {
        return preconditions;
    }

    public void setPreconditions(List<Object> preconditions) {
        this.preconditions = preconditions;
    }

    public List<Object> getExclusions() {
        return exclusions;
    }

    public void setExclusions(List<Object> exclusions) {
        this.exclusions = exclusions;
    }

    public List<Recipe> getRecipeList() {
        return recipeList;
    }

    /**
     * recipeList를 설정합니다.
     * 
     * @param recipeList Recipe 리스트
     */
    public void setRecipeList(List<Recipe> recipeList) {
        this.recipeList = recipeList;
    }

    /**
     * recipeList를 설정합니다 (파싱 시 사용).
     * YAML에서 파싱된 recipeList를 Recipe 객체 리스트로 변환합니다.
     * 
     * 단순 문자열 형식(- com.yourorg.RecipeC)은 Recipe{name:"com.yourorg.RecipeC", attributes: {}}로 변환됩니다.
     * Map 형식(- org.openrewrite.text.ChangeText: {toText: 2})은 Recipe{name:"org.openrewrite.text.ChangeText", attributes: {"toText": 2}}로 변환됩니다.
     * 
     * @param recipeListRaw 파싱된 원본 recipeList (List<Object>)
     */
    @com.fasterxml.jackson.annotation.JsonSetter("recipeList")
    public void setRecipeListRaw(List<Object> recipeListRaw) {
        if (recipeListRaw == null) {
            this.recipeList = null;
            return;
        }

        List<Recipe> normalizedList = new ArrayList<>();
        
        for (Object item : recipeListRaw) {
            Recipe recipe = Recipe.fromValue(item);
            if (recipe.getName() != null) {
                normalizedList.add(recipe);
            }
        }
        
        this.recipeList = normalizedList;
    }
}

