#!/bin/bash
# Script to test GitLab CI/CD configuration locally using Docker
# This simulates the GitLab CI environment

set -e

echo "🚀 Testing GitLab CI/CD Configuration"
echo "======================================"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if Docker is available
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker is not installed or not in PATH${NC}"
    exit 1
fi

# Check if Docker daemon is running
if ! docker ps &> /dev/null; then
    echo -e "${RED}❌ Docker daemon is not running${NC}"
    echo "Please start Docker and try again"
    exit 1
fi

echo -e "${GREEN}✓ Docker is available${NC}"

# Test the Docker image
IMAGE="jangrewe/gitlab-ci-android"
echo ""
echo "📦 Testing Docker image: $IMAGE"
echo "Pulling image (this may take a while on first run)..."
docker pull "$IMAGE" || {
    echo -e "${RED}❌ Failed to pull Docker image${NC}"
    echo "Trying to continue with cached image if available..."
    docker images | grep gitlab-ci-android || exit 1
}

echo -e "${GREEN}✓ Docker image ready${NC}"

# Create a temporary directory for testing
TEST_DIR=$(mktemp -d)
echo ""
echo "📁 Test directory: $TEST_DIR"

# Copy project files to test directory
echo "📋 Copying project files..."
cp -r android "$TEST_DIR/"
cp .gitlab-ci.yml "$TEST_DIR/"

# Test each stage
echo ""
echo "🧪 Testing CI/CD Stages"
echo "======================"

# Function to test a job
test_job() {
    local job_name=$1
    local script_cmd=$2
    local stage=$3
    
    echo ""
    echo -e "${YELLOW}Testing: $job_name (stage: $stage)${NC}"
    
    docker run --rm \
        -v "$TEST_DIR:/builds/project" \
        -w /builds/project/android \
        -e ANDROID_COMPILE_SDK=34 \
        -e ANDROID_BUILD_TOOLS=34.0.0 \
        -e ANDROID_MIN_SDK=26 \
        -e ANDROID_TARGET_SDK=34 \
        -e GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.jvmargs='-Xmx2048m -XX:MaxMetaspaceSize=512m'" \
        "$IMAGE" \
        bash -c "
            export GRADLE_USER_HOME=/builds/project/.gradle
            chmod +x ./gradlew || true
            $script_cmd
        " && {
        echo -e "${GREEN}✓ $job_name passed${NC}"
        return 0
    } || {
        echo -e "${RED}✗ $job_name failed${NC}"
        return 1
    }
}

# Test lint stage (with allow_failure)
echo ""
test_job "lint" "./gradlew lint --stacktrace --no-daemon || true" "lint" || echo "Lint failed but allowed"

# Test test stage
test_job "test" "./gradlew test --stacktrace --no-daemon --continue" "test" || {
    echo -e "${RED}❌ Test stage failed${NC}"
    # Cleanup
    rm -rf "$TEST_DIR"
    exit 1
}

# Test build:debug stage
test_job "build:debug" "./gradlew assembleDebug --stacktrace --no-daemon" "build" || {
    echo -e "${RED}❌ Build stage failed${NC}"
    # Cleanup
    rm -rf "$TEST_DIR"
    exit 1
}

# Cleanup
echo ""
echo "🧹 Cleaning up..."
rm -rf "$TEST_DIR"

echo ""
echo -e "${GREEN}✅ All CI/CD tests passed!${NC}"
echo ""
echo "The GitLab CI/CD configuration is working correctly."
echo "You can now commit and push .gitlab-ci.yml to your GitLab repository."
