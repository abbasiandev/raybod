#!/bin/bash
set -e
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "🚀 Quick CI/CD Test"
echo "==================="

if ! docker ps &> /dev/null; then
    echo -e "${RED}❌ Docker not running${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Docker running${NC}"

IMAGE="jangrewe/gitlab-ci-android"
echo "📦 Checking image: $IMAGE"

if docker images | grep -q "jangrewe/gitlab-ci-android"; then
    echo -e "${GREEN}✓ Image found${NC}"
else
    echo "Pulling image..."
    docker pull "$IMAGE" 2>&1 | tail -3
fi

echo ""
echo "📋 Validating YAML..."
python3 -c "import yaml; yaml.safe_load(open('.gitlab-ci.yml'))" && echo -e "${GREEN}✓ YAML valid${NC}" || exit 1

echo ""
echo "🔧 Testing Gradle..."
cd android && ./gradlew tasks --all 2>&1 | grep -q "assembleDebug" && echo -e "${GREEN}✓ Gradle tasks OK${NC}" || exit 1

echo ""
echo -e "${GREEN}✅ Configuration validated!${NC}"
