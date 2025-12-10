# 테스트 프로젝트

이 디렉토리는 `rewrite-prepare-maven-plugin`을 테스트하기 위한 샘플 프로젝트입니다.

## 구조

```
test-project/
├── pom.xml                          # 플러그인 설정이 포함된 테스트 프로젝트
├── migration-ci/
│   ├── rules/
│   │   └── merge-rules.yml          # 머지 규칙 파일
│   └── recipes/
│       ├── base.yml                 # 기본 레시피 정의 및 트리거 recipe
│       ├── my-service.yml           # base.yml에 머지되는 파일 (서비스별 recipe)
│       └── common.yml                # base.yml에 머지되는 파일 (공통 recipe)
└── openrewrite/
    └── rewrite.yml                  # 플러그인 실행 후 생성되는 출력 파일
```

### 파일 역할

- **base.yml**: 기본 레시피 정의와 트리거 recipe(최종 실행 recipe)를 정의합니다. `org.yourcompany.openrewrite/v1/merge` 타입을 포함하지 않습니다.
- **my-service.yml**: `base.yml`에 머지되어 최종 실행 recipe를 만드는 용도의 파일입니다. `org.yourcompany.openrewrite/v1/merge` 타입을 포함하여 자신의 레시피가 트리거 recipe의 `recipeList`에 어떻게 추가되는지 정의합니다.
- **common.yml**: `base.yml`에 머지되어 최종 실행 recipe를 만드는 용도의 파일입니다. `org.yourcompany.openrewrite/v1/merge` 타입을 포함하여 자신의 레시피가 트리거 recipe의 `recipeList`에 어떻게 추가되는지 정의합니다.

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
mvn com.yourcompany.plugins:rewrite-prepare-maven-plugin:1.0-SNAPSHOT:prepare
```

### 4. 결과 확인

플러그인 실행 후 `openrewrite/rewrite.yml` 파일이 생성됩니다.

## 예상 결과

`merge-rules.yml`의 설정에 따라:

1. **파일 병합**: `base.yml`, `my-service.yml`, `common.yml`이 순서대로 병합됩니다.
2. **recipeList 업데이트**: `com.example.Main` recipe의 `recipeList`가 `org.yourcompany.openrewrite/v1/merge` 타입의 규칙에 따라 업데이트됩니다:
   
   **my-service.yml의 규칙:**
   - `com.example.Recipe1`이 가장 앞에 추가됩니다 (first).
   - `com.example.Recipe10`이 가장 마지막에 추가됩니다 (last).
   - `com.example.Recipe5`가 `com.example.Recipe2` 앞에 추가됩니다 (before).
   - `com.example.Recipe8`이 `org.openrewrite.text.ChangeText` 뒤에 추가됩니다 (after).
   
   **common.yml의 규칙:**
   - `com.example.Recipe11`이 가장 앞에 추가됩니다 (first).
   - `com.example.Recipe20`이 가장 마지막에 추가됩니다 (last).
   - `com.example.Recipe6`이 `org.openrewrite.text.ChangeText` 앞에 추가됩니다 (before).
   - `com.example.Recipe9`이 `com.example.Recipe3` 뒤에 추가됩니다 (after).

3. **최종 출력**: `org.yourcompany.openrewrite/v1/merge` 타입은 제외하고 모든 `specs.openrewrite.org/v1beta/recipe` 타입의 정의만 `openrewrite/rewrite.yml`에 저장됩니다.

## CLI 옵션

플러그인 설정을 CLI로 오버라이드할 수 있습니다:

```bash
mvn rewrite-prepare:prepare \
  -Drewrite-prepare.recipeDirectory=./custom-recipes \
  -Drewrite-prepare.mergeRuleFile=./custom-rules.yml \
  -Drewrite-prepare.outputFile=./rewrite.yml \
  -Drewrite-prepare.groupId=com.custom \
  -Drewrite-prepare.artifactId=custom-service
```

