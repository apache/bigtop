# Log aggregation for Kubernetes cluster

Deploy Loki stack:
```
$ helm repo add loki https://grafana.github.io/loki/charts
$ helm repo update
$ helm upgrade --install --namespace default loki -f logs/loki/loki-values.yaml loki/loki
$ helm upgrade --install --namespace default promtail -f logs/loki/promtail-values.yaml loki/promtail --set "loki.serviceName=loki"
```

## Loki in Grafana
You should add the Loki data source to Grafana configuration.

1. In Grafana, go to Configuration > Data Sources
2. Go to ```Add data source```
3. Choose Loki --> The http URL field ```http://loki:3100```

## Querying logs from Loki on Grafana

1. Explore > Choose 'Loki' data source
2. Enter a Liki query. E.g., ```{app="zookeeper",namespace="bigtop"}```
3. You can see the log streams from Zookeeper Pods on Kubernetes cluster 
