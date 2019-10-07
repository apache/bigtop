echo "ok to cleanup?"
read x

kubectl delete ns minio
helm delete minio --purge



kubectl create ns minio ; 
# Sets up minio passwords...
kubectl -n minio create secret generic my-minio-secret --from-literal=accesskey=minio --from-literal=secretkey=minio123 

# Install minio
helm install --set existingSecret=my-minio-secret stable/minio --namespace=minio --name=minio

echo "installing via helm: minio"

# Install presto w/ minio configured...
kubectl create -f ./ -n minio
