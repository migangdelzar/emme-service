# EMME Locust Load Tests

## Run
```bash
pip install -r requirements.txt
locust -f locustfile.py --host=http://localhost:8081
# Open http://localhost:8089 in browser
```

## Headless
```bash
locust -f locustfile.py --host=http://localhost:8081 --headless -u 100 -r 10 -t 5m
```

## Scenarios
| Scenario | Weight | Endpoint |
|----------|--------|----------|
| Health check | 3 | GET /actuator/health |
| List services | 5 | GET /api/services |
| List appointments | 5 | GET /api/appointments |
| List customers | 3 | GET /api/customers |
| Create appointment | 2 | POST /api/appointments |
| Search slots | 2 | GET /api/appointments/slots |
| AI chat | 1 | POST /api/ai/chat |
| Metrics | 1 | GET /actuator/prometheus |

## Target thresholds (from design spec)
- 95% API reads < 500ms
- 95% API writes < 1,000ms
- 100 concurrent users, < 1% errors
