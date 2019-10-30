## Use HostPath for persistent local storage with Kubernetes

https://github.com/rancher/local-path-provisioner

```
$ kubectl create -f $BIGTOP_HOME/storage/local-path/local-path-storage.yaml
$ kubectl get sc

```

Mark ```local-path``` StorageClass as default:
```
kubectl patch storageclass local-path -p '{"metadata": {"annotations":{"storageclass.kubernetes.io/is-default-class":"true"}}}'
```
