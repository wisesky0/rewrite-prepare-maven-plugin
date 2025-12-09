package com.yourcompany.plugins.rewriteprepare.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * updateOrder의 각 항목을 표현하는 클래스
 * 
 * first/last의 경우: key는 null이고 values만 있음
 *   예: - com.example.Recipe1 -> UpdateEntry{key: null, values: ["com.example.Recipe1"]}
 * 
 * before/after의 경우: key는 대상 recipe 이름이고 values는 추가할 recipe 리스트
 *   예: - com.example.Recipe4: [com.example.Recipe5] -> UpdateEntry{key: "com.example.Recipe4", values: ["com.example.Recipe5"]}
 */
public class UpdateEntry {
    private String key;
    private List<String> values;

    public UpdateEntry() {
    }

    public UpdateEntry(String key, List<String> values) {
        this.key = key;
        this.values = values;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    /**
     * YAML 파싱 시 사용되는 setter
     * 단순 문자열이나 Map 형식을 UpdateEntry로 변환합니다.
     * 
     * @param rawValue 파싱된 원본 값 (String 또는 Map)
     */
    @com.fasterxml.jackson.annotation.JsonSetter
    public void setValue(Object rawValue) {
        if (rawValue instanceof String) {
            // 단순 문자열: first/last의 경우
            // 예: "com.example.Recipe1" -> UpdateEntry{key: null, values: ["com.example.Recipe1"]}
            this.key = null;
            this.values = new ArrayList<>();
            this.values.add((String) rawValue);
        } else if (rawValue instanceof Map) {
            // Map 형식: before/after의 경우
            // 예: {"com.example.Recipe4": ["com.example.Recipe5"]} -> UpdateEntry{key: "com.example.Recipe4", values: ["com.example.Recipe5"]}
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) rawValue;
            if (map.size() == 1) {
                Map.Entry<String, Object> entry = map.entrySet().iterator().next();
                this.key = entry.getKey();
                if (entry.getValue() instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> valueList = (List<String>) entry.getValue();
                    this.values = valueList;
                } else {
                    this.values = new ArrayList<>();
                }
            }
        } else if (rawValue instanceof List) {
            // List 형식: first/last에서 여러 항목이 있는 경우
            // 예: ["com.example.Recipe1", "com.example.Recipe2"]
            this.key = null;
            @SuppressWarnings("unchecked")
            List<String> valueList = (List<String>) rawValue;
            this.values = valueList;
        }
    }
}

