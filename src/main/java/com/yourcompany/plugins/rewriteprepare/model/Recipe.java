package com.yourcompany.plugins.rewriteprepare.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.yourcompany.plugins.rewriteprepare.serializer.UnquotedStringSerializer;

/**
 * OpenRewrite Recipe를 표현하는 모델 클래스
 * 
 * 각 Recipe는 name과 attributes를 가집니다.
 * YAML에서는 다음과 같이 표현됩니다:
 * 
 * - org.openrewrite.text.ChangeText:  <---> Recipe{name:"org.openrewrite.text.ChangeText", attributes: {"toText": 2}}
 *     toText: 2
 * - com.yourorg.RecipeB:  <---> Recipe{name:"com.yourorg.RecipeB", attributes: {"exampleConfig1": "foo","exampleConfig2": "bar"}}
 *     exampleConfig1: foo
 *     exampleConfig2: bar
 * - com.yourorg.RecipeC  <---> Recipe{name:"com.yourorg.RecipeC", attributes: {}}
 * 
 * 단순 문자열 형식(- com.yourorg.RecipeC)은 Recipe{name:"com.yourorg.RecipeC", attributes: {}}로 변환됩니다.
 */
public class Recipe {
    @JsonSerialize(using = UnquotedStringSerializer.class)
    private String name;
    private Map<String, Object> attributes;

    public Recipe() {
        this.attributes = new LinkedHashMap<>();
    }

    public Recipe(String name) {
        this.name = name;
        this.attributes = new LinkedHashMap<>();
    }

    public Recipe(String name, Map<String, Object> attributes) {
        this.name = name;
        this.attributes = attributes != null ? new LinkedHashMap<>(attributes) : new LinkedHashMap<>();
    }

    /**
     * YAML 파싱 시 사용되는 생성자
     * 단순 문자열이나 Map 형식을 Recipe로 변환합니다.
     * 
     * @param value 파싱된 원본 값 (String 또는 Map)
     * @return Recipe 객체
     */
    @JsonCreator
    public static Recipe fromValue(Object value) {
        Recipe recipe = new Recipe();
        recipe.setValue(value);
        return recipe;
    }

    /**
     * YAML 파싱 시 값을 설정합니다.
     * 
     * @param value 파싱된 원본 값 (String 또는 Map)
     */
    public void setValue(Object value) {
        if (value instanceof String) {
            // 단순 문자열인 경우: Recipe{name:"com.yourorg.RecipeC", attributes: {}}
            this.name = (String) value;
            this.attributes = new LinkedHashMap<>();
        } else if (value instanceof Map) {
            // Map 형태인 경우: {"org.openrewrite.text.ChangeText": {"toText": 2}}
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            if (map.size() == 1) {
                Map.Entry<String, Object> entry = map.entrySet().iterator().next();
                this.name = entry.getKey();
                Object configValue = entry.getValue();
                if (configValue instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> configMap = (Map<String, Object>) configValue;
                    this.attributes = new LinkedHashMap<>(configMap);
                } else {
                    this.attributes = new LinkedHashMap<>();
                }
            }
        }
    }

    /**
     * YAML 직렬화 시 사용되는 메서드
     * Recipe를 YAML 형식으로 변환합니다.
     * 
     * @return YAML에 맞는 형식 (String 또는 Map)
     */
    @JsonValue
    public Object toValue() {
        if (attributes == null || attributes.isEmpty()) {
            // 빈 attributes인 경우: 단순 문자열로 직렬화
            return name;
        } else {
            // attributes가 있는 경우: Map 형식으로 직렬화
            Map<String, Object> map = new LinkedHashMap<>();
            map.put(name, attributes);
            return map;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getAttributes() {
        return attributes != null ? attributes : Collections.emptyMap();
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes != null ? new LinkedHashMap<>(attributes) : new LinkedHashMap<>();
    }

    @Override
    public String toString() {
        return "Recipe{name=\"" + name + "\", attributes=" + attributes + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Recipe recipe = (Recipe) o;
        if (name == null) {
            return recipe.name == null;
        }
        return name.equals(recipe.name);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}

