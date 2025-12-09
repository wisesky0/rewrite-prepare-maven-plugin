package com.yourcompany.plugins.rewriteprepare.model;

import java.util.List;

/**
 * 머지 규칙 파일의 루트 모델 클래스
 * merge-rules.yml 파일의 전체 구조를 표현합니다.
 */
public class MergeRules {
    private List<Rule> rules;

    public List<Rule> getRules() {
        return rules;
    }

    public void setRules(List<Rule> rules) {
        this.rules = rules;
    }
}

