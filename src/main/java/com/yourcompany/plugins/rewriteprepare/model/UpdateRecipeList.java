package com.yourcompany.plugins.rewriteprepare.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * recipeList를 업데이트하기 위한 정보를 담는 모델 클래스
 * 대상 레시피 이름과 업데이트 순서(first, last, before, after)를 포함합니다.
 * 
 * requirements.md에 따라 다음 필드가 필수입니다:
 * - name: 필수, 없거나 비어 있으면 오류 발생
 */
public class UpdateRecipeList {
    @JsonProperty(required = true)
    private String name;
    
    private UpdateOrder updateOrder;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UpdateOrder getUpdateOrder() {
        return updateOrder;
    }

    public void setUpdateOrder(UpdateOrder updateOrder) {
        this.updateOrder = updateOrder;
    }

    /**
     * updateOrder를 설정합니다 (YAML 파싱 시 사용).
     * YAML에서 리스트로 파싱된 updateOrder를 UpdateOrder 객체로 변환합니다.
     * 
     * @param updateOrderRaw 파싱된 원본 updateOrder (List<Object>)
     */
    @com.fasterxml.jackson.annotation.JsonSetter("updateOrder")
    public void setUpdateOrderRaw(List<Object> updateOrderRaw) {
        if (updateOrderRaw == null) {
            this.updateOrder = null;
            return;
        }

        UpdateOrder order = new UpdateOrder();
        order.setUpdateOrderRaw(updateOrderRaw);
        this.updateOrder = order;
    }

    /**
     * 업데이트 순서를 표현하는 내부 클래스
     * YAML 구조: updateOrder는 리스트이며, 각 항목은 "first", "last", "before", "after" 키를 가진 Map입니다.
     * 각 키의 값은 List<UpdateEntry> 형식입니다.
     * 
     * YAML 예시:
     * updateOrder:
     *   - first:
     *     - com.example.Recipe1
     *   - last:
     *     - com.example.Recipe10
     *   - before:
     *     - com.example.Recipe4:
     *       - com.example.Recipe5
     */
    public static class UpdateOrder {
        private List<UpdateEntry> first;
        private List<UpdateEntry> last;
        private List<UpdateEntry> before;
        private List<UpdateEntry> after;

        /**
         * updateOrder를 설정합니다.
         * YAML에서 파싱된 리스트를 받아서 각 항목을 처리합니다.
         * 
         * @param updateOrderRaw 파싱된 원본 updateOrder (List<Object>)
         */
        public void setUpdateOrderRaw(List<Object> updateOrderRaw) {
            if (updateOrderRaw == null) {
                return;
            }

            for (Object item : updateOrderRaw) {
                if (item instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> orderMap = (Map<String, Object>) item;
                    
                    // first 처리
                    if (orderMap.containsKey("first")) {
                        this.first = parseUpdateEntryList(orderMap.get("first"));
                    }
                    // last 처리
                    if (orderMap.containsKey("last")) {
                        this.last = parseUpdateEntryList(orderMap.get("last"));
                    }
                    // before 처리
                    if (orderMap.containsKey("before")) {
                        this.before = parseUpdateEntryList(orderMap.get("before"));
                    }
                    // after 처리
                    if (orderMap.containsKey("after")) {
                        this.after = parseUpdateEntryList(orderMap.get("after"));
                    }
                }
            }
        }

        /**
         * YAML에서 파싱된 값을 List<UpdateEntry>로 변환합니다.
         * 
         * @param rawValue 파싱된 원본 값 (List<Object>)
         * @return List<UpdateEntry>
         */
        private List<UpdateEntry> parseUpdateEntryList(Object rawValue) {
            List<UpdateEntry> result = new ArrayList<>();
            
            if (rawValue instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> rawList = (List<Object>) rawValue;
                
                for (Object item : rawList) {
                    UpdateEntry entry = new UpdateEntry();
                    entry.setValue(item);
                    result.add(entry);
                }
            }
            
            return result;
        }

        public List<UpdateEntry> getFirst() {
            return first;
        }

        public void setFirst(List<UpdateEntry> first) {
            this.first = first;
        }

        public List<UpdateEntry> getLast() {
            return last;
        }

        public void setLast(List<UpdateEntry> last) {
            this.last = last;
        }

        public List<UpdateEntry> getBefore() {
            return before;
        }

        public void setBefore(List<UpdateEntry> before) {
            this.before = before;
        }

        public List<UpdateEntry> getAfter() {
            return after;
        }

        public void setAfter(List<UpdateEntry> after) {
            this.after = after;
        }
    }
}

