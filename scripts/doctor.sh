#!/bin/bash
set -e
echo "=== EMME Platform Doctor ==="

# 1. Cluster
echo "☸️  Cluster:"
kubectl cluster-info 2>/dev/null | head -2 || echo "  ❌ No cluster"

# 2. Pods
echo "📦 Services:"
kubectl get pods -n emme-dev --no-headers 2>/dev/null | while read line; do
  name=$(echo "$line" | awk '{print $1}')
  ready=$(echo "$line" | awk '{print $2}')
  status=$(echo "$line" | awk '{print $3}')
  if [ "$ready" = "1/1" ] && [ "$status" = "Running" ]; then echo "  ✅ $name"
  else echo "  ❌ $name ($ready, $status)"; fi
done

# 3. Health
echo "🏥 Backend:"
curl -sf http://localhost:8081/actuator/health 2>/dev/null && echo "  ✅ UP" || echo "  ❌ Not reachable"

# 4. DB
echo "🗄️  Database:"
TABLES=$(kubectl exec -n emme-dev deploy/postgres -- psql -U emme -d emme -t -c "SELECT count(*) FROM information_schema.tables WHERE table_schema='public'" 2>/dev/null | tr -d ' ' || echo "0")
echo "  Tables: $TABLES"

# 5. pgvector
VEC=$(kubectl exec -n emme-dev deploy/postgres -- psql -U emme -d emme -t -c "SELECT count(*) FROM pg_extension WHERE extname='vector'" 2>/dev/null | tr -d ' ' || echo "0")
echo "  pgvector: $([ "$VEC" = "1" ] && echo '✅' || echo '❌')"

# 6. AI
echo "🤖 AI:"
curl -sf -X POST http://localhost:8081/api/ai/chat -H "Content-Type: application/json" -d '{"userMessage":"hi","conversationContext":""}' 2>/dev/null | head -c 60 && echo "" || echo "  ❌"

# 7. Catalog
echo "📋 Catalog:"
curl -sf -o /dev/null -w "  HTTP %{http_code}\n" http://localhost:8081/api/catalog/items 2>/dev/null || echo "  ❌"

echo "✅ Doctor complete"
