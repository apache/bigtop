# Spark on Kubernetes

Volcano[1]:
```
$ cd $VOLCANO_HOME
$ kubectl apply -f installer/volcano-development.yaml

```
*TODO*
- Install Volcano via Helm

Spark operator[2]:
You can install spark operator on 'bigtop' namespace:
```
$ cd $BIGTOP_HOME

$ kubectl create -f spark/spark-rbac.yaml
$ helm repo add incubator http://storage.googleapis.com/kubernetes-charts-incubator
$ helm install incubator/sparkoperator --namespace bigtop -f spark/values.yaml \
--set enableBatchScheduler=true \
--set enableWebhook=true

$ kubectl get po -n bigtop

```

Running spark Pi examples:
```
$ kubectl apply -f spark/examples/spark-pi.yaml
$ kubectl describe sparkapplication -n bigtop spark-pi
......
$ kubectl logs -n bigtop spark-pi-driver
......

Pi is roughly 3.1405357026785135

```

Running Spark Pi example with Volcano scheduler:
```
kubectl apply -f spark/examples/spark-pi-batch.yaml
$ kubectl logs -n bigtop spark-pi-batch-driver

```
----

1. https://volcano.sh

2. https://github.com/GoogleCloudPlatform/spark-on-k8s-operator

