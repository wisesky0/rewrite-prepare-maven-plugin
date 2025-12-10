# Rewrite Prepare Maven Plugin 요구사항 정의서

## 개요

이 플러그인은 `recipeDirectory`에 있는 OpenRewrite recipe YAML 파일들을 찾아 병합하고, 지정한 이름으로 레시피 정의를 찾아 `recipeList` 속성의 값을 편집합니다.

플러그인은 `merge-rules.yml` 파일의 규칙에 따라 프로젝트의 `groupId`, `artifactId`와 매칭되는 규칙을 찾고, 해당 규칙에 지정된 Recipe Definition 파일들을 병합한 후, `recipeList`를 업데이트하여 최종 결과를 출력 파일에 저장합니다.

**파일 역할:**

- **base.yml**: 기본 레시피 정의와 트리거 recipe(최종 실행 recipe)를 정의하는 파일입니다. `org.yourcompany.openrewrite/v1/merge` 타입을 포함하지 않습니다.
- **머지 파일들**(`my-service.yml`, `common.yml` 등): `base.yml`에 머지되어 최종 실행 recipe를 만드는 용도의 파일들입니다. `org.yourcompany.openrewrite/v1/merge` 타입을 포함하여 자신의 레시피가 트리거 recipe의 `recipeList`에 어떻게 추가되는지 정의합니다.

## 1. 전체 처리 흐름

다음은 플러그인이 recipe 파일을 병합하고 업데이트하는 전체 과정입니다:

```
[프로젝트 정보]
  ├─ groupId: com.example
  └─ artifactId: my-service
         │
         ▼
[merge-rules.yml 읽기]
  └─ 매칭되는 규칙 찾기
         │
         ▼
[파일 병합]
  ├─ base.yml (기본 레시피, 트리거 recipe) ────┐
  ├── my-service.yml (머지 파일) ───────────────┼─→ [병합된 Recipe Definitions]
  └── common.yml (머지 파일) ───────────────────┘
         │
         ▼
[recipeList 업데이트]
  └─ org.yourcompany.openrewrite/v1/merge 타입의 규칙 적용
         │
         ▼
[결과 출력]
  └─ openrewrite/rewrite.yml
```

### 상세 처리 단계

1. **규칙 매칭**
   - 프로젝트의 `groupId`, `artifactId`와 `merge-rules.yml`의 `rules` 항목을 비교합니다.
   - glob 패턴 및 쉼표로 구분된 여러 패턴을 지원합니다.
   - 매칭되는 모든 규칙을 선택합니다.

2. **파일 병합**
   - 매칭된 규칙들의 `mergeFiles`에 지정된 파일들을 순서대로 읽어 병합합니다.
   - 첫 번째 파일은 일반적으로 `base.yml`로, 기본 레시피 정의와 트리거 recipe(최종 실행 recipe)를 정의합니다.
   - 이후 파일들(`my-service.yml`, `common.yml` 등)은 `base.yml`에 머지되어 최종 실행 recipe를 만드는 용도입니다.
   - 중복 파일은 한 번만 처리합니다 (스킵).
   - 각 Recipe Definition 파일은 `---` 구분자로 여러 recipe definition을 포함할 수 있습니다.

3. **recipeList 업데이트**
   - 병합된 Recipe Definition 중 `type: org.yourcompany.openrewrite/v1/merge` 타입을 찾습니다.
   - 이 타입은 `base.yml`에 머지되는 파일들(`my-service.yml`, `common.yml` 등)에만 포함됩니다.
   - 각 merge 정의의 `rules[*].updateRecipeList`를 순서대로 적용합니다.
   - `updateRecipeList.name`과 일치하는 `specs.openrewrite.org/v1beta/recipe` 타입의 recipe(일반적으로 `base.yml`에 정의된 트리거 recipe)를 찾습니다.
   - `updateOrder`에 따라 해당 recipe의 `recipeList`를 업데이트합니다.

4. **결과 출력**
   - `org.yourcompany.openrewrite/v1/merge` 타입은 제외하고 모든 `specs.openrewrite.org/v1beta/recipe` 타입의 정의를 `outputFile`에 저장합니다.
   - 각 recipe definition 앞에 `# Source: 파일명` 주석을 추가합니다.

## 2. 디렉토리 구조

프로젝트의 디렉토리 구조는 다음과 같습니다:

```
project-root/
├── pom.xml
└── migration-ci/
    ├── rules/
    │   └── merge-rules.yml
    └── recipes/
        ├── base.yml
        ├── my-service.yml
        └── common.yml
```

### 디렉토리 설명

- **migration-ci/rules/**: 머지 규칙 파일(`merge-rules.yml`)이 위치하는 디렉토리입니다.
- **migration-ci/recipes/**: Recipe Definition 파일들이 위치하는 디렉토리입니다.
  - `base.yml`: 기본 레시피 정의와 트리거 recipe(최종 실행 recipe)를 정의하는 파일입니다.
  - `my-service.yml`, `common.yml` 등: `base.yml`에 머지되어 최종 실행 recipe를 만드는 용도의 파일들입니다.

## 3. pom.xml 설정 예시

```xml
<plugin>
    <groupId>com.yourcompany.plugins</groupId>
    <artifactId>rewrite-prepare-maven-plugin</artifactId>
    <version>1.0.0</version>
    <configuration>
        <!-- recipeDirectory: 생략 가능하며, 기본값은 ${project.basedir}입니다. CLI 변수명은 rewrite-prepare.recipeDirectory -->
        <recipeDirectory>${project.basedir}/migration-ci/recipes</recipeDirectory>
        
        <!-- mergeRuleFile: 생략 가능하며, 기본값은 merge-rules.yml입니다. CLI 변수명은 rewrite-prepare.mergeRuleFile -->
        <mergeRuleFile>${project.basedir}/migration-ci/rules/merge-rules.yml</mergeRuleFile>
        
        <!-- outputFile: 생략 가능하며, 기본값은 openrewrite/rewrite.yml입니다. CLI 변수명은 rewrite-prepare.outputFile -->
        <outputFile>${project.basedir}/openrewrite/rewrite.yml</outputFile>
        
        <!-- groupId: 생략 가능하며, 기본값은 project.groupId입니다. CLI 변수명은 rewrite-prepare.groupId -->
        <groupId>com.example</groupId>
        
        <!-- artifactId: 생략 가능하며, 기본값은 project.artifactId입니다. CLI 변수명은 rewrite-prepare.artifactId -->
        <artifactId>my-service</artifactId>
    </configuration>
</plugin>
```

## 4. 머지 규칙 파일 (merge-rules.yml)

rewrite-prepare-maven-plugin에서 지정한 `groupId`, `artifactId`와 `rules`의 `artifactId`, `groupId`를 매핑하여 선택된 머지 규칙에 따라 하나의 recipe YAML 파일을 생성합니다.

**파일 역할:**

- 첫 번째 파일(일반적으로 `base.yml`): 기본 레시피 정의와 트리거 recipe(최종 실행 recipe)를 정의합니다.
- 이후 파일들(`my-service.yml`, `common.yml` 등): `base.yml`에 머지되어 최종 실행 recipe를 만드는 용도입니다.

### 4.1 YAML 형식

```yaml
rules:
  - artifactId: my-service # glob 패턴을 사용하여, '*-svc'와 같이 작성할 수도 있습니다. 쉼표로 구분하여 여러 패턴을 지정할 수 있습니다 (예: 'my-service,other-service,*-api'). 필수 속성으로 없으면 오류가 발생합니다.
    groupId: com.example  # groupId는 생략 가능하며, 생략된 경우 '*'와 같습니다. 쉼표로 구분하여 여러 패턴을 지정할 수 있습니다.
    mergeFiles: # recipeDirectory 하위의 상대 경로입니다. 필수 속성으로 없거나 비어 있으면 오류가 발생합니다.
      - base.yml          # 기본 레시피 정의 및 트리거 recipe
      - my-service.yml    # base.yml에 머지되는 파일
```

### 4.2 규칙 매칭

- `artifactId`와 `groupId`는 glob 패턴을 지원합니다 (예: `*-service`, `my-*`).
- 쉼표로 구분하여 여러 패턴을 지정할 수 있습니다 (예: `'my-service,other-service,*-api'`).
- `groupId`가 생략된 경우 모든 `groupId`와 매칭됩니다 (`*`와 동일).

### 4.3 mergeFiles 경로

`mergeFiles`에 지정된 경로는 `recipeDirectory`를 기준으로 한 상대 경로입니다.

예를 들어, `recipeDirectory`가 `${project.basedir}/migration-ci/recipes`이고 `mergeFiles`에 `base.yml`이 지정된 경우, 실제 파일 경로는 `${project.basedir}/migration-ci/recipes/base.yml`이 됩니다.

## 5. Recipe Definition 형식

Recipe Definition 파일(`mergeFiles`에 지정된 YAML 파일)에는 두 가지 타입의 recipe definition을 포함할 수 있습니다.

### 5.1 머지 규칙 정의 (org.yourcompany.openrewrite/v1/merge)

`org.yourcompany.openrewrite/v1/merge` 타입은 파싱 후 트리거 recipe(대상 recipe)의 `recipeList`에 자신의 레시피가 어떻게 호출되는지를 정의하기 위한 목적입니다.

즉, `org.yourcompany.openrewrite/v1/merge` 타입은 `recipeList` 업데이트 규칙을 정의하여, 특정 recipe의 `recipeList`에 다른 recipe들을 어떤 순서로 추가할지 지정합니다. **이 타입의 정의는 병합된 결과 파일에 포함되지 않습니다.**

**사용 위치:**

- **`base.yml`에는 포함하지 않습니다.** `base.yml`은 기본 레시피 정의와 트리거 recipe를 정의하는 파일이므로 이 타입을 포함하지 않습니다.
- **머지되는 파일들에만 포함합니다.** `base.yml`에 머지되어 최종 실행 recipe를 만드는 용도의 파일들(`my-service.yml`, `common.yml` 등)에 이 타입을 포함하여 해당 파일의 레시피들이 트리거 recipe의 `recipeList`에 어떻게 추가되는지 정의합니다.

```yaml
---
type: org.yourcompany.openrewrite/v1/merge
name: 마이그레이션 레시피 머지 규칙
rules: 
  - updateRecipeList:
      # name: recipeList를 수정할 레시피 정의의 이름. 필수 속성으로 없거나 비어 있으면 오류가 발생합니다.
      name: com.example.Main
      updateOrder: 
        # first: com.example.Main 레시피 정의를 찾아 recipeList의 가장 앞에 삽입합니다.
        - first:
            # List<UpdateEntry>
          - com.example.Recipe1  # UpdateEntry{ key: null, values: ["com.example.Recipe1"] }
        # last: com.example.Main 레시피 정의를 찾아 recipeList의 가장 마지막에 추가합니다.
        - last:
          - com.example.Recipe10 
        # before: com.example.Main 레시피 정의를 찾아 recipeList에서 지정한 이름을 찾아 그 앞에 추가합니다.
        - before:
          # List<UpdateEntry>
          - com.example.Recipe4:   # UpdateEntry{ key: "com.example.Recipe4", values: ["com.example.Recipe5"] }
            - com.example.Recipe5
          - com.example.Recipe14:   # UpdateEntry{ key: "com.example.Recipe14", values: ["com.example.Recipe15"] }
            - com.example.Recipe15
        # after: com.example.Main 레시피 정의를 찾아 recipeList에서 지정한 이름을 찾아 그 뒤에 추가합니다.
        - after:
          - com.example.Recipe7:
            - com.example.Recipe8
          - com.example.Recipe17:
            - com.example.Recipe18
```

**주의사항:**

- 이 타입의 정의는 `base.yml`에 머지되는 파일들(`my-service.yml`, `common.yml` 등)에만 포함합니다. `base.yml`에는 포함하지 않습니다.
- 파일 머지 후, `updateRecipeList[*].name`과 일치하는 `specs.openrewrite.org/v1beta/recipe` 타입의 recipe(일반적으로 `base.yml`에 정의된 트리거 recipe)를 찾아 해당 recipe의 `recipeList`를 업데이트합니다.
- `merge-rules.yml`의 `updateRecipeList`와 Recipe Definition 파일의 `org.yourcompany.openrewrite/v1/merge` 타입 모두 지원되며, 모든 규칙이 순서대로 적용됩니다.

### 5.2 일반 Recipe 정의 (specs.openrewrite.org/v1beta/recipe)

이 타입은 실제 OpenRewrite recipe를 정의합니다. **이 타입의 정의는 병합된 결과 파일에 포함됩니다.**

```yaml
---
type: specs.openrewrite.org/v1beta/recipe # string
name: com.yourorg.RecipeA # string
displayName: Recipe A # string, optional, 값이 없으면 출력하지 않습니다.
description: Applies Recipe A. # string, optional, 값이 없으면 출력하지 않습니다.
tags: # list of string, optional, 값이 없으면 출력하지 않습니다.
  - tag1
  - tag2
estimatedEffortPerOccurrence: PT15M # duration, optional, null 이면 출력하지 않습니다.
                                    # "PT20.345S" -- parses as "20.345 seconds"
                                    # "PT15M"     -- parses as "15 minutes" (where a minute is 60 seconds)
                                    # "PT10H"     -- parses as "10 hours" (where an hour is 3600 seconds)
                                    # "P2D"       -- parses as "2 days" (where a day is 24 hours or 86400 seconds)
                                    # "P2DT3H4M"  -- parses as "2 days, 3 hours and 4 minutes"
                                    # "P-6H3M"    -- parses as "-6 hours and +3 minutes"
                                    # "-P6H3M"    -- parses as "-6 hours and -3 minutes"
                                    # "-P-6H+3M"  -- parses as "+6 hours and -3 minutes"
causesAnotherCycle: false # boolean, optional, null이면 출력하지 않습니다.
preconditions: # arrays of recipes, optional, 값이 없으면 출력하지 않습니다.
  - org.openrewrite.text.Find:
      # map<String,Object> 형식, 비어 있으면 출력하지 않습니다.
      find: 1
exclusions: # arrays of recipes, optional, 값이 없으면 출력하지 않습니다.
  - org.openrewrite.text.Find:
      # map<String,Object> 형식, 비어 있으면 출력하지 않습니다.
      find: 1
recipeList: # arrays of recipes(List<Recipe>), optional, 값이 없으면 출력하지 않습니다.
  - org.openrewrite.text.ChangeText: # <---> Recipe{name:"org.openrewrite.text.ChangeText", attributes: {"toText": 2}}
      toText: 2
  - com.yourorg.RecipeB: # <---> Recipe{name:"com.yourorg.RecipeB", attributes: {"exampleConfig1": "foo","exampleConfig2": "bar"}}
      # map<String,Object> 형식, 비어 있으면 출력하지 않습니다.
      exampleConfig1: foo
      exampleConfig2: bar
  - com.yourorg.RecipeC # Recipe{name:"com.yourorg.RecipeC", attributes: {}}
```

### 5.3 문서 구분자

문서 구분자 `---`로 여러 개의 recipe definition을 구분합니다. jackson-databind-yaml을 사용하여 `---`가 누락되지 않도록 주의해야 합니다.

## 6. 완전한 예시

### 6.1 파일 구조

```
project-root/
├── pom.xml
└── migration-ci/
    ├── rules/
    │   └── merge-rules.yml
    └── recipes/
        ├── base.yml          # 기본 레시피 정의 및 트리거 recipe
        ├── my-service.yml    # base.yml에 머지되는 파일
        └── common.yml         # base.yml에 머지되는 파일
```

### 6.2 pom.xml 설정

```xml
<plugin>
    <groupId>com.yourcompany.plugins</groupId>
    <artifactId>rewrite-prepare-maven-plugin</artifactId>
    <version>1.0.0</version>
    <configuration>
        <recipeDirectory>${project.basedir}/migration-ci/recipes</recipeDirectory>
        <mergeRuleFile>${project.basedir}/migration-ci/rules/merge-rules.yml</mergeRuleFile>
        <outputFile>${project.basedir}/openrewrite/rewrite.yml</outputFile>
        <groupId>com.example</groupId>
        <artifactId>my-service</artifactId>
    </configuration>
</plugin>
```

### 6.3 migration-ci/rules/merge-rules.yml

```yaml
rules:
  - artifactId: my-service
    groupId: com.example
    mergeFiles:
      - base.yml          # 기본 레시피 정의 및 트리거 recipe
      - my-service.yml    # base.yml에 머지되는 파일
  - artifactId: '*-service'
    mergeFiles:
      - base.yml          # 기본 레시피 정의 및 트리거 recipe
      - common.yml        # base.yml에 머지되는 파일
```

### 6.4 migration-ci/recipes/base.yml

`base.yml`은 기본 레시피 정의와 트리거 recipe(최종 실행 recipe)를 정의하는 파일입니다. 이 파일에는 `org.yourcompany.openrewrite/v1/merge` 타입을 포함하지 않습니다.

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
  - com.example.Recipe2
  - com.example.Recipe3
```

### 6.5 migration-ci/recipes/my-service.yml

`my-service.yml`은 `base.yml`에 머지되어 최종 실행 recipe를 만드는 용도의 파일입니다. 이 파일에는 `org.yourcompany.openrewrite/v1/merge` 타입을 포함하여 자신의 레시피가 트리거 recipe(`com.example.Main`)의 `recipeList`에 어떻게 추가되는지 정의합니다.

```yaml
---
type: org.yourcompany.openrewrite/v1/merge
name: 머지 규칙
rules:
  - updateRecipeList:
      name: com.example.Main
      updateOrder:
        - last:
          - com.example.ServiceRecipe
---
type: specs.openrewrite.org/v1beta/recipe
name: com.example.ServiceRecipe
displayName: Service Recipe
recipeList:
  - com.example.ServiceRecipe1
  - com.example.ServiceRecipe2
```

### 6.6 migration-ci/recipes/common.yml

`common.yml`은 `base.yml`에 머지되어 최종 실행 recipe를 만드는 용도의 파일입니다. 이 파일에는 `org.yourcompany.openrewrite/v1/merge` 타입을 포함하여 자신의 레시피가 트리거 recipe(`com.example.Main`)의 `recipeList`에 어떻게 추가되는지 정의합니다.

```yaml
---
type: org.yourcompany.openrewrite/v1/merge
name: 머지 규칙
rules:
  - updateRecipeList:
      name: com.example.Main
      updateOrder:
        - first:
          - com.example.CommonRecipe
---
type: specs.openrewrite.org/v1beta/recipe
name: com.example.CommonRecipe
displayName: Common Recipe
recipeList:
  - com.example.CommonRecipe1
```

### 6.7 실행 결과 (openrewrite/rewrite.yml)

플러그인 실행 후 최종 머지 결과인 `openrewrite/rewrite.yml`에는:

- **`org.yourcompany.openrewrite/v1/merge` 타입은 포함되지 않습니다.** 이 타입은 `recipeList` 업데이트 규칙을 정의하는 용도로만 사용되며, 최종 출력 파일에는 포함되지 않습니다.
- **`specs.openrewrite.org/v1beta/recipe` 타입만 포함됩니다.** 이 타입의 recipe 정의만 최종 출력 파일에 저장됩니다.
- `recipeList`가 `updateOrder`에 따라 업데이트된 상태로 포함됩니다.

예상 결과:

```yaml
# Source: base.yml
---
type: specs.openrewrite.org/v1beta/recipe
name: com.example.Main
displayName: Main Recipe
description: Main recipe for migration
tags:
  - migration
  - base
recipeList:
  - com.example.CommonRecipe       # first (common.yml의 org.yourcompany.openrewrite/v1/merge 정의에 따름)
  - org.openrewrite.text.ChangeText:
      toText: 2
  - com.example.Recipe2
  - com.example.Recipe3
  - com.example.ServiceRecipe       # last (my-service.yml의 org.yourcompany.openrewrite/v1/merge 정의에 따름)
# Source: my-service.yml
---
type: specs.openrewrite.org/v1beta/recipe
name: com.example.ServiceRecipe
displayName: Service Recipe
recipeList:
  - com.example.ServiceRecipe1
  - com.example.ServiceRecipe2
# Source: common.yml
---
type: specs.openrewrite.org/v1beta/recipe
name: com.example.CommonRecipe
displayName: Common Recipe
recipeList:
  - com.example.CommonRecipe1
```

## 7. 용어 정리

- **Recipe Definition 파일**: `mergeFiles`에 지정된 YAML 파일. 여러 recipe definition을 포함할 수 있습니다.
- **base.yml**: 기본 레시피 정의와 트리거 recipe(최종 실행 recipe)를 정의하는 파일입니다. `org.yourcompany.openrewrite/v1/merge` 타입을 포함하지 않습니다.
- **머지 파일들**: `base.yml`에 머지되어 최종 실행 recipe를 만드는 용도의 파일들(`my-service.yml`, `common.yml` 등)입니다. `org.yourcompany.openrewrite/v1/merge` 타입을 포함하여 자신의 레시피가 트리거 recipe의 `recipeList`에 어떻게 추가되는지 정의합니다.
- **merge-rules.yml**: 프로젝트와 매칭할 규칙을 정의하는 설정 파일입니다.
- **org.yourcompany.openrewrite/v1/merge**: `recipeList` 업데이트 규칙을 정의하는 타입입니다. 머지 파일들에만 포함되며, 결과 파일에 포함되지 않습니다.
- **specs.openrewrite.org/v1beta/recipe**: 실제 OpenRewrite recipe를 정의하는 타입입니다. 결과 파일에 포함됩니다.
- **트리거 recipe**: `updateRecipeList.name`으로 지정되는 recipe로, 일반적으로 `base.yml`에 정의된 최종 실행 recipe입니다.
- **updateRecipeList**: 특정 recipe의 `recipeList`를 업데이트하는 규칙입니다.
- **updateOrder**: `recipeList`에 recipe를 추가하는 순서입니다 (`first`, `last`, `before`, `after`).
- **recipeDirectory**: Recipe Definition 파일들이 위치하는 디렉토리입니다.
- **mergeFiles**: 병합할 Recipe Definition 파일들의 목록입니다 (`recipeDirectory` 기준 상대 경로).
