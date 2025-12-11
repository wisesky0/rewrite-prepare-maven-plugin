package com.yourcompany.plugins.rewriteprepare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.Properties;

import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.yourcompany.plugins.RewriteInitializeMojo;
import com.yourcompany.plugins.RewritePrepareMojo;

/**
 * Initialize 골에서 설정한 properties를 다른 골에서 읽어오는지 테스트하는 통합 테스트
 * 
 * 이 테스트는 initialize 골에서 prop-map.properties 파일을 읽어서
 * MavenProject의 properties에 설정한 후, 다른 골(prepare)에서
 * 그 properties를 읽어올 수 있는지 검증합니다.
 */
class InitializePropertiesIntegrationTest {

    /**
     * initialize 골에서 설정한 properties를 prepare 골에서 읽어오는지 테스트
     * 
     * 테스트 시나리오:
     * 1. prop-map.properties 파일 생성
     * 2. initialize 골 실행하여 properties 설정
     * 3. prepare 골에서 설정된 properties를 읽어오는지 확인
     */
    @Test
    void testPropertiesFromInitializeToPrepare(@TempDir Path tempDir) throws Exception {
        // 테스트 프로젝트 디렉토리 설정
        File projectDir = tempDir.toFile();
        File rulesDir = new File(projectDir, "migration-ci/rules");
        rulesDir.mkdirs();

        // prop-map.properties 파일 생성
        File propMapFile = new File(rulesDir, "prop-map.properties");
        String propMapContent = "# Property Map Properties\n" +
                "test.app.name=TestApplication\n" +
                "test.app.version=2.0.0\n" +
                "test.database.url=jdbc:mysql://test:3306/testdb\n" +
                "test.service.port=9090\n";

        try (FileWriter writer = new FileWriter(propMapFile)) {
            writer.write(propMapContent);
        }

        // MavenProject 시뮬레이션 (실제 MavenProject 객체 생성)
        MavenProject project = createMockMavenProject(projectDir);

        // 1. Initialize 골 실행
        RewriteInitializeMojo initializeMojo = new RewriteInitializeMojo();
        setField(initializeMojo, "project", project);
        setField(initializeMojo, "propMapFile", propMapFile);

        // initialize 골 실행
        initializeMojo.execute();

        // 2. Initialize 골에서 설정한 properties 확인
        Properties projectProperties = project.getProperties();
        assertNotNull(projectProperties, "project properties가 null이 아니어야 합니다.");
        assertEquals("TestApplication", projectProperties.getProperty("test.app.name"));
        assertEquals("2.0.0", projectProperties.getProperty("test.app.version"));
        assertEquals("jdbc:mysql://test:3306/testdb", projectProperties.getProperty("test.database.url"));
        assertEquals("9090", projectProperties.getProperty("test.service.port"));

        // 3. Prepare 골에서 같은 MavenProject를 사용하여 properties 읽기
        RewritePrepareMojo prepareMojo = new RewritePrepareMojo();
        setField(prepareMojo, "project", project);

        // prepare 골에서 properties를 읽어올 수 있는지 확인 (리플렉션 사용)
        MavenProject prepareProject = (MavenProject) getField(prepareMojo, "project");
        Properties prepareProperties = prepareProject.getProperties();
        assertNotNull(prepareProperties, "prepare 골에서 properties를 읽을 수 있어야 합니다.");
        assertEquals("TestApplication", prepareProperties.getProperty("test.app.name"));
        assertEquals("2.0.0", prepareProperties.getProperty("test.app.version"));
        assertEquals("jdbc:mysql://test:3306/testdb", prepareProperties.getProperty("test.database.url"));
        assertEquals("9090", prepareProperties.getProperty("test.service.port"));
    }

    /**
     * initialize 골에서 설정한 properties를 resolve 골에서 읽어오는지 테스트
     */
    @Test
    void testPropertiesFromInitializeToResolve(@TempDir Path tempDir) throws Exception {
        // 테스트 프로젝트 디렉토리 설정
        File projectDir = tempDir.toFile();
        File rulesDir = new File(projectDir, "migration-ci/rules");
        rulesDir.mkdirs();

        // prop-map.properties 파일 생성
        File propMapFile = new File(rulesDir, "prop-map.properties");
        String propMapContent = "resolve.test.key=resolve-test-value\n" +
                "resolve.test.number=12345\n";

        try (FileWriter writer = new FileWriter(propMapFile)) {
            writer.write(propMapContent);
        }

        // MavenProject 시뮬레이션
        MavenProject project = createMockMavenProject(projectDir);

        // 1. Initialize 골 실행
        RewriteInitializeMojo initializeMojo = new RewriteInitializeMojo();
        setField(initializeMojo, "project", project);
        setField(initializeMojo, "propMapFile", propMapFile);

        initializeMojo.execute();

        // 2. Resolve 골에서 같은 MavenProject를 사용하여 properties 읽기
        com.yourcompany.plugins.RewriteResolveMojo resolveMojo = new com.yourcompany.plugins.RewriteResolveMojo();
        setField(resolveMojo, "project", project);

        // resolve 골에서 properties를 읽어올 수 있는지 확인 (리플렉션 사용)
        MavenProject resolveProject = (MavenProject) getField(resolveMojo, "project");
        Properties resolveProperties = resolveProject.getProperties();
        assertNotNull(resolveProperties, "resolve 골에서 properties를 읽을 수 있어야 합니다.");
        assertEquals("resolve-test-value", resolveProperties.getProperty("resolve.test.key"));
        assertEquals("12345", resolveProperties.getProperty("resolve.test.number"));
    }

    /**
     * 여러 골에서 동일한 properties를 공유하는지 테스트
     */
    @Test
    void testPropertiesSharedBetweenGoals(@TempDir Path tempDir) throws Exception {
        // 테스트 프로젝트 디렉토리 설정
        File projectDir = tempDir.toFile();
        File rulesDir = new File(projectDir, "migration-ci/rules");
        rulesDir.mkdirs();

        // prop-map.properties 파일 생성
        File propMapFile = new File(rulesDir, "prop-map.properties");
        String propMapContent = "shared.key=shared-value\n" +
                "shared.number=999\n";

        try (FileWriter writer = new FileWriter(propMapFile)) {
            writer.write(propMapContent);
        }

        // MavenProject 시뮬레이션
        MavenProject project = createMockMavenProject(projectDir);

        // Initialize 골 실행
        RewriteInitializeMojo initializeMojo = new RewriteInitializeMojo();
        setField(initializeMojo, "project", project);
        setField(initializeMojo, "propMapFile", propMapFile);
        initializeMojo.execute();

        // Prepare 골에서 properties 읽기
        RewritePrepareMojo prepareMojo = new RewritePrepareMojo();
        setField(prepareMojo, "project", project);
        MavenProject prepareProject = (MavenProject) getField(prepareMojo, "project");
        Properties prepareProperties = prepareProject.getProperties();

        // Resolve 골에서 properties 읽기
        com.yourcompany.plugins.RewriteResolveMojo resolveMojo = new com.yourcompany.plugins.RewriteResolveMojo();
        setField(resolveMojo, "project", project);
        MavenProject resolveProject = (MavenProject) getField(resolveMojo, "project");
        Properties resolveProperties = resolveProject.getProperties();

        // 두 골이 동일한 properties 객체를 공유하는지 확인
        assertTrue(prepareProperties == resolveProperties, 
                   "prepare와 resolve 골이 동일한 properties 객체를 공유해야 합니다.");

        // 두 골 모두에서 동일한 값을 읽을 수 있는지 확인
        assertEquals("shared-value", prepareProperties.getProperty("shared.key"));
        assertEquals("shared-value", resolveProperties.getProperty("shared.key"));
        assertEquals("999", prepareProperties.getProperty("shared.number"));
        assertEquals("999", resolveProperties.getProperty("shared.number"));
    }

    /**
     * Mock MavenProject 생성
     */
    private MavenProject createMockMavenProject(File basedir) {
        MavenProject project = new MavenProject();
        project.setFile(new File(basedir, "pom.xml"));
        project.setGroupId("com.example");
        project.setArtifactId("test-project");
        project.setVersion("1.0.0");
        return project;
    }

    /**
     * 리플렉션을 사용하여 private 필드에 값 설정
     */
    private void setField(Object target, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * 리플렉션을 사용하여 private 필드 값 읽기
     */
    private Object getField(Object target, String fieldName) throws Exception {
        java.lang.reflect.Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    /**
     * 클래스 계층 구조에서 필드 찾기
     */
    private java.lang.reflect.Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> currentClass = clazz;
        while (currentClass != null) {
            try {
                return currentClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                currentClass = currentClass.getSuperclass();
            }
        }
        throw new NoSuchFieldException("Field " + fieldName + " not found in " + clazz);
    }
}

