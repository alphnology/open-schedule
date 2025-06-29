.PHONY: help build image image-amd64 clean logs restart build-image build-image-amd64 build-push-image build-push-image-amd64

default: help

PROJECT_VERSION := $(shell mvn help:evaluate -q -DforceStdout -D"expression=project.version")
PROJECT_NAME=$(shell mvn help:evaluate -q -DforceStdout -D"expression=project.name")
PROJECT_DOCKER_REPOSITORY=alphnology/${PROJECT_NAME}

# COLORS
GREEN  := $(shell tput -Txterm setaf 2)
YELLOW := $(shell tput -Txterm setaf 3)
WHITE  := $(shell tput -Txterm setaf 7)
RESET  := $(shell tput -Txterm sgr0)


TARGET_MAX_CHAR_NUM=22
## Show this help.
help:
	@echo ''
	@echo '       ${YELLOW}Project ${GREEN}${PROJECT_NAME}${RESET}'
	@echo ''
	@echo 'Usage:'
	@echo '  ${YELLOW}make${RESET} ${GREEN}<target>${RESET}'
	@echo ''
	@echo 'Targets:'
	@awk '/^[a-zA-Z\-\_0-9]+:/ { \
		helpMessage = match(lastLine, /^## (.*)/); \
		if (helpMessage) { \
			helpCommand = substr($$1, 0, index($$1, ":")-1); \
			helpMessage = substr(lastLine, RSTART + 3, RLENGTH); \
			printf "  ${YELLOW}%-$(TARGET_MAX_CHAR_NUM)s${RESET} ${GREEN}%s${RESET}\n", helpCommand, helpMessage; \
		} \
	} \
	{ lastLine = $$0 }' $(MAKEFILE_LIST)

PROJECT_DEPENDENCY=dependency:go-offline -Pproduction
PROJECT_CLEAN_PACKAGE=clean package -DskipTests -Pproduction

## Clean and build the project.
build:
	@echo '${GREEN}Building the project: ${RESET}'$(PROJECT_NAME)
ifeq ($(OS),Windows_NT)
	@echo '${YELLOW}OS:${RESET}'$(OS)
	@.\mvnw.cmd ${PROJECT_DEPENDENCY}
	@.\mvnw.cmd ${PROJECT_CLEAN_PACKAGE}
else
	@echo '${YELLOW}OS: ${RESET}'$(shell uname)
	@./mvnw ${PROJECT_DEPENDENCY}
	@./mvnw ${PROJECT_CLEAN_PACKAGE}
endif


## Create the docker image
image:
	@echo '${GREEN}building ${RESET}'$(PROJECT_NAME)
	@echo '${GREEN}PROJECT_DOCKER_REPOSITORY ${RESET}'$(PROJECT_DOCKER_REPOSITORY):$(PROJECT_VERSION)
	@docker build -t ${PROJECT_DOCKER_REPOSITORY} .

## Create the docker image for amd64 platform.
image-amd64:
	@echo '${GREEN}building ${RESET}'$(PROJECT_NAME)
	@echo '${GREEN}PROJECT_DOCKER_REPOSITORY ${RESET}'$(PROJECT_DOCKER_REPOSITORY):$(PROJECT_VERSION)
ifeq ($(OS),Windows_NT)
	@docker build -t ${PROJECT_DOCKER_REPOSITORY} .
else
	@docker buildx build --platform linux/amd64 -o type=docker -t ${PROJECT_DOCKER_REPOSITORY}:${PROJECT_VERSION} .
endif

## Clean docker images and restart containers.
clean:
	@echo '${GREEN}Clean docker image ${RESET}'$(PROJECT_NAME)
	docker compose down -v
	docker compose up -d

## Show logs from Docker containers.
logs:
	@echo '${GREEN}Show logs docker image ${RESET}'$(PROJECT_NAME)
	@docker compose logs -f

## Clean and restart docker containers and run the application.
restart: clean
	@echo '${GREEN}Restart docker image ${RESET}'$(PROJECT_NAME)
ifeq ($(OS),Windows_NT)
	@.\mvnw.cmd
else
	@./mvnw
endif

## Build the project and create the docker image.
build-image: build image

## Build the project and create the docker image for amd64 platform.
build-image-amd64: build image-amd64

## Build, create, and push the docker image to DockerHub.
build-push-image: build-image
	@echo Pushing image to ECR
	@echo '${GREEN}PROJECT_DOCKER_REPOSITORY ${RESET}'$(PROJECT_DOCKER_REPOSITORY):${PROJECT_VERSION}
	@docker push ${PROJECT_DOCKER_REPOSITORY}:${PROJECT_VERSION}

## Build, create, and push the amd64 docker image to DockerHub.
build-push-image-amd64: build-image-amd64
	@echo Pushing image to ECR
	@echo '${GREEN}PROJECT_DOCKER_REPOSITORY ${RESET}'$(PROJECT_DOCKER_REPOSITORY):${PROJECT_VERSION}
	@docker push ${PROJECT_DOCKER_REPOSITORY}:${PROJECT_VERSION}

