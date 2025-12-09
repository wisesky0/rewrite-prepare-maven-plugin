#!/bin/bash

# groupId와 package를 변경하는 스크립트
# 사용법: ./change-groupid.sh <새로운-groupId>
# 예: ./change-groupid.sh com.mycompany.plugins

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 인자 확인
if [ $# -eq 0 ]; then
    echo -e "${RED}오류: 새로운 groupId를 인자로 제공해야 합니다.${NC}"
    echo "사용법: $0 <새로운-groupId>"
    echo "예: $0 com.mycompany.plugins"
    exit 1
fi

OLD_GROUP_ID="com.yourcompany.plugins"
NEW_GROUP_ID="$1"

# groupId 형식 검증 (간단한 검증)
if [[ ! "$NEW_GROUP_ID" =~ ^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)*$ ]]; then
    echo -e "${RED}오류: 잘못된 groupId 형식입니다.${NC}"
    echo "groupId는 소문자로 시작하고 점(.)으로 구분된 형식이어야 합니다."
    echo "예: com.mycompany.plugins"
    exit 1
fi

echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}groupId 및 package 변경 스크립트${NC}"
echo -e "${YELLOW}========================================${NC}"
echo ""
echo "기존 groupId: ${OLD_GROUP_ID}"
echo "새로운 groupId: ${NEW_GROUP_ID}"
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

# 1. pom.xml의 groupId 변경
echo "1. pom.xml 파일들 변경 중..."
for pom_file in pom.xml src/test/resources/test-project/pom.xml; do
    if [ -f "$pom_file" ]; then
        if grep -q "$OLD_GROUP_ID" "$pom_file"; then
            if [[ "$OSTYPE" == "darwin"* ]]; then
                # macOS
                sed -i '' "s|$OLD_GROUP_ID|$NEW_GROUP_ID|g" "$pom_file"
            else
                # Linux
                sed -i "s|$OLD_GROUP_ID|$NEW_GROUP_ID|g" "$pom_file"
            fi
            echo "  ✓ $pom_file"
            CHANGED_FILES=$((CHANGED_FILES + 1))
        fi
    fi
done

# 2. Java 소스 파일의 package 및 import 변경
echo "2. Java 소스 파일들 변경 중..."
OLD_PACKAGE="com.yourcompany.plugins"
NEW_PACKAGE="$NEW_GROUP_ID"

# Java 파일 찾기 및 변경
while IFS= read -r java_file; do
    if grep -q "$OLD_PACKAGE" "$java_file"; then
        if [[ "$OSTYPE" == "darwin"* ]]; then
            # macOS
            sed -i '' "s|$OLD_PACKAGE|$NEW_PACKAGE|g" "$java_file"
        else
            # Linux
            sed -i "s|$OLD_PACKAGE|$NEW_PACKAGE|g" "$java_file"
        fi
        echo "  ✓ $java_file"
        CHANGED_FILES=$((CHANGED_FILES + 1))
    fi
done < <(find src -name "*.java" -type f)

# 3. 디렉토리 구조 변경
echo "3. 디렉토리 구조 변경 중..."
OLD_DIR_PATH="src/main/java/com/yourcompany/plugins"
NEW_DIR_PATH="src/main/java/$(echo $NEW_GROUP_ID | tr '.' '/')"

if [ -d "$OLD_DIR_PATH" ]; then
    # 새 디렉토리 경로 생성
    NEW_DIR_PARENT="src/main/java/$(echo $NEW_GROUP_ID | tr '.' '/')"
    mkdir -p "$NEW_DIR_PARENT"
    
    # 디렉토리 이동
    if [ -d "$OLD_DIR_PATH/rewriteprepare" ]; then
        mv "$OLD_DIR_PATH/rewriteprepare" "$NEW_DIR_PATH/rewriteprepare" 2>/dev/null || true
        
        # 빈 디렉토리 정리 (하위부터 상위로)
        rmdir "$OLD_DIR_PATH" 2>/dev/null || true
        rmdir "src/main/java/com/yourcompany" 2>/dev/null || true
        rmdir "src/main/java/com" 2>/dev/null || true
        
        echo "  ✓ 디렉토리 이동: $OLD_DIR_PATH -> $NEW_DIR_PATH"
        CHANGED_DIRS=$((CHANGED_DIRS + 1))
    fi
fi

# 테스트 디렉토리도 변경
OLD_TEST_DIR_PATH="src/test/java/com/yourcompany/plugins"
NEW_TEST_DIR_PATH="src/test/java/$(echo $NEW_GROUP_ID | tr '.' '/')"

if [ -d "$OLD_TEST_DIR_PATH" ]; then
    mkdir -p "$NEW_TEST_DIR_PATH"
    
    if [ -d "$OLD_TEST_DIR_PATH/rewriteprepare" ]; then
        mv "$OLD_TEST_DIR_PATH/rewriteprepare" "$NEW_TEST_DIR_PATH/rewriteprepare" 2>/dev/null || true
        
        # 빈 디렉토리 정리
        rmdir "$OLD_TEST_DIR_PATH" 2>/dev/null || true
        rmdir "src/test/java/com/yourcompany" 2>/dev/null || true
        rmdir "src/test/java/com" 2>/dev/null || true
        
        echo "  ✓ 테스트 디렉토리 이동: $OLD_TEST_DIR_PATH -> $NEW_TEST_DIR_PATH"
        CHANGED_DIRS=$((CHANGED_DIRS + 1))
    fi
fi

# 4. 문서 파일 변경
echo "4. 문서 파일들 변경 중..."
for doc_file in docs/requirements.md src/test/resources/test-project/README.md; do
    if [ -f "$doc_file" ]; then
        if grep -q "$OLD_GROUP_ID" "$doc_file"; then
            if [[ "$OSTYPE" == "darwin"* ]]; then
                sed -i '' "s|$OLD_GROUP_ID|$NEW_GROUP_ID|g" "$doc_file"
            else
                sed -i "s|$OLD_GROUP_ID|$NEW_GROUP_ID|g" "$doc_file"
            fi
            echo "  ✓ $doc_file"
            CHANGED_FILES=$((CHANGED_FILES + 1))
        fi
    fi
done

# 5. 스크립트 파일 변경 (run-test.sh 등)
echo "5. 스크립트 파일들 변경 중..."
while IFS= read -r script_file; do
    if grep -q "$OLD_GROUP_ID" "$script_file"; then
        if [[ "$OSTYPE" == "darwin"* ]]; then
            sed -i '' "s|$OLD_GROUP_ID|$NEW_GROUP_ID|g" "$script_file"
        else
            sed -i "s|$OLD_GROUP_ID|$NEW_GROUP_ID|g" "$script_file"
        fi
        echo "  ✓ $script_file"
        CHANGED_FILES=$((CHANGED_FILES + 1))
    fi
done < <(find . -name "*.sh" -type f -not -path "./target/*" -not -path "./.git/*")

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

