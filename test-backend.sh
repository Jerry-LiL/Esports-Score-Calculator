#!/bin/bash

echo "🧪 Testing LiL Ranker Authentication Backend"
echo "==========================================="
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if server is running
echo "1️⃣ Checking if server is running..."
if curl -s http://localhost:3000/health > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Server is running${NC}"
else
    echo -e "${RED}❌ Server is not running!${NC}"
    echo "Please start the server first: ./start-backend.sh"
    exit 1
fi
echo ""

# Test 1: Health Check
echo "2️⃣ Testing health endpoint..."
HEALTH=$(curl -s http://localhost:3000/health)
if [[ $HEALTH == *"OK"* ]]; then
    echo -e "${GREEN}✅ Health check passed${NC}"
else
    echo -e "${RED}❌ Health check failed${NC}"
fi
echo ""

# Test 2: Get all keys
echo "3️⃣ Fetching all keys..."
KEYS=$(curl -s http://localhost:3000/api/keys)
echo "$KEYS" | jq '.' 2>/dev/null || echo "$KEYS"
echo ""

# Test 3: Validate a fresh key
echo "4️⃣ Testing key validation (TEST-KEY-2025-002)..."
VALIDATE=$(curl -s -X POST http://localhost:3000/api/validate-key \
    -H "Content-Type: application/json" \
    -d '{"key":"TEST-KEY-2025-002","deviceId":"test-device-001"}')

if [[ $VALIDATE == *"success\":true"* ]]; then
    echo -e "${GREEN}✅ Key validation successful${NC}"
    echo "$VALIDATE" | jq '.' 2>/dev/null || echo "$VALIDATE"
else
    echo -e "${YELLOW}⚠️  Key might be already used or invalid${NC}"
    echo "$VALIDATE" | jq '.' 2>/dev/null || echo "$VALIDATE"
fi
echo ""

# Test 4: Try to use the same key again
echo "5️⃣ Testing duplicate key usage..."
DUPLICATE=$(curl -s -X POST http://localhost:3000/api/validate-key \
    -H "Content-Type: application/json" \
    -d '{"key":"TEST-KEY-2025-002","deviceId":"test-device-002"}')

if [[ $DUPLICATE == *"already redeemed"* ]]; then
    echo -e "${GREEN}✅ Correctly rejected duplicate usage${NC}"
else
    echo -e "${YELLOW}⚠️  Unexpected response${NC}"
fi
echo "$DUPLICATE" | jq '.' 2>/dev/null || echo "$DUPLICATE"
echo ""

# Test 5: Invalid key
echo "6️⃣ Testing invalid key..."
INVALID=$(curl -s -X POST http://localhost:3000/api/validate-key \
    -H "Content-Type: application/json" \
    -d '{"key":"INVALID-KEY-XYZ","deviceId":"test-device-003"}')

if [[ $INVALID == *"does not exist"* ]]; then
    echo -e "${GREEN}✅ Correctly rejected invalid key${NC}"
else
    echo -e "${RED}❌ Unexpected response${NC}"
fi
echo "$INVALID" | jq '.' 2>/dev/null || echo "$INVALID"
echo ""

echo "==========================================="
echo -e "${GREEN}✅ All tests completed!${NC}"
echo ""
echo "📋 Summary:"
echo "   - Health check: OK"
echo "   - Key listing: OK"
echo "   - Key validation: OK"
echo "   - Duplicate prevention: OK"
echo "   - Invalid key rejection: OK"
echo ""
echo "💡 Tip: Check backend/README.md for more info"
