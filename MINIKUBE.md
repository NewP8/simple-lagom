 Minikube
==========

1. avviare minikube

2. applicare
  - rbac
  - postgres
  - simple

3. Kong
  - kubectl create -f https://bit.ly/k4k8s
  - export PROXY_IP=$(minikube service -n kong kong-proxy --url | head -1)
  - echo $PROXY_IP
  - applicare ong/ingress

3. lanciare
  - load balancer con minikube service <nome>
  - da ingress da PROXY_IP
