# Rewrite Prepare Maven Plugin

OpenRewrite recipe YAML 파일들을 병합하고 `recipeList`를 편집하는 Maven 플러그인입니다.

## 개요

이 플러그인은 `recipeDirectory`에 있는 OpenRewrite recipe YAML 파일들을 찾아 병합하고, 지정한 이름으로 레시피 정의를 찾아 `recipeList` 속성의 값을 편집합니다.

## 설치

### Maven Central에서 설치 (향후 배포 시)

```xml
<plugin>
    <groupId>com.yourcompany.plugins</groupId>
    <artifactId>rewrite-prepare-maven-plugin</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</plugin>
```

### 로컬에서 빌드 및 설치

```bash
git clone https://github.com/wisesky0/rewrite-prepare-maven-plugin.git
cd rewrite-prepare-maven-plugin
mvn clean install
```

## 사용 방법

### 1. pom.xml 설정

프로젝트의 `pom.xml`에 플러그인을 추가합니다:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>com.yourcompany.plugins</groupId>
            <artifactId>rewrite-prepare-maven-plugin</artifactId>
            <version>0.1.0-SNAPSHOT</version>
            <configuration>
                <!-- recipeDirectory: recipe 파일들이 있는 디렉토리 (기본값: ${project.basedir}) -->
                <recipeDirectory>${project.basedir}/migration-ci/recipes</recipeDirectory>
                
                <!-- mergeRuleFile: 머지 규칙 파일 경로 (기본값: merge-rules.yml) -->
                <mergeRuleFile>${project.basedir}/migration-ci/merge-rules.yml</mergeRuleFile>
                
                <!-- outputFile: 출력 파일 경로 (기본값: openrewrite/rewrite.yml) -->
                <outputFile>${project.basedir}/openrewrite/rewrite.yml</outputFile>
                
                <!-- groupId: 프로젝트 groupId (기본값: ${project.groupId}) -->
                <groupId>com.example</groupId>
                
                <!-- artifactId: 프로젝트 artifactId (기본값: ${project.artifactId}) -->
                <artifactId>my-service</artifactId>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>prepare</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### 2. 플러그인 실행

#### Maven 빌드 시 자동 실행

`executions`에 설정되어 있으면 Maven 빌드 시 자동으로 실행됩니다:

```bash
mvn clean compile
```

#### 수동 실행

```bash
mvn rewrite-prepare:prepare
```

#### CLI 옵션으로 설정 오버라이드

```bash
mvn rewrite-prepare:prepare \
  -Drewrite-prepare.recipeDirectory=./custom-recipes \
  -Drewrite-prepare.mergeRuleFile=./custom-rules.yml \
  -Drewrite-prepare.outputFile=./output.yml \
  -Drewrite-prepare.groupId=com.custom \
  -Drewrite-prepare.artifactId=custom-service
```

## merge-rules.yml 설정

`merge-rules.yml` 파일은 프로젝트의 `groupId`와 `artifactId`에 따라 어떤 recipe 파일들을 병합하고 어떻게 `recipeList`를 업데이트할지 정의합니다.

### 기본 구조

```yaml
rules:
  - artifactId: my-service        # 필수: 프로젝트 artifactId (glob 패턴 지원)
    groupId: com.example          # 선택: 프로젝트 groupId (생략 시 '*'와 동일)
    mergeFiles:                   # 필수: 병합할 recipe 파일 목록 (상대 경로)
      - base.yml
      - my-service.yml
    updateRecipeList:             # 선택: recipeList 업데이트 설정
      name: com.example.Main     # 필수: 업데이트할 recipe 정의 이름
      updateOrder:               # 선택: 업데이트 순서
        - first:                  # recipeList의 가장 앞에 추가
          - com.example.Recipe1
        - last:                   # recipeList의 가장 마지막에 추가
          - com.example.Recipe10
        - before:                 # 지정한 recipe 앞에 추가
          - com.example.Recipe4:
              - com.example.Recipe5
        - after:                  # 지정한 recipe 뒤에 추가
          - com.example.Recipe7:
              - com.example.Recipe8
```

### 필수 속성

- **`artifactId`**: 필수 속성입니다. 없으면 오류가 발생합니다.
  - glob 패턴을 사용할 수 있습니다 (예: `'*-service'`, `'*-svc'`)
  
- **`mergeFiles`**: 필수 속성입니다. 없거나 비어 있으면 오류가 발생합니다.
  - `recipeDirectory` 하위의 상대 경로를 지정합니다.
  - 중복된 파일은 자동으로 스킵됩니다.

- **`updateRecipeList.name`**: `updateRecipeList`가 있으면 필수 속성입니다. 없거나 비어 있으면 오류가 발생합니다.
  - `recipeList`를 수정할 레시피 정의의 이름을 지정합니다.

### 선택 속성

- **`groupId`**: 생략 가능합니다. 생략된 경우 `'*'`와 같습니다.
  - glob 패턴을 사용할 수 있습니다.

- **`updateRecipeList`**: 생략 가능합니다. 생략하면 `recipeList` 업데이트를 수행하지 않습니다.

### updateOrder 설명

`updateOrder`는 다음 4가지 순서로 recipe를 추가할 수 있습니다:

#### 1. `first`: 가장 앞에 추가

```yaml
- first:
  - com.example.Recipe1
  - com.example.Recipe2
```

`recipeList`의 가장 앞에 지정한 recipe들을 순서대로 추가합니다.

#### 2. `last`: 가장 마지막에 추가

```yaml
- last:
  - com.example.Recipe10
```

`recipeList`의 가장 마지막에 지정한 recipe들을 순서대로 추가합니다.

#### 3. `before`: 지정한 recipe 앞에 추가

```yaml
- before:
  - com.example.Recipe4:        # 이 recipe 앞에 추가
    - com.example.Recipe5
  - com.example.Recipe14:       # 이 recipe 앞에 추가
    - com.example.Recipe15
```

지정한 recipe 이름 앞에 새로운 recipe들을 추가합니다. 여러 개의 대상 recipe를 지정할 수 있습니다.

#### 4. `after`: 지정한 recipe 뒤에 추가

```yaml
- after:
  - com.example.Recipe7:        # 이 recipe 뒤에 추가
    - com.example.Recipe8
  - com.example.Recipe17:       # 이 recipe 뒤에 추가
    - com.example.Recipe18
```

지정한 recipe 이름 뒤에 새로운 recipe들을 추가합니다. 여러 개의 대상 recipe를 지정할 수 있습니다.

### Glob 패턴 지원

`artifactId`와 `groupId`에는 glob 패턴을 사용할 수 있습니다:

```yaml
rules:
  - artifactId: '*-service'     # 모든 '-service'로 끝나는 artifactId 매칭
    mergeFiles:
      - base.yml
      - common.yml
  - artifactId: 'my-*'          # 'my-'로 시작하는 모든 artifactId 매칭
    mergeFiles:
      - base.yml
      - my-specific.yml
```

### 쉼표로 구분된 여러 패턴 지원

`artifactId`와 `groupId`에는 쉼표로 구분하여 여러 패턴을 지정할 수 있습니다. 하나라도 매칭되면 규칙이 적용됩니다:

```yaml
rules:
  - artifactId: 'my-service,other-service,*-api'  # 세 패턴 중 하나라도 매칭되면 적용
    mergeFiles:
      - base.yml
      - common.yml
  - artifactId: 'service-*,api-*,*-svc'            # glob 패턴과 일반 패턴 혼합 가능
    groupId: 'com.example,com.other'              # groupId에도 적용 가능
    mergeFiles:
      - base.yml
      - shared.yml
```

**참고**: 쉼표 주변의 공백은 자동으로 제거되며, 빈 패턴은 무시됩니다.

### 여러 규칙 매칭

프로젝트의 `groupId`와 `artifactId`가 여러 규칙과 매칭되는 경우:

1. **모든 규칙의 `mergeFiles`를 병합**: 중복된 파일은 자동으로 스킵됩니다.
2. **모든 규칙의 `updateRecipeList`를 순서대로 수행**: `recipeList`에 이미 있는 recipe를 추가하려 하면 경고를 출력하고 스킵합니다.
3. **병합된 결과를 출력 파일에 저장**

### 예시

```yaml
rules:
  # 특정 서비스에 대한 규칙
  - artifactId: my-service
    groupId: com.example
    mergeFiles:
      - base.yml
      - my-service.yml
    updateRecipeList:
      name: com.example.Main
      updateOrder:
        - first:
          - com.example.Recipe1
        - last:
          - com.example.Recipe10
        - before:
          - com.example.Recipe4:
              - com.example.Recipe5
        - after:
          - com.example.Recipe7:
              - com.example.Recipe8
  
  # 모든 서비스에 공통으로 적용되는 규칙
  - artifactId: '*-service'
    mergeFiles:
      - base.yml
      - common.yml
    updateRecipeList:
      name: com.example.Main
      updateOrder:
        - first:
          - com.example.Recipe11
        - last:
          - com.example.Recipe20
```

## Recipe Definition 형식

OpenRewrite recipe YAML 파일은 `---` 구분자로 여러 개의 recipe definition을 포함할 수 있습니다:

```yaml
---
type: specs.openrewrite.org/v1beta/recipe
name: com.example.Main
displayName: Main Recipe
description: Main recipe for migration
tags:
  - migration
  - base
recipeList:
  - org.openrewrite.text.ChangeText:
      toText: 2
  - com.example.RecipeB:
      exampleConfig1: foo
      exampleConfig2: bar
  - com.example.RecipeC
---
type: specs.openrewrite.org/v1beta/recipe
name: com.example.OtherRecipe
displayName: Other Recipe
recipeList:
  - com.example.Recipe1
```

### recipeList 형식

`recipeList`는 다음 두 가지 형식을 지원합니다:

1. **단순 문자열**: `- com.example.RecipeC`
   - 빈 attributes를 가진 recipe로 처리됩니다.

2. **Map 형식**: `- org.openrewrite.text.ChangeText: { toText: 2 }`
   - recipe 이름과 attributes를 포함합니다.

## 처리 순서

플러그인은 다음 순서로 작업을 수행합니다:

1. **규칙 매칭**: 프로젝트의 `groupId`, `artifactId`와 매칭되는 규칙을 찾습니다.
2. **파일 병합**: 매칭된 모든 규칙의 `mergeFiles`를 병합합니다 (중복 파일은 스킵).
3. **recipeList 업데이트**: 매칭된 모든 규칙의 `updateRecipeList`를 순서대로 수행합니다.
4. **결과 출력**: 최종 결과를 `outputFile`에 저장합니다.

## 프로젝트 구조 예시

```
project/
├── pom.xml
├── migration-ci/
│   ├── merge-rules.yml          # 머지 규칙 파일
│   └── recipes/
│       ├── base.yml             # 기본 recipe
│       ├── my-service.yml       # 서비스별 recipe
│       └── common.yml            # 공통 recipe
└── openrewrite/
    └── rewrite.yml              # 플러그인 실행 후 생성되는 출력 파일
```

## 로그 레벨

플러그인은 다음 로그 레벨로 정보를 출력합니다:

- **INFO**: 파일 병합 완료, recipe 추가 완료, 플러그인 실행 완료
- **WARN**: 파일이 존재하지 않음, recipe가 이미 존재함, 대상 recipe를 찾을 수 없음
- **DEBUG**: 상세한 처리 내역

## 문제 해결

### 규칙이 매칭되지 않는 경우

- `groupId`와 `artifactId`가 정확히 일치하는지 확인하세요.
- glob 패턴을 사용하는 경우 따옴표로 감싸야 합니다: `'*-service'`

### 필수 속성 오류

- `artifactId`는 필수입니다.
- `mergeFiles`는 필수이며 비어있으면 안 됩니다.
- `updateRecipeList`가 있으면 `name`도 필수입니다.

### recipeList 업데이트가 작동하지 않는 경우

- `updateRecipeList.name`에 지정한 recipe 정의가 병합된 파일에 존재하는지 확인하세요.
- `before`/`after`에서 지정한 recipe 이름이 `recipeList`에 존재하는지 확인하세요.

## 라이선스

이 프로젝트의 라이선스 정보는 LICENSE 파일을 참조하세요.
