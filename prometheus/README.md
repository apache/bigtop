# Prometheus Operator

https://github.com/helm/charts/tree/master/stable/prometheus-operator


Prerequisites:
- Kubernetes
- Helm

Deploy Prometheus operator:
```
$ cd $BIGTOP_HOME
$ helm install --namespace default --name prometheus -f prometheus/values.yaml stable/prometheus-operator
 
```

Port forward for Grafana:
```
$ kubectl port-forward $(kubectl get pods --selector=app=grafana --output=jsonpath="{.items..metadata.name}") 3000
```

Get Grafana usernamd and password:
```
$ kubectl get secrets --all-namespaces | grep grafana | grep Opaque
$ kubectl get secret --namespace default <secret name> -o jsonpath="{.data.admin-password}" | base64 --decode ; echo
```

Browse http://localhost:3000 for Grafana.

Port forward for Prometheus:
```
$ kubectl port-forward --namespace default <prometheus pod name> 9090

```

Browse http://localhost:9090 for Prometheus web UI.
