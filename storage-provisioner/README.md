1. The easiest way to get simple PVCs working on a one node cluster:

```
helm upgrade --install hostpath-provisioner --namespace kube-system rimusz/hostpath-provisioner
```

2. NFS is better for a real cluster, and mostly as easy as hostpath-provisioner.

```
 helm install stable/nfs-server-provisioner
```

Alternatively, NFS or other proper distributed filesystems can be used
out of the box.

After installing the NFS provisioner, run:

```

kubectl patch storageclass nfs -p '{"metadata": {"annotations":{"storageclass.kubernetes.io/is-default-class":"true"}}}'

```

Which will make dynamic volumes needed by ZK and so on provisioned
via NFS.


For Minio --- see the presto3-minio work, which includes helm instructions for minio alongside presto as well.
