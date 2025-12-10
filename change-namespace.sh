#!/bin/bash

# namespace를 변경하는 스크립트
# com.yourcompany를 com.mycompany로 변경 (groupId, package, documents 등 모든 곳에서)
# 사용법: ./change-namespace.sh [새로운-namespace]
# 예: ./change-namespace.sh com.mycompany
# 인자가 없으면 기본값으로 com.mycompany를 사용합니다.

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 기본값 설정
OLD_NAMESPACE="com.yourcompany"
NEW_NAMESPACE="${1:-com.mycompany}"

# namespace 형식 검증 (간단한 검증)
if [[ ! "$NEW_NAMESPACE" =~ ^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)*$ ]]; then
    echo -e "${RED}오류: 잘못된 namespace 형식입니다.${NC}"
    echo "namespace는 소문자로 시작하고 점(.)으로 구분된 형식이어야 합니다."
    echo "예: com.mycompany"
    exit 1
fi

echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}namespace 변경 스크립트${NC}"
echo -e "${YELLOW}========================================${NC}"
echo ""
echo "기존 namespace: ${OLD_NAMESPACE}"
echo "새로운 namespace: ${NEW_NAMESPACE}"
echo ""

# 확인
read -p "계속하시겠습니까? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "취소되었습니다."
    exit 0
fi

# 프로젝트 루트 디렉토리
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_ROOT"

# 변경 통계
CHANGED_FILES=0
CHANGED_DIRS=0

echo ""
echo -e "${GREEN}변경 작업 시작...${NC}"
echo ""

# 1. pom.xml 파일들 변경
echo "1. pom.xml 파일들 변경 중..."
for pom_file in pom.xml src/test/resources/test-project/pom.xml; do
    if [ -f "$pom_file" ]; then
        if grep -q "$OLD_NAMESPACE" "$pom_file"; then
            if [[ "$OSTYPE" == "darwin"* ]]; then
                # macOS
                sed -i '' "s|$OLD_NAMESPACE|$NEW_NAMESPACE|g" "$pom_file"
            else
                # Linux
                sed -i "s|$OLD_NAMESPACE|$NEW_NAMESPACE|g" "$pom_file"
            fi
            echo "  ✓ $pom_file"
            CHANGED_FILES=$((CHANGED_FILES + 1))
        fi
    fi
done

# 2. Java 소스 파일의 package 및 import 변경
echo "2. Java 소스 파일들 변경 중..."
OLD_PACKAGE_PREFIX="$OLD_NAMESPACE"
NEW_PACKAGE_PREFIX="$NEW_NAMESPACE"

# Java 파일 찾기 및 변경
while IFS= read -r java_file; do
    if grep -q "$OLD_PACKAGE_PREFIX" "$java_file"; then
        if [[ "$OSTYPE" == "darwin"* ]]; then
            # macOS
            sed -i '' "s|$OLD_PACKAGE_PREFIX|$NEW_PACKAGE_PREFIX|g" "$java_file"
        else
            # Linux
            sed -i "s|$OLD_PACKAGE_PREFIX|$NEW_PACKAGE_PREFIX|g" "$java_file"
        fi
        echo "  ✓ $java_file"
        CHANGED_FILES=$((CHANGED_FILES + 1))
    fi
done < <(find src -name "*.java" -type f)

# 3. 디렉토리 구조 변경
echo "3. 디렉토리 구조 변경 중..."
OLD_DIR_PATH="src/main/java/$(echo $OLD_NAMESPACE | tr '.' '/')"
NEW_DIR_PATH="src/main/java/$(echo $NEW_NAMESPACE | tr '.' '/')"

if [ -d "$OLD_DIR_PATH" ]; then
    # 새 디렉토리 경로 생성
    NEW_DIR_PARENT="src/main/java/$(echo $NEW_NAMESPACE | tr '.' '/')"
    mkdir -p "$NEW_DIR_PARENT"
    
    # 기존 디렉토리 구조를 찾아서 이동
    if [ -d "$OLD_DIR_PATH/plugins" ]; then
        # com.yourcompany/plugins 구조인 경우
        if [ -d "$OLD_DIR_PATH/plugins/rewriteprepare" ]; then
            mkdir -p "$NEW_DIR_PATH/plugins"
            mv "$OLD_DIR_PATH/plugins/rewriteprepare" "$NEW_DIR_PATH/plugins/rewriteprepare" 2>/dev/null || true
            echo "  ✓ 디렉토리 이동: $OLD_DIR_PATH/plugins -> $NEW_DIR_PATH/plugins"
            CHANGED_DIRS=$((CHANGED_DIRS + 1))
        fi
        
        # 빈 디렉토리 정리 (하위부터 상위로)
        rmdir "$OLD_DIR_PATH/plugins" 2>/dev/null || true
    else
        # com.yourcompany 바로 아래에 있는 경우
        for subdir in "$OLD_DIR_PATH"/*; do
            if [ -d "$subdir" ]; then
                dirname=$(basename "$subdir")
                mkdir -p "$NEW_DIR_PATH"
                mv "$subdir" "$NEW_DIR_PATH/$dirname" 2>/dev/null || true
                echo "  ✓ 디렉토리 이동: $OLD_DIR_PATH/$dirname -> $NEW_DIR_PATH/$dirname"
                CHANGED_DIRS=$((CHANGED_DIRS + 1))
            fi
        done
    fi
    
    # 빈 디렉토리 정리
    rmdir "$OLD_DIR_PATH" 2>/dev/null || true
    OLD_PARENT="src/main/java/$(echo $OLD_NAMESPACE | cut -d'.' -f1)"
    if [ -d "$OLD_PARENT" ] && [ -z "$(ls -A "$OLD_PARENT")" ]; then
        rmdir "$OLD_PARENT" 2>/dev/null || true
    fi
fi

# 테스트 디렉토리도 변경
OLD_TEST_DIR_PATH="src/test/java/$(echo $OLD_NAMESPACE | tr '.' '/')"
NEW_TEST_DIR_PATH="src/test/java/$(echo $NEW_NAMESPACE | tr '.' '/')"

if [ -d "$OLD_TEST_DIR_PATH" ]; then
    mkdir -p "$NEW_TEST_DIR_PATH"
    
    if [ -d "$OLD_TEST_DIR_PATH/plugins" ]; then
        # com.yourcompany/plugins 구조인 경우
        if [ -d "$OLD_TEST_DIR_PATH/plugins/rewriteprepare" ]; then
            mkdir -p "$NEW_TEST_DIR_PATH/plugins"
            mv "$OLD_TEST_DIR_PATH/plugins/rewriteprepare" "$NEW_TEST_DIR_PATH/plugins/rewriteprepare" 2>/dev/null || true
            echo "  ✓ 테스트 디렉토리 이동: $OLD_TEST_DIR_PATH/plugins -> $NEW_TEST_DIR_PATH/plugins"
            CHANGED_DIRS=$((CHANGED_DIRS + 1))
        fi
        
        # 빈 디렉토리 정리
        rmdir "$OLD_TEST_DIR_PATH/plugins" 2>/dev/null || true
    else
        # com.yourcompany 바로 아래에 있는 경우
        for subdir in "$OLD_TEST_DIR_PATH"/*; do
            if [ -d "$subdir" ]; then
                dirname=$(basename "$subdir")
                mkdir -p "$NEW_TEST_DIR_PATH"
                mv "$subdir" "$NEW_TEST_DIR_PATH/$dirname" 2>/dev/null || true
                echo "  ✓ 테스트 디렉토리 이동: $OLD_TEST_DIR_PATH/$dirname -> $NEW_TEST_DIR_PATH/$dirname"
                CHANGED_DIRS=$((CHANGED_DIRS + 1))
            fi
        done
    fi
    
    # 빈 디렉토리 정리
    rmdir "$OLD_TEST_DIR_PATH" 2>/dev/null || true
    OLD_TEST_PARENT="src/test/java/$(echo $OLD_NAMESPACE | cut -d'.' -f1)"
    if [ -d "$OLD_TEST_PARENT" ] && [ -z "$(ls -A "$OLD_TEST_PARENT")" ]; then
        rmdir "$OLD_TEST_PARENT" 2>/dev/null || true
    fi
fi

# 4. 문서 파일 변경
echo "4. 문서 파일들 변경 중..."
for doc_file in docs/requirements.md README.md src/test/resources/test-project/README.md; do
    if [ -f "$doc_file" ]; then
        if grep -q "$OLD_NAMESPACE" "$doc_file"; then
            if [[ "$OSTYPE" == "darwin"* ]]; then
                sed -i '' "s|$OLD_NAMESPACE|$NEW_NAMESPACE|g" "$doc_file"
            else
                sed -i "s|$OLD_NAMESPACE|$NEW_NAMESPACE|g" "$doc_file"
            fi
            echo "  ✓ $doc_file"
            CHANGED_FILES=$((CHANGED_FILES + 1))
        fi
    fi
done

# 5. YAML 파일 변경
echo "5. YAML 파일들 변경 중..."
while IFS= read -r yaml_file; do
    if grep -q "$OLD_NAMESPACE" "$yaml_file"; then
        if [[ "$OSTYPE" == "darwin"* ]]; then
            sed -i '' "s|$OLD_NAMESPACE|$NEW_NAMESPACE|g" "$yaml_file"
        else
            sed -i "s|$OLD_NAMESPACE|$NEW_NAMESPACE|g" "$yaml_file"
        fi
        echo "  ✓ $yaml_file"
        CHANGED_FILES=$((CHANGED_FILES + 1))
    fi
done < <(find . -name "*.yml" -o -name "*.yaml" -type f -not -path "./target/*" -not -path "./.git/*")

# 6. 스크립트 파일 변경 (run-test.sh 등)
echo "6. 스크립트 파일들 변경 중..."
while IFS= read -r script_file; do
    if grep -q "$OLD_NAMESPACE" "$script_file"; then
        if [[ "$OSTYPE" == "darwin"* ]]; then
            sed -i '' "s|$OLD_NAMESPACE|$NEW_NAMESPACE|g" "$script_file"
        else
            sed -i "s|$OLD_NAMESPACE|$NEW_NAMESPACE|g" "$script_file"
        fi
        echo "  ✓ $script_file"
        CHANGED_FILES=$((CHANGED_FILES + 1))
    fi
done < <(find . -name "*.sh" -type f -not -path "./target/*" -not -path "./.git/*" -not -name "change-namespace.sh")

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}변경 완료!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "변경된 파일 수: $CHANGED_FILES"
echo "변경된 디렉토리 수: $CHANGED_DIRS"
echo ""
echo -e "${YELLOW}다음 단계:${NC}"
echo "1. 변경 사항을 확인하세요: git diff"
echo "2. 프로젝트를 다시 빌드하세요: mvn clean compile"
echo "3. 테스트를 실행하세요: mvn test"
echo ""

