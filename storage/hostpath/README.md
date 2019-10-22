# Install dynamic hostpath provisioner Helm chart

```
$ helm repo add rimusz https://charts.rimusz.net
$ helm repo update
$ helm upgrade --install hostpath-provisioner \
--namespace kube-system \
--set storageClass.defaultClass=false \
rimusz/hostpath-provisioner

```

Mark ```hostpath``` StorageClass as default:
```
kubectl patch storageclass hostpath -p '{"metadata": {"annotations":{"storageclass.kubernetes.io/is-default-class":"true"}}}'
```
