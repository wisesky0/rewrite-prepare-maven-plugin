# Rewrite Prepare Maven Plugin 요구사항 정의서

## 개요

이 플러그인은 `recipeDirectory`에 있는 OpenRewrite recipe YAML 파일들을 찾아 병합하고, 지정한 이름으로 레시피 정의를 찾아 `recipeList` 속성의 값을 편집합니다.

## 1. pom.xml 설정 예시

```xml
<plugin>
    <groupId>com.yourcompany.plugins</groupId>
    <artifactId>rewrite-prepare-maven-plugin</artifactId>
    <version>1.0.0</version>
    <configuration>
        <!-- recipeDirectory: 생략 가능하며, 기본값은 ${project.basedir}입니다. CLI 변수명은 rewrite-prepare.recipeDirectory -->
        <recipeDirectory>${project.basedir}/migration-ci/recipes</recipeDirectory>
        
        <!-- mergeRuleFile: 생략 가능하며, 기본값은 merge-rules.yml입니다. CLI 변수명은 rewrite-prepare.mergeRuleFile -->
        <mergeRuleFile>${project.basedir}/migration-ci/merge-rules.yml</mergeRuleFile>
        
        <!-- outputFile: 생략 가능하며, 기본값은 openrewrite/rewrite.yml입니다. CLI 변수명은 rewrite-prepare.outputFile -->
        <outputFile>${project.basedir}/openrewrite/rewrite.yml</outputFile>
        
        <!-- groupId: 생략 가능하며, 기본값은 project.groupId입니다. CLI 변수명은 rewrite-prepare.groupId -->
        <groupId>com.example</groupId>
        
        <!-- artifactId: 생략 가능하며, 기본값은 project.artifactId입니다. CLI 변수명은 rewrite-prepare.artifactId -->
        <artifactId>my-service</artifactId>
    </configuration>
</plugin>
```

## 2. Recipe Definition 형식

문서 구분자 `---`로 여러 개의 recipe definition이 등록되어 있습니다.
jackson-databind-yaml을 사용하여 `---`가 누락되지 않도록 주의해야 합니다.

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
      # map<String,Object> 형식, 비어있으면 출력하지 않습니다.
      find: 1
exclusions: # arrays of recipes, optional, 값이 없으면 출력하지 않습니다.
  - org.openrewrite.text.Find:
      # map<String,Object> 형식, 비어있으면 출력하지 않습니다.
      find: 1
recipeList: # arrays of recipes(List<Recipe>), optional, 값이 없으면 출력하지 않습니다.
  - org.openrewrite.text.ChangeText: # <---> Recipe{name:"org.openrewrite.text.ChangeText", attributes: {"toText": 2}}
      toText: 2
  - com.yourorg.RecipeB: # <---> Recipe{name:"com.yourorg.RecipeB", attributes: {"exampleConfig1": "foo","exampleConfig2": "bar"}}
      # map<String,Object> 형식, 비어있으면 출력하지 않습니다.
      exampleConfig1: foo
      exampleConfig2: bar
  - com.yourorg.RecipeC # Recipe{name:"com.yourorg.RecipeB", attributes: {"exampleConfig1": "foo","exampleConfig2": "bar"}}
```

## 3. 머지 규칙 파일

rewrite-prepare-maven-plugin에서 지정한 `groupId`, `artifactId`와 `rules`의 `artifactId`, `groupId`를 매핑하여 선택된 머지 규칙에 따라 하나의 recipe YAML 파일을 생성합니다.

### 3.1 처리 순서

선택된 머지 규칙이 여러 개인 경우:

1. 모든 규칙의 `mergeFiles`를 수행합니다. 중복된 파일 요청은 skip합니다. 처리 내역은 머지, 스킵 모두 info 레벨로 출력합니다.
2. 모든 규칙의 `updateRecipeList`를 순서대로 수행하며, `recipeList`에 이미 있는 recipe를 처리하려 하면 warning을 출력한 후 skip합니다. 처리 결과는 info 레벨로 출력합니다.
3. 머지된 결과를 출력합니다.

### 3.2 YAML 형식 (merge-rules.yml)

```yaml
rules:
  - artifactId: myaa-svc # glob 패턴을 사용하여, '*-svc'와 같이 작성할 수도 있습니다. 필수속성으로 없으면 오류발생시킨다.
    groupId: com.example  # groupId는 생략 가능하며, 생략된 경우 '*'와 같습니다.
    mergeFiles: # recipeDirectory 하위의 상대 경로입니다. 필수속성으로 없거나 비어 있으면 오류발생시킨다.
      - base.yml
      - migration-ci/recipes/myaa-svc.yml
    updateRecipeList:
      # name: recipeList를 수정할 레시피 정의의 이름. 필수속성으로 없거나 비어 있으면 오류발생시킨다.
      name: com.example.Main
      updateOrder: 
        # first: com.example.Main 레시피 정의를 찾아 recipeList의 가장 앞에 삽입합니다.
        - first:
            # List<UpdateEntry>
          - com.example.Recipe1  # UpdateEntry{ key: null, values: ["com.example.Recipe5"] }
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
  - artifactId: '*-service'
    mergeFiles:
      - base.yml
      - common.yml
    updateRecipeList:
      updateOrder:
        - first: 
          - com.example.Recipe11  
        - last: 
          - com.example.Recipe20  
        - before: 
            com.example.Recipe4: 
              - com.example.Recipe6
            com.example.Recipe14:
              - com.example.Recipe16
        - after: 
            com.example.Recipe7:  
              - com.example.Recipe9
            com.example.Recipe17: 
              - com.example.Recipe19
```
