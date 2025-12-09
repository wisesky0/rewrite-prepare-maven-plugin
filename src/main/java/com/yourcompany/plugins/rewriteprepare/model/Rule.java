package com.yourcompany.plugins.rewriteprepare.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 머지 규칙을 표현하는 모델 클래스
 * artifactId와 groupId 패턴 매칭 및 mergeFiles, updateRecipeList 정보를 포함합니다.
 * 
 * requirements.md에 따라 다음 필드들이 필수입니다:
 * - artifactId: 필수, 없으면 오류 발생
 * - mergeFiles: 필수, 없거나 비어 있으면 오류 발생
 */
public class Rule {
    @JsonProperty(required = true)
    private String artifactId;
    
    private String groupId;
    
    @JsonProperty(required = true)
    private List<String> mergeFiles;
    
    private UpdateRecipeList updateRecipeList;

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public List<String> getMergeFiles() {
        return mergeFiles;
    }

    public void setMergeFiles(List<String> mergeFiles) {
        this.mergeFiles = mergeFiles;
    }

    public UpdateRecipeList getUpdateRecipeList() {
        return updateRecipeList;
    }

    public void setUpdateRecipeList(UpdateRecipeList updateRecipeList) {
        this.updateRecipeList = updateRecipeList;
    }
}

