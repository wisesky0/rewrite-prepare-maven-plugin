package com.yourcompany.plugins.rewriteprepare;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yourcompany.plugins.rewriteprepare.service.VariableReplacer;

/**
 * Rewrite Prepare Maven Plugin의 변수 치환 Mojo 클래스
 * 
 * 이 goal은 outputFile에 있는 recipe 파일에서 ${variable} 또는 $variable 형식의 변수를
 * rules/var-map.properties 파일에서 찾아서 값을 치환합니다.
 * 
 * 처리 순서:
 * 1. var-map.properties 파일을 읽어서 변수 맵을 생성합니다.
 * 2. outputFile에서 ${variable} 또는 $variable 패턴을 찾아서 치환합니다.
 * 3. 치환된 내용을 outputFile에 저장합니다.
 * 
 * 주의: 이 goal은 prepare goal 이후에 실행되어야 합니다.
 * prepare goal에서 머지된 결과 파일(outputFile)의 변수를 치환합니다.
 */
@Mojo(name = "resolve", defaultPhase = org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_RESOURCES)
public class RewriteResolveMojo extends AbstractMojo {
    private static final Logger logger = LoggerFactory.getLogger(RewriteResolveMojo.class);

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * 출력 파일 경로 (변수 치환 대상 파일)
     * 기본값: openrewrite/rewrite.yml
     * CLI 변수명: rewrite-prepare.outputFile
     */
    @Parameter(property = "rewrite-prepare.outputFile", defaultValue = "openrewrite/rewrite.yml")
    private File outputFile;

    /**
     * 변수 맵 파일 경로
     * 기본값: ${project.basedir}/migration-ci/rules/var-map.properties
     * CLI 변수명: rewrite-prepare.varMapFile
     */
    @Parameter(property = "rewrite-prepare.varMapFile", defaultValue = "${project.basedir}/migration-ci/rules/var-map.properties")
    private File varMapFile;

    private final VariableReplacer variableReplacer;

    public RewriteResolveMojo() {
        this.variableReplacer = new VariableReplacer();
    }

    @Override
    public void execute() throws MojoExecutionException {
        try {
            logger.info("Rewrite Resolve Maven Plugin 실행 시작");
            logger.info("설정 정보:");
            logger.info("  outputFile: {}", outputFile.getAbsolutePath());
            logger.info("  varMapFile: {}", varMapFile.getAbsolutePath());

            // var-map.properties 파일 확인
            if (!varMapFile.exists()) {
                logger.warn("변수 맵 파일이 존재하지 않습니다: {}", varMapFile.getAbsolutePath());
                logger.warn("변수 치환을 수행하지 않습니다.");
                return;
            }

            // outputFile 확인
            if (!outputFile.exists()) {
                logger.warn("출력 파일이 존재하지 않습니다: {}", outputFile.getAbsolutePath());
                logger.warn("prepare goal을 먼저 실행해야 합니다.");
                return;
            }

            // 변수 맵 로드
            variableReplacer.loadVariableMap(varMapFile);
            logger.info("변수 맵 로드 완료: {} 개의 변수", variableReplacer.getVariableCount());

            // outputFile에서 변수 치환 수행
            try {
                int replacedCount = variableReplacer.replaceVariablesInFile(outputFile);
                if (replacedCount > 0) {
                    logger.info("파일 처리 완료: {} ({} 개의 변수 치환)", outputFile.getName(), replacedCount);
                } else {
                    logger.info("파일 처리 완료: {} (치환된 변수 없음)", outputFile.getName());
                }
                logger.info("Rewrite Resolve Maven Plugin 실행 완료");
            } catch (IOException e) {
                logger.error("파일 처리 중 오류 발생: {}", outputFile.getAbsolutePath(), e);
                throw new MojoExecutionException("파일 처리 실패: " + outputFile.getAbsolutePath(), e);
            }

        } catch (Exception e) {
            logger.error("플러그인 실행 중 오류 발생", e);
            throw new MojoExecutionException("플러그인 실행 실패", e);
        }
    }
}

