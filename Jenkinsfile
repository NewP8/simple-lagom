pipeline {
  agent any
  options {
    timestamps ()
    parallelsAlwaysFailFast()
    timeout (time: 1, unit: 'HOURS')
  }
  environment {

    FULL_SERVICE_VERSION = "${sh(script:'git rev-parse --short HEAD', returnStdout: true).trim()}"
    SERVICE_VERSION = "v${FULL_SERVICE_VERSION}"
    GIT_BRANCH = "${sh(script:'git rev-parse --abbrev-ref HEAD', returnStdout: true).trim()}"
    BUILD_VERSION = "${SERVICE_VERSION}" // "0.0.1"
    GOOGLE_PROJECT_NAME = 'fintech-266508'
    GOOGLE_ZONE_NAME = 'europe-west1-b'
    // GOOGLE_TEST_CLUSTER_NAME = "simple-${SERVICE_VERSION}"
    GOOGLE_TEST_CLUSTER_NAME = "simple-vedf2928" // ${SERVICE_VERSION}"
    // GOOGLE_CLUSTER_NAME = 'gke_${GOOGLE_PROJECT_NAME}_${GOOGLE_ZONE_NAME}_${GOOGLE_TEST_CLUSTER_NAME}'
    APPS_NAMESPACE = 'simple-ns'
    SIMPLE_IMAGE = "gcr.io/${GOOGLE_PROJECT_NAME}/simple-impl:${SERVICE_VERSION}"
  }
  stages {

    // BUILD
    stage ('Build') {
      steps {
        sh 'sbt compile'
      }
    }

    // DOCKER BUILD e PUSH
    stage('Containerization') {
      steps {
        sh 'sbt clean docker:publishLocal'
        sh 'docker tag simple-impl:latest ${SIMPLE_IMAGE}'
        sh 'gcloud docker -- push ${SIMPLE_IMAGE}'
      }
    }

    // CLUSTER
    stage('Cluster creation') {
      steps {
//         sh 'gcloud container clusters create ${GOOGLE_TEST_CLUSTER_NAME} \
//           --cluster-version=latest \
//           --zone=${GOOGLE_ZONE_NAME} \
//           --project ${GOOGLE_PROJECT_NAME} \
//           --machine-type=e2-standard-4 \
//           --num-nodes=2'
        sh 'gcloud container clusters get-credentials ${GOOGLE_TEST_CLUSTER_NAME} --project ${GOOGLE_PROJECT_NAME} --zone ${GOOGLE_ZONE_NAME}'
        // sh 'kubectl create namespace ${APPS_NAMESPACE}'
        // sh 'kubectl apply -f deploy/kubernetes/resources/storage-class-slow.yaml'
        sh 'kubectl apply -f deploy/kubernetes/resources/rbac.yaml -n ${APPS_NAMESPACE}'
        // sh "kubectl create secret generic simple-secret --from-literal=db-password=simpledb -n ${APPS_NAMESPACE}"
      }
    }

    stage('simple') {
      steps {
        // sh 'kubectl apply --cluster gke_${GOOGLE_PROJECT_NAME}_${GOOGLE_ZONE_NAME}_${GOOGLE_TEST_CLUSTER_NAME} -f deploy/kubernetes/resources/simple/postgres-simple.yaml -n ${APPS_NAMESPACE}'
        sh 'sed -i "s#REPLACE_WITH_IMAGE_NAME#${SIMPLE_IMAGE}#g" deploy/kubernetes/resources/simple/simple.yaml'
        sh 'sleep 4s'
        sh 'kubectl apply -f deploy/kubernetes/resources/simple/simple.yaml -n ${APPS_NAMESPACE}'
      }
    }

    stage('ingress') {
      steps {
//         sh 'helm repo add nginx-stable https://helm.nginx.com/stable'
//         sh 'helm repo update'
//         sh 'helm install nginx-ingress nginx-stable/nginx-ingress'
        sh 'kubectl apply -f deploy/kubernetes/resources/kong/simple-ingress.yaml -n ${APPS_NAMESPACE}'
      }
    }
  }
}
