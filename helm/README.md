# Helm

```
$ curl -LO https://git.io/get_helm.sh
$ chmod 700 get_helm.sh
$ ./get_helm.sh

$ helm bersion
```

Add an account for deploy tiller:
```
$ kubectl --namespace kube-system create serviceaccount tiller
$ kubectl create clusterrolebinding tiller-cluster-rule \
 --clusterrole=cluster-admin --serviceaccount=kube-system:tiller
$ kubectl --namespace kube-system patch deploy tiller-deploy \
 -p '{"spec":{"template":{"spec":{"serviceAccount":"tiller"}}}}'
```

Initialize Helm:
```
$ helm init --history-max 200
```


