build:
	docker build -t dns-server:latest . -f Dockerfile

run:
	docker run --read-only -p 53:53/udp dns-server:latest
