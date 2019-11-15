PRESTO_VERSION := latest

.PHONY: build 

build:
	docker build --squash --build-arg PRESTO_IMAGE=prestosql/presto:${PRESTO_VERSION} -t bigtop/presto-coordinator:${PRESTO_VERSION} coordinator
	docker build --squash --build-arg PRESTO_IMAGE=prestosql/presto:${PRESTO_VERSION} -t bigtop/presto-worker:${PRESTO_VERSION} worker

