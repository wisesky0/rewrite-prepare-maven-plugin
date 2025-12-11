#!/bin/bash

# Rewrite Prepare Maven Plugin 테스트 실행 스크립트
#
# 이 스크립트는 test-project에서 플러그인을 테스트하기 위해 사용됩니다.
# 
# 처리 과정:
# 1. 플러그인을 빌드하고 로컬 Maven 저장소에 설치
# 2. test-project에서 prepare goal 실행 (recipe 병합 및 업데이트)
# 3. test-project에서 resolve goal 실행 (변수 치환)
# 4. 생성된 openrewrite/rewrite.yml 파일 확인
#
# 파일 구조:
# - migration-ci/rules/merge-rules.yml: 머지 규칙 파일
# - migration-ci/rules/var-map.properties: 변수 맵 파일
# - migration-ci/recipes/base.yml: 기본 레시피 정의 및 트리거 recipe
# - migration-ci/recipes/my-service.yml: base.yml에 머지되는 파일
# - migration-ci/recipes/common.yml: base.yml에 머지되는 파일
# - openrewrite/rewrite.yml: 플러그인 실행 후 생성되는 출력 파일

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEST_PROJECT_DIR="$SCRIPT_DIR"

# 플러그인 루트 디렉토리 찾기 (pom.xml이 있는 디렉토리)
# 방법 1: 상대 경로로 찾기 (../../../../pom.xml)
PLUGIN_DIR="$(cd "$SCRIPT_DIR/../../../../" && pwd)"
if [ ! -f "$PLUGIN_DIR/pom.xml" ]; then
    # 방법 2: 현재 디렉토리에서 pom.xml 찾기
    CURRENT_DIR="$SCRIPT_DIR"
    while [ "$CURRENT_DIR" != "/" ]; do
        if [ -f "$CURRENT_DIR/pom.xml" ]; then
            PLUGIN_DIR="$CURRENT_DIR"
            break
        fi
        CURRENT_DIR="$(dirname "$CURRENT_DIR")"
    done
fi

# pom.xml 확인
if [ ! -f "$PLUGIN_DIR/pom.xml" ]; then
    echo "오류: 플러그인 루트 디렉토리(pom.xml)를 찾을 수 없습니다."
    exit 1
fi

echo "=========================================="
echo "Rewrite Prepare Maven Plugin 테스트"
echo "=========================================="
echo ""

# 1. 플러그인 빌드 및 설치
echo "1. 플러그인 빌드 및 설치 중..."
mvn -f "$PLUGIN_DIR/pom.xml" clean install -DskipTests
echo "✓ 플러그인 설치 완료"
echo ""

# 2. 테스트 프로젝트 확인
echo "2. 테스트 프로젝트 확인..."
echo "✓ 테스트 프로젝트 디렉토리: $TEST_PROJECT_DIR"
echo ""

# 3. 기존 출력 파일 삭제 (있는 경우)
OUTPUT_FILE="$TEST_PROJECT_DIR/openrewrite/rewrite.yml"
if [ -f "$OUTPUT_FILE" ]; then
    echo "3. 기존 출력 파일 삭제..."
    rm -f "$OUTPUT_FILE"
    echo "✓ 기존 파일 삭제 완료"
    echo ""
fi

# 4. recipe 병합 및 업데이트 실행 (prepare goal)
# 이 단계에서 변수 placeholder가 유지된 채로 머지됩니다.
echo "4. recipe 병합 및 업데이트 실행 중 (prepare goal)..."
mvn -f "$TEST_PROJECT_DIR/pom.xml" rewrite-prepare:prepare
echo ""

# 5. 변수 치환 실행 (resolve goal)
# prepare goal에서 생성된 outputFile의 변수를 치환합니다.
echo "5. 변수 치환 실행 중 (resolve goal)..."
VAR_MAP_FILE="$TEST_PROJECT_DIR/migration-ci/rules/var-map.properties"
if [ -f "$VAR_MAP_FILE" ]; then
    if [ -f "$OUTPUT_FILE" ]; then
        echo "--- 변수 치환 전 (처음 10줄) ---"
        head -10 "$OUTPUT_FILE" | grep -E "\$\{|MyApplication" || echo "변수 확인 중..."
        echo ""
        
        mvn -f "$TEST_PROJECT_DIR/pom.xml" rewrite-prepare:resolve
        echo "✓ 변수 치환 완료"
        
        echo ""
        echo "--- 변수 치환 후 (처음 10줄) ---"
        head -10 "$OUTPUT_FILE" | grep -E "\$\{|MyApplication" || echo "변수 치환 확인 중..."
    else
        echo "⚠ 출력 파일이 없습니다. prepare goal을 먼저 실행해야 합니다."
    fi
else
    echo "⚠ 변수 맵 파일이 없습니다: $VAR_MAP_FILE"
    echo "  변수 치환을 건너뜁니다."
fi
echo ""

# 6. 결과 확인
echo "6. 결과 확인..."
if [ -f "$OUTPUT_FILE" ]; then
    echo "✓ 출력 파일 생성됨: $OUTPUT_FILE"
    echo ""
    echo "=== 생성된 파일 내용 (처음 50줄) ==="
    head -50 "$OUTPUT_FILE"
    echo ""
    echo "... (나머지 생략) ..."
    echo ""
    echo "=== 테스트 완료 ==="
else
    echo "✗ 출력 파일이 생성되지 않았습니다: $OUTPUT_FILE"
    exit 1
fi

