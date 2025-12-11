package com.yourcompany.plugins.rewriteprepare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.Properties;

import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.yourcompany.plugins.RewriteInitializeMojo;

/**
 * RewriteInitializeMojo 클래스의 테스트
 * initialize goal 실행을 테스트합니다.
 * 
 * 참고: 실제 Mojo 실행 테스트는 Maven 플러그인 테스트 하네스를 사용해야 하지만,
 * 여기서는 Properties 파일 읽기 및 설정 기능을 검증합니다.
 */
class RewriteInitializeMojoTest {

    @Test
    void testPropMapFileLoading(@TempDir Path tempDir) throws Exception {
        // prop-map.properties 파일 생성
        File propMapFile = tempDir.resolve("prop-map.properties").toFile();
        String propMapContent = "# Property Map Properties\n" +
                "app.name=MyApplication\n" +
                "app.version=1.0.0\n" +
                "database.url=jdbc:mysql://localhost:3306/mydb\n" +
                "database.username=admin\n" +
                "service.port=8080\n";

        try (FileWriter writer = new FileWriter(propMapFile)) {
            writer.write(propMapContent);
        }

        // Properties 파일 읽기 테스트
        java.util.Properties properties = new java.util.Properties();
        try (java.io.FileInputStream inputStream = new java.io.FileInputStream(propMapFile)) {
            properties.load(inputStream);
        }

        // 검증
        assertNotNull(properties);
        assertEquals(5, properties.size());
        assertEquals("MyApplication", properties.getProperty("app.name"));
        assertEquals("1.0.0", properties.getProperty("app.version"));
        assertEquals("jdbc:mysql://localhost:3306/mydb", properties.getProperty("database.url"));
        assertEquals("admin", properties.getProperty("database.username"));
        assertEquals("8080", properties.getProperty("service.port"));
    }

    @Test
    void testPropertiesSetting(@TempDir Path tempDir) throws Exception {
        // prop-map.properties 파일 생성
        File propMapFile = tempDir.resolve("prop-map.properties").toFile();
        String propMapContent = "test.key1=value1\n" +
                "test.key2=value2\n" +
                "test.key3=value3\n";

        try (FileWriter writer = new FileWriter(propMapFile)) {
            writer.write(propMapContent);
        }

        // Properties 파일 읽기
        java.util.Properties propProperties = new java.util.Properties();
        try (java.io.FileInputStream inputStream = new java.io.FileInputStream(propMapFile)) {
            propProperties.load(inputStream);
        }

        // MavenProject properties 시뮬레이션
        Properties projectProperties = new Properties();
        
        // prop-map.properties의 값을 project properties에 설정
        for (String key : propProperties.stringPropertyNames()) {
            String value = propProperties.getProperty(key);
            if (value != null) {
                projectProperties.setProperty(key.trim(), value.trim());
            }
        }

        // 검증
        assertEquals(3, projectProperties.size());
        assertEquals("value1", projectProperties.getProperty("test.key1"));
        assertEquals("value2", projectProperties.getProperty("test.key2"));
        assertEquals("value3", projectProperties.getProperty("test.key3"));
    }

    @Test
    void testPropertiesOverwrite(@TempDir Path tempDir) throws Exception {
        // prop-map.properties 파일 생성
        File propMapFile = tempDir.resolve("prop-map.properties").toFile();
        String propMapContent = "existing.key=new-value\n";

        try (FileWriter writer = new FileWriter(propMapFile)) {
            writer.write(propMapContent);
        }

        // MavenProject properties 시뮬레이션 (기존 값이 있는 경우)
        Properties projectProperties = new Properties();
        projectProperties.setProperty("existing.key", "old-value");
        
        // prop-map.properties의 값을 project properties에 설정 (덮어쓰기)
        java.util.Properties propProperties = new java.util.Properties();
        try (java.io.FileInputStream inputStream = new java.io.FileInputStream(propMapFile)) {
            propProperties.load(inputStream);
        }

        for (String key : propProperties.stringPropertyNames()) {
            String value = propProperties.getProperty(key);
            if (value != null) {
                projectProperties.setProperty(key.trim(), value.trim());
            }
        }

        // 검증: 기존 값이 새 값으로 덮어써졌는지 확인
        assertEquals("new-value", projectProperties.getProperty("existing.key"));
    }

    @Test
    void testEmptyPropertiesFile(@TempDir Path tempDir) throws Exception {
        // 빈 prop-map.properties 파일 생성
        File propMapFile = tempDir.resolve("prop-map.properties").toFile();
        try (FileWriter writer = new FileWriter(propMapFile)) {
            writer.write("# Empty properties file\n");
        }

        // Properties 파일 읽기
        java.util.Properties properties = new java.util.Properties();
        try (java.io.FileInputStream inputStream = new java.io.FileInputStream(propMapFile)) {
            properties.load(inputStream);
        }

        // 검증: 빈 파일이어도 오류 없이 처리되어야 함
        assertNotNull(properties);
        assertTrue(properties.isEmpty());
    }
}

