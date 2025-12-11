package com.yourcompany.plugins;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Rewrite Prepare Maven Plugin의 초기화 Mojo 클래스
 * 
 * 이 goal은 migration-ci/rules/prop-map.properties 파일을 읽어서
 * MavenProject의 properties에 값을 설정합니다.
 * 이후에 수행할 plugin에서 ${project.properties.key} 형식으로
 * 이 값을 참조할 수 있습니다.
 * 
 * 처리 순서:
 * 1. prop-map.properties 파일을 읽습니다.
 * 2. 파일의 모든 key-value 쌍을 MavenProject의 properties에 설정합니다.
 * 3. 설정된 properties는 이후 실행되는 다른 plugin에서 참조할 수 있습니다.
 * 
 * 주의: 이 goal은 다른 plugin goal들보다 먼저 실행되어야 합니다.
 * 기본 phase는 INITIALIZE로 설정되어 있습니다.
 */
@Mojo(name = "initialize", defaultPhase = org.apache.maven.plugins.annotations.LifecyclePhase.INITIALIZE)
public class RewriteInitializeMojo extends AbstractMojo {
    private static final Logger logger = LoggerFactory.getLogger(RewriteInitializeMojo.class);

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * prop-map.properties 파일 경로
     * 기본값: ${project.basedir}/migration-ci/rules/prop-map.properties
     * CLI 변수명: rewrite-prepare.propMapFile
     */
    @Parameter(property = "rewrite-prepare.propMapFile", defaultValue = "${project.basedir}/migration-ci/rules/prop-map.properties")
    private File propMapFile;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            logger.info("Rewrite Initialize Maven Plugin 실행 시작");
            logger.info("prop-map 파일 경로: {}", propMapFile.getAbsolutePath());

            // prop-map.properties 파일 확인
            if (!propMapFile.exists()) {
                logger.warn("prop-map 파일이 존재하지 않습니다: {}", propMapFile.getAbsolutePath());
                logger.warn("properties 설정을 수행하지 않습니다.");
                return;
            }

            // Properties 파일 읽기
            Properties propProperties = new Properties();
            try (InputStream inputStream = new FileInputStream(propMapFile)) {
                propProperties.load(inputStream);
            } catch (IOException e) {
                logger.error("prop-map 파일 읽기 실패: {}", propMapFile.getAbsolutePath(), e);
                throw new MojoExecutionException("prop-map 파일 읽기 실패: " + propMapFile.getAbsolutePath(), e);
            }

            // MavenProject의 properties에 설정
            Properties projectProperties = project.getProperties();
            int propertyCount = 0;
            
            for (String key : propProperties.stringPropertyNames()) {
                String value = propProperties.getProperty(key);
                if (value != null) {
                    String trimmedKey = key.trim();
                    String trimmedValue = value.trim();
                    
                    // 기존 property가 있으면 경고 로그 출력
                    if (projectProperties.containsKey(trimmedKey)) {
                        logger.warn("기존 property를 덮어씁니다: {} = {} (기존 값: {})", 
                                   trimmedKey, trimmedValue, projectProperties.getProperty(trimmedKey));
                    }
                    
                    projectProperties.setProperty(trimmedKey, trimmedValue);
                    logger.debug("Property 설정: {} = {}", trimmedKey, trimmedValue);
                    propertyCount++;
                }
            }

            logger.info("Property 설정 완료: {} 개의 property가 설정되었습니다.", propertyCount);
            
            if (logger.isDebugEnabled() && propertyCount > 0) {
                logger.debug("설정된 property 목록:");
                for (String key : propProperties.stringPropertyNames()) {
                    logger.debug("  {} = {}", key.trim(), propProperties.getProperty(key).trim());
                }
            }

            logger.info("Rewrite Initialize Maven Plugin 실행 완료");

        } catch (Exception e) {
            logger.error("플러그인 실행 중 오류 발생", e);
            throw new MojoExecutionException("플러그인 실행 실패", e);
        }
    }
}

