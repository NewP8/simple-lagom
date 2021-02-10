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
    GOOGLE_TEST_CLUSTER_NAME = "simple-${SERVICE_VERSION}"
    GOOGLE_TEST_CLUSTER_VERSION = 'latest'
    GOOGLE_TEST_MACHINE_TYPE = 'e2-standard-4'
    GOOGLE_TEST_NUM_NODES = '2'
    APPS_NAMESPACE = 'kafka'
    SIMPLE_IMAGE = "gcr.io/${GOOGLE_PROJECT_NAME}/simple-impl:${SERVICE_VERSION}"
    SENTINELLA_IMAGE = "gcr.io/${GOOGLE_PROJECT_NAME}/sentinella-impl:${SERVICE_VERSION}"
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
        sh 'gcloud docker -- push gcr.io/${GOOGLE_PROJECT_NAME}/simple-impl:${SERVICE_VERSION}'
        sh 'docker tag sentinella-impl:latest ${SENTINELLA_IMAGE}'
        sh 'gcloud docker -- push gcr.io/${GOOGLE_PROJECT_NAME}/sentinella-impl:${SERVICE_VERSION}'
      }
    }

    // CLUSTER
    stage('Cluster creation') {
      steps {
        sh 'gcloud container clusters create ${GOOGLE_TEST_CLUSTER_NAME} \
          --cluster-version=${GOOGLE_TEST_CLUSTER_VERSION} \
          --zone=${GOOGLE_ZONE_NAME} \
          --project ${GOOGLE_PROJECT_NAME} \
          --machine-type=${GOOGLE_TEST_MACHINE_TYPE} \
          --num-nodes=${GOOGLE_TEST_NUM_NODES}'
        sh 'gcloud container clusters get-credentials ${GOOGLE_TEST_CLUSTER_NAME} --project ${GOOGLE_PROJECT_NAME} --zone ${GOOGLE_ZONE_NAME}'
        sh 'kubectl apply --cluster gke_${GOOGLE_PROJECT_NAME}_${GOOGLE_ZONE_NAME}_${GOOGLE_TEST_CLUSTER_NAME} -f deploy/kubernetes/resources/storage-class-slow.yaml'
        // sh 'kubectl create --cluster gke_${GOOGLE_PROJECT_NAME}_${GOOGLE_ZONE_NAME}_${GOOGLE_TEST_CLUSTER_NAME} namespace ${APPS_NAMESPACE}'
      }
    }

    // INGRESS - KONG
    stage ('Deploy Ingress') {
      steps {
        sh 'kubectl create --cluster gke_${GOOGLE_PROJECT_NAME}_${GOOGLE_ZONE_NAME}_${GOOGLE_TEST_CLUSTER_NAME}  -f deploy/kubernetes/resources/kong/kong.yaml'
        sh 'kubectl apply --cluster gke_${GOOGLE_PROJECT_NAME}_${GOOGLE_ZONE_NAME}_${GOOGLE_TEST_CLUSTER_NAME} -f deploy/kubernetes/resources/kong/simple-ingress.yaml'
        // -n ${APPS_NAMESPACE}'
      }
    }

    // KAFKA e RBAC
    stage ('Deploy Kafka and Rbac') {
      steps {
        sh 'kubectl apply --cluster gke_${GOOGLE_PROJECT_NAME}_${GOOGLE_ZONE_NAME}_${GOOGLE_TEST_CLUSTER_NAME} -f deploy/kubernetes/resources/kafka/strimzi.yaml'
        sh 'kubectl apply --cluster gke_${GOOGLE_PROJECT_NAME}_${GOOGLE_ZONE_NAME}_${GOOGLE_TEST_CLUSTER_NAME} -f deploy/kubernetes/resources/kafka/kafka-persistent-single.yaml -n ${APPS_NAMESPACE}'
        sh 'kubectl apply --cluster gke_${GOOGLE_PROJECT_NAME}_${GOOGLE_ZONE_NAME}_${GOOGLE_TEST_CLUSTER_NAME} -f deploy/kubernetes/resources/rbac.yaml -n ${APPS_NAMESPACE}'
      }
    }

    // SERVICES
    stage ('Deploy Services') {
      parallel {
        stage('simple') {
          steps {
             sh 'kubectl apply --cluster gke_${GOOGLE_PROJECT_NAME}_${GOOGLE_ZONE_NAME}_${GOOGLE_TEST_CLUSTER_NAME} -f deploy/kubernetes/resources/simple/postgres-simple.yaml -n ${APPS_NAMESPACE}'
            sh 'sed -i "s#REPLACE_WITH_IMAGE_NAME#${SIMPLE_IMAGE}#g" deploy/kubernetes/resources/simple/simple.yaml'
            sh 'sleep 10s'
            sh 'kubectl apply --cluster gke_${GOOGLE_PROJECT_NAME}_${GOOGLE_ZONE_NAME}_${GOOGLE_TEST_CLUSTER_NAME} -f deploy/kubernetes/resources/simple/simple.yaml -n ${APPS_NAMESPACE}'
          }
        }
        stage('sentinella') {
          steps {
            sh 'sed -i "s#REPLACE_WITH_IMAGE_NAME#${SENTINELLA_IMAGE}#g" deploy/kubernetes/resources/sentinella/sentinella.yaml'
            sh 'sleep 10s'
            sh 'kubectl apply --cluster gke_${GOOGLE_PROJECT_NAME}_${GOOGLE_ZONE_NAME}_${GOOGLE_TEST_CLUSTER_NAME} -f deploy/kubernetes/resources/sentinella/sentinella.yaml -n ${APPS_NAMESPACE}'
          }
        }
      }
    }
  }
  post {
    always {
      sh 'docker system prune --filter until=168h --force --all'
    }
  }
}
