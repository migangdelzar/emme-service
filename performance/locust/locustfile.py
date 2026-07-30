from locust import HttpUser, task, between
import random
import uuid

class EmmeUser(HttpUser):
    wait_time = between(1, 3)
    # Auth token — set via environment or hardcoded for test
    # In real test: get token from Keycloak first

    def on_start(self):
        """Login and get auth token."""
        # Stub: in real test, POST to Keycloak token endpoint
        self.headers = {
            "Authorization": "Bearer test-token",
            "Content-Type": "application/json",
            "X-Tenant-Id": "00000000-0000-0000-0000-100000000000"
        }

    @task(3)
    def get_health(self):
        self.client.get("/actuator/health")

    @task(5)
    def list_services(self):
        self.client.get("/api/v1/services", headers=self.headers)

    @task(5)
    def list_appointments(self):
        self.client.get("/api/v1/appointments", headers=self.headers)

    @task(3)
    def list_customers(self):
        self.client.get("/api/v1/customers", headers=self.headers)

    @task(2)
    def create_appointment(self):
        payload = {
            "customerId": str(uuid.uuid4()),
            "serviceId": str(uuid.uuid4()),
            "artistId": str(uuid.uuid4()),
            "startsAt": "2026-07-05T10:00:00Z",
            "endsAt": "2026-07-05T11:00:00Z"
        }
        self.client.post("/api/v1/appointments", json=payload, headers=self.headers)

    @task(2)
    def search_slots(self):
        self.client.get(
            f"/api/v1/appointments/slots?serviceId={uuid.uuid4()}&date=2026-07-05",
            headers=self.headers
        )

    @task(1)
    def chat_ai(self):
        payload = {"userMessage": "What services do you offer?", "conversationContext": ""}
        self.client.post("/api/v1/ai/chat", json=payload, headers=self.headers)

    @task(1)
    def get_metrics(self):
        self.client.get("/actuator/prometheus")
