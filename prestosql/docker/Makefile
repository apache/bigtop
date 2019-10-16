PRESTO_VERSION := latest

.PHONY: build 

build:
	docker build --squash --build-arg PRESTO_IMAGE=prestosql/presto:${PRESTO_VERSION} -t bigtop/prestosql-coordinator:${PRESTO_VERSION} coordinator
	docker build --squash --build-arg PRESTO_IMAGE=prestosql/presto:${PRESTO_VERSION} -t bigtop/prestosql-worker:${PRESTO_VERSION} worker

