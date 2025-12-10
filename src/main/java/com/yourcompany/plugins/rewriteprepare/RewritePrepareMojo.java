package com.yourcompany.plugins.rewriteprepare;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yourcompany.plugins.rewriteprepare.model.MergeRecipeDefinition;
import com.yourcompany.plugins.rewriteprepare.model.MergeRules;
import com.yourcompany.plugins.rewriteprepare.model.RecipeDefinition;
import com.yourcompany.plugins.rewriteprepare.model.Rule;
import com.yourcompany.plugins.rewriteprepare.service.PatternMatcher;
import com.yourcompany.plugins.rewriteprepare.service.RecipeListUpdater;
import com.yourcompany.plugins.rewriteprepare.service.RecipeMerger;
import com.yourcompany.plugins.rewriteprepare.service.YamlParser;

/**
 * Rewrite Prepare Maven Plugin의 메인 Mojo 클래스
 * 
 * 이 플러그인은 recipeDirectory에 있는 OpenRewrite recipe YAML 파일들을 찾아 병합하고,
 * 지정한 이름으로 레시피 정의를 찾아 recipeList 속성의 값을 편집합니다.
 * 
 * 처리 순서:
 * 1. merge-rules.yml 파일을 읽어서 현재 프로젝트의 groupId, artifactId와 매칭되는 규칙을 찾습니다.
 * 2. 매칭된 규칙들의 mergeFiles를 병합합니다.
 * 3. 매칭된 규칙들의 updateRecipeList를 순서대로 수행합니다.
 * 4. 최종 결과를 outputFile에 저장합니다.
 */
@Mojo(name = "prepare", defaultPhase = org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_RESOURCES)
public class RewritePrepareMojo extends AbstractMojo {
    private static final Logger logger = LoggerFactory.getLogger(RewritePrepareMojo.class);

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * recipe 파일들이 있는 디렉토리
     * 기본값: ${project.basedir}
     * CLI 변수명: rewrite-prepare.recipeDirectory
     */
    @Parameter(property = "rewrite-prepare.recipeDirectory", defaultValue = "${project.basedir}")
    private File recipeDirectory;

    /**
     * 머지 규칙 파일 경로
     * 기본값: merge-rules.yml
     * CLI 변수명: rewrite-prepare.mergeRuleFile
     */
    @Parameter(property = "rewrite-prepare.mergeRuleFile", defaultValue = "merge-rules.yml")
    private File mergeRuleFile;

    /**
     * 출력 파일 경로
     * 기본값: openrewrite/rewrite.yml
     * CLI 변수명: rewrite-prepare.outputFile
     */
    @Parameter(property = "rewrite-prepare.outputFile", defaultValue = "openrewrite/rewrite.yml")
    private File outputFile;

    /**
     * 프로젝트 groupId (기본값: project.groupId)
     * CLI 변수명: rewrite-prepare.groupId
     */
    @Parameter(property = "rewrite-prepare.groupId")
    private String groupId;

    /**
     * 프로젝트 artifactId (기본값: project.artifactId)
     * CLI 변수명: rewrite-prepare.artifactId
     */
    @Parameter(property = "rewrite-prepare.artifactId")
    private String artifactId;

    private final YamlParser yamlParser;
    private final RecipeMerger recipeMerger;
    private final RecipeListUpdater recipeListUpdater;

    public RewritePrepareMojo() {
        this.yamlParser = new YamlParser();
        this.recipeMerger = new RecipeMerger(yamlParser);
        this.recipeListUpdater = new RecipeListUpdater();
    }

    @Override
    public void execute() throws MojoExecutionException {
        try {
            logger.info("Rewrite Prepare Maven Plugin 실행 시작");

            // 기본값 설정
            if (groupId == null || groupId.isEmpty()) {
                groupId = project.getGroupId();
            }
            if (artifactId == null || artifactId.isEmpty()) {
                artifactId = project.getArtifactId();
            }

            // 상대 경로를 절대 경로로 변환
            if (!mergeRuleFile.isAbsolute()) {
                mergeRuleFile = new File(project.getBasedir(), mergeRuleFile.getPath());
            }
            if (!outputFile.isAbsolute()) {
                outputFile = new File(project.getBasedir(), outputFile.getPath());
            }
            if (!recipeDirectory.isAbsolute()) {
                recipeDirectory = new File(project.getBasedir(), recipeDirectory.getPath());
            }

            logger.info("설정 정보:");
            logger.info("  recipeDirectory: {}", recipeDirectory.getAbsolutePath());
            logger.info("  mergeRuleFile: {}", mergeRuleFile.getAbsolutePath());
            logger.info("  outputFile: {}", outputFile.getAbsolutePath());
            logger.info("  groupId: {}", groupId);
            logger.info("  artifactId: {}", artifactId);

            // merge-rules.yml 파일 읽기
            if (!mergeRuleFile.exists()) {
                throw new MojoExecutionException("머지 규칙 파일이 존재하지 않습니다: " + mergeRuleFile.getAbsolutePath());
            }

            MergeRules mergeRules = yamlParser.parseMergeRules(mergeRuleFile);
            if (mergeRules.getRules() == null || mergeRules.getRules().isEmpty()) {
                logger.warn("머지 규칙이 없습니다. 처리할 내용이 없습니다.");
                return;
            }

            // 필수 속성 검증 (@JsonProperty(required = true)로 자동 검증되지만, 빈 값 체크는 수동으로 수행)
            validateRules(mergeRules.getRules());

            // 현재 프로젝트와 매칭되는 규칙 찾기
            List<Rule> matchedRules = findMatchingRules(mergeRules.getRules(), groupId, artifactId);
            if (matchedRules.isEmpty()) {
                logger.warn("매칭되는 머지 규칙이 없습니다. groupId={}, artifactId={}", groupId, artifactId);
                return;
            }

            logger.info("매칭된 규칙: {} 개", matchedRules.size());

            // 1단계: 모든 규칙의 mergeFiles 병합 (MergeRecipeDefinition 포함)
            RecipeMerger.MergeResult mergeResult = mergeAllFilesWithMerge(matchedRules);
            List<RecipeDefinition> mergedRecipes = mergeResult.getRecipes();
            List<MergeRecipeDefinition> mergedMergeDefinitions = mergeResult.getMergeDefinitions();

            // 2단계: MergeRecipeDefinition의 updateRecipeList 수행
            for (MergeRecipeDefinition mergeDef : mergedMergeDefinitions) {
                if (mergeDef.getRules() != null) {
                    for (com.yourcompany.plugins.rewriteprepare.model.UpdateRecipeList updateRecipeList : mergeDef.getRules()) {
                        if (updateRecipeList != null) {
                            mergedRecipes = recipeListUpdater.updateRecipeList(mergedRecipes, updateRecipeList);
                        }
                    }
                }
            }

            // 3단계: merge-rules.yml의 updateRecipeList 수행
            for (Rule rule : matchedRules) {
                if (rule.getUpdateRecipeList() != null) {
                    mergedRecipes = recipeListUpdater.updateRecipeList(mergedRecipes, rule.getUpdateRecipeList());
                }
            }

            // 4단계: 결과 출력 (MergeRecipeDefinition은 제외)
            yamlParser.writeRecipes(mergedRecipes, outputFile);

            logger.info("Rewrite Prepare Maven Plugin 실행 완료");

        } catch (Exception e) {
            logger.error("플러그인 실행 중 오류 발생", e);
            throw new MojoExecutionException("플러그인 실행 실패", e);
        }
    }

    /**
     * 규칙들의 필수 속성을 검증합니다.
     * @JsonProperty(required = true)로 필드 존재 여부는 자동 검증되지만,
     * 빈 문자열이나 빈 리스트는 수동으로 검증합니다.
     * requirements.md에 따라 다음 필드들이 필수입니다:
     * - artifactId: 필수, 없거나 비어 있으면 오류 발생
     * - mergeFiles: 필수, 없거나 비어 있으면 오류 발생
     * - updateRecipeList.name: updateRecipeList가 있으면 name도 필수, 없거나 비어 있으면 오류 발생
     *
     * @param rules 검증할 규칙 리스트
     * @throws MojoExecutionException 필수 속성이 비어 있는 경우
     */
    private void validateRules(List<Rule> rules) throws MojoExecutionException {
        for (int i = 0; i < rules.size(); i++) {
            Rule rule = rules.get(i);
            int ruleIndex = i + 1;

            // artifactId 검증 (빈 문자열 체크)
            if (rule.getArtifactId() != null && rule.getArtifactId().trim().isEmpty()) {
                throw new MojoExecutionException(
                    String.format("규칙 %d: artifactId는 필수 속성입니다. 비어 있으면 오류가 발생합니다.", ruleIndex));
            }

            // mergeFiles 검증 (빈 리스트 체크)
            if (rule.getMergeFiles() != null && rule.getMergeFiles().isEmpty()) {
                throw new MojoExecutionException(
                    String.format("규칙 %d: mergeFiles는 필수 속성입니다. 비어 있으면 오류가 발생합니다.", ruleIndex));
            }

            // updateRecipeList.name 검증 (updateRecipeList가 있으면 name도 필수, 빈 문자열 체크)
            if (rule.getUpdateRecipeList() != null) {
                String name = rule.getUpdateRecipeList().getName();
                if (name != null && name.trim().isEmpty()) {
                    throw new MojoExecutionException(
                        String.format("규칙 %d: updateRecipeList.name은 필수 속성입니다. 비어 있으면 오류가 발생합니다.", ruleIndex));
                }
            }
        }
    }

    /**
     * 현재 프로젝트의 groupId, artifactId와 매칭되는 규칙을 찾습니다.
     */
    private List<Rule> findMatchingRules(List<Rule> rules, String projectGroupId, String projectArtifactId) {
        return rules.stream()
                .filter(rule -> {
                    boolean groupIdMatches = PatternMatcher.matches(rule.getGroupId(), projectGroupId);
                    boolean artifactIdMatches = PatternMatcher.matches(rule.getArtifactId(), projectArtifactId);
                    return groupIdMatches && artifactIdMatches;
                })
                .collect(Collectors.toList());
    }

    /**
     * 모든 규칙의 mergeFiles를 병합합니다.
     * 중복된 파일은 skip합니다.
     * @deprecated mergeAllFilesWithMerge를 사용하세요.
     */
    @Deprecated
    private List<RecipeDefinition> mergeAllFiles(List<Rule> matchedRules) throws Exception {
        List<String> allMergeFiles = new ArrayList<>();
        for (Rule rule : matchedRules) {
            if (rule.getMergeFiles() != null) {
                allMergeFiles.addAll(rule.getMergeFiles());
            }
        }

        return recipeMerger.mergeFiles(recipeDirectory, allMergeFiles);
    }
    
    /**
     * 모든 규칙의 mergeFiles를 병합하고 MergeRecipeDefinition도 함께 반환합니다.
     * 중복된 파일은 skip합니다.
     */
    private RecipeMerger.MergeResult mergeAllFilesWithMerge(List<Rule> matchedRules) throws Exception {
        List<String> allMergeFiles = new ArrayList<>();
        for (Rule rule : matchedRules) {
            if (rule.getMergeFiles() != null) {
                allMergeFiles.addAll(rule.getMergeFiles());
            }
        }

        return recipeMerger.mergeFilesWithMerge(recipeDirectory, allMergeFiles);
    }
}

