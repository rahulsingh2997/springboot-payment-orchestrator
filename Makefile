.PHONY: docker-build docker-up docker-down docker-logs

docker-build:
	docker build -t springboot-payment-orchestrator:local .

docker-up:
	docker-compose up -d --build

docker-down:
	docker-compose down -v

docker-logs:
	docker-compose logs -f
