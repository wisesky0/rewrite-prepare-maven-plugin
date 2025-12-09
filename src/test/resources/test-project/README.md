# 테스트 프로젝트

이 디렉토리는 `rewrite-prepare-maven-plugin`을 테스트하기 위한 샘플 프로젝트입니다.

## 구조

```
test-project/
├── pom.xml                          # 플러그인 설정이 포함된 테스트 프로젝트
├── migration-ci/
│   ├── merge-rules.yml              # 머지 규칙 파일
│   └── recipes/
│       ├── base.yml                 # 기본 recipe
│       ├── my-service.yml           # 서비스별 recipe
│       └── common.yml               # 공통 recipe
└── openrewrite/
    └── rewrite.yml                  # 플러그인 실행 후 생성되는 출력 파일
```

## 사용 방법

### 1. 플러그인 설치

먼저 플러그인을 로컬 Maven 저장소에 설치합니다:

```bash
cd /path/to/rewrite-prepare-maven-plugin
mvn clean install
```

### 2. 테스트 프로젝트로 이동

```bash
cd src/test/resources/test-project
```

### 3. 플러그인 실행

```bash
mvn rewrite-prepare:prepare
```

또는

```bash
mvn com.yourcompany.plugins:rewrite-prepare-maven-plugin:0.1.0-SNAPSHOT:prepare
```

### 4. 결과 확인

플러그인 실행 후 `openrewrite/rewrite.yml` 파일이 생성됩니다.

## 예상 결과

`merge-rules.yml`의 설정에 따라:

1. `base.yml`과 `my-service.yml`이 병합됩니다.
2. `com.example.Main` recipe의 `recipeList`가 업데이트됩니다:
   - `com.example.Recipe1`이 가장 앞에 추가됩니다.
   - `com.example.Recipe10`이 가장 마지막에 추가됩니다.
   - `com.example.Recipe5`가 `com.example.Recipe4` 앞에 추가됩니다.
   - `com.example.Recipe8`이 `com.example.Recipe7` 뒤에 추가됩니다.

## CLI 옵션

플러그인 설정을 CLI로 오버라이드할 수 있습니다:

```bash
mvn rewrite-prepare:prepare \
  -Drewrite-prepare.recipeDirectory=./custom-recipes \
  -Drewrite-prepare.mergeRuleFile=./custom-rules.yml \
  -Drewrite-prepare.outputFile=./output.yml \
  -Drewrite-prepare.groupId=com.custom \
  -Drewrite-prepare.artifactId=custom-service
```

