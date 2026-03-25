#!/bin/bash
# deploy.sh — Build and deploy SnapLink to local minikube
set -e

echo "==> Pointing Docker CLI at minikube's daemon..."
eval $(minikube docker-env)

echo "==> Building snaplink image inside minikube..."
cd "$(dirname "$0")/.."
docker build -t snaplink:latest .

echo "==> Applying K8s manifests (order matters: infra first, app last)..."
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/postgres.yaml
kubectl apply -f k8s/redis.yaml

echo "==> Waiting for Postgres and Redis to be ready..."
kubectl rollout status deployment/postgres -n snaplink
kubectl rollout status deployment/redis -n snaplink

echo "==> Deploying SnapLink app..."
kubectl apply -f k8s/app.yaml
kubectl rollout status deployment/snaplink-app -n snaplink

echo ""
echo "==> All pods:"
kubectl get pods -n snaplink
echo ""
echo "==> Services:"
kubectl get svc -n snaplink
echo ""
echo "==> To access SnapLink from your browser:"
echo "    kubectl port-forward svc/snaplink-app 8090:8080 -n snaplink"
echo "    Then open: http://localhost:8090"
