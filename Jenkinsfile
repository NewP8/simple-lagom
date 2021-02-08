CLUSTERIP = 'initial_value'
TEST_DATAOBJECT_ELASTIC_DB_PASSWORD = 'initial_value'

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

//    PROJECT_SHORTNAME = 'simple'
//     DEPLOY_ENV = 'test'
//     SUBDOMAIN_NAME = 'sandbox'
//     DOMAIN_NAME = 'fintechcgn.it'
    FULL_HOSTNAME = "simple-hostname"
    // BASE_URL = "https://${FULL_HOSTNAME}"

    GOOGLE_PROJECT_NAME = 'fintech-266508'
    GOOGLE_ZONE_NAME = 'europe-west1-b'
    GOOGLE_TEST_CLUSTER_NAME = "simple-${SERVICE_VERSION}"
    GOOGLE_TEST_CLUSTER_VERSION = 'latest'
    GOOGLE_TEST_MACHINE_TYPE = 'n1-standard-4'
    GOOGLE_TEST_NUM_NODES = '1'

    APPS_NAMESPACE = 'kafka'

    SIMPLE_IMAGE = "gcr.io/${GOOGLE_PROJECT_NAME}/simple-impl:${SERVICE_VERSION}"
//     SIMPLE_REQUIRED_CONTACT_POINT_NR = '1'
//     SIMPLE_DB_URL = 'jdbc:postgresql://localhost:5432/simpledb'
//     SIMPLE_DB_USER = 'simpledb'
//     SIMPLE_DB_PASSWORD = 'simpledb' // poi da togliere e generare al volo $(openssl rand -base64 48)

    SENTINELLA_IMAGE = "gcr.io/${GOOGLE_PROJECT_NAME}/sentinella-impl:${SERVICE_VERSION}"
//     SENTINELLA_REQUIRED_CONTACT_POINT_NR = '1'

  }
  stages {

    stage ('Build') {
      steps {
        // slackSend (message: "simple- pipeline started!") // ${env.BASE_URL}
        sh 'sbt compile'
      }
    }

    stage('Containerization') {
      steps {
        sh 'sbt -DbuildTarget=kubernetes clean docker:publishLocal'

        // kong ?

        // simple
        sh 'docker tag simple-impl:latest ${SIMPLE_IMAGE}'
        sh 'gcloud docker -- push gcr.io/${GOOGLE_PROJECT_NAME}/simple-impl:${SERVICE_VERSION}'

        // sentinella
        sh 'docker tag sentinella-impl:latest ${SENTINELLA_IMAGE}'
        sh 'gcloud docker -- push gcr.io/${GOOGLE_PROJECT_NAME}/sentinella-impl:${SERVICE_VERSION}'
      }
    }

    // TESTING ENVIRONMENT
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
        // non serve ssd per test
        sh 'kubectl create --cluster gke_${GOOGLE_PROJECT_NAME}_${GOOGLE_ZONE_NAME}_${GOOGLE_TEST_CLUSTER_NAME} namespace ${APPS_NAMESPACE}'
      }
    }
    // END CREATE TESTING ENVIRONMENT

    // TESTING ENVIRONMENT
    // stage ('Deploy Services') {
    //   parallel {
        stage ('Deploy Ingress') {
          steps {
            // Deploy Kong ingress controller
            sh 'sed -i "s#REPLACE_WITH_GOOGLE_PROJECT_NAME#${GOOGLE_PROJECT_NAME}#g" deploy/kubernetes/resources/kong/values.yaml'
            // sh 'sed -i "s#REPLACE_WITH_SERVICE_VERSION#${SERVICE_VERSION}#g" deploy/kubernetes/resources/kong/values.yaml'
            sh 'helm repo add kong https://charts.konghq.com'
            sh 'helm repo update'
            sh 'helm install --kube-context gke_${GOOGLE_PROJECT_NAME}_${GOOGLE_ZONE_NAME}_${GOOGLE_TEST_CLUSTER_NAME} --namespace ${APPS_NAMESPACE} kong kong/kong --set ingressController.installCRDs=false -f deploy/kubernetes/resources/kong/values.yaml --wait --timeout 600s'
            // sh 'helm upgrade --kube-context gke_${GOOGLE_PROJECT_NAME}_${GOOGLE_ZONE_NAME}_${GOOGLE_TEST_CLUSTER_NAME} --namespace ${APPS_NAMESPACE} kong kong/kong --set ingressController.installCRDs=false -f deploy/kubernetes/resources/kong/values.yaml'
            // sh 'sed -i "s#REPLACE_WITH_FQDN#${FULL_HOSTNAME}#g" deploy/kubernetes/resources/kong/simple-ingress.yaml'
            // sh 'sed -i "s#REPLACE_WITH_CERT_MANAGER_CERT_NAME#sdc-tls-secret#g" deploy/kubernetes/resources/kong/simple-ingress.yaml'
            // replace base64_tls_certificate with the ssl certificare of actual host
            // sh 'sed -i "s#REPLACE_WITH_TLS_CRT#LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUYxVENDQkwyZ0F3SUJBZ0lRTDBVa0dSdjU2M3dvcnlUUmE3ZkllekFOQmdrcWhraUc5dzBCQVFzRkFEQ0IKanpFTE1Ba0dBMVVFQmhNQ1IwSXhHekFaQmdOVkJBZ1RFa2R5WldGMFpYSWdUV0Z1WTJobGMzUmxjakVRTUE0RwpBMVVFQnhNSFUyRnNabTl5WkRFWU1CWUdBMVVFQ2hNUFUyVmpkR2xuYnlCTWFXMXBkR1ZrTVRjd05RWURWUVFECkV5NVRaV04wYVdkdklGSlRRU0JFYjIxaGFXNGdWbUZzYVdSaGRHbHZiaUJUWldOMWNtVWdVMlZ5ZG1WeUlFTkIKTUI0WERUSXdNREV5T0RBd01EQXdNRm9YRFRJeE1ERXlOekl6TlRrMU9Wb3dJakVnTUI0R0ExVUVBd3dYS2k1egpZVzVrWW05NExtWnBiblJsWTJoaloyNHVhWFF3Z2dFaU1BMEdDU3FHU0liM0RRRUJBUVVBQTRJQkR3QXdnZ0VLCkFvSUJBUUNrN3JXMEtPU2VBYUpWdlk0bWVCeW84eGRBRTZkbEhnUlRmWi81QUFpZ29GZFpBVkVLMWdnSEZpN1IKRGE5ekNFOFNwc3N0VUE2MHR5T1FCbEwySWVCL2JmdXJwbE1XdkZiMlFrVDRrdXdOaFBackRZWVcxVVdmSlpWcgo5eG9nVGJGaVlQUEE1anE3N0Y5c2ZCUzRTbkZtbE1Ea3pnVGpvQng5YWFKRThVeS9DUFZodEJ6WGlhSHZ2Z2JtCkloT09ncFhrODRlSmpBa29XWHdiWGkweUhKZXVkKzFyaHpQN0tjaFQ2NnBoc2lFNzdJOXdBS1lCeC9yb0VVRGEKMGI4QS9KK2dncmxFT3ZKMnZ0dWt5KzlwWlkySGhoTWZNQ3BBTjQ3NlM2SjFocG5PbkN2bWNSUWxmK1A1RU84MApqdFkvd2dBNkZqdlgrdERORHdVWmQ1aDZiS2dSQWdNQkFBR2pnZ0tYTUlJQ2t6QWZCZ05WSFNNRUdEQVdnQlNOCmpGN0VWSzJLNFhmcG0vbWJCZUc0QVkxaDRUQWRCZ05WSFE0RUZnUVVFWTJoNGZrczhZVGUvYStDVUt1d3NRaGYKc1ZFd0RnWURWUjBQQVFIL0JBUURBZ1dnTUF3R0ExVWRFd0VCL3dRQ01BQXdIUVlEVlIwbEJCWXdGQVlJS3dZQgpCUVVIQXdFR0NDc0dBUVVGQndNQ01Fa0dBMVVkSUFSQ01FQXdOQVlMS3dZQkJBR3lNUUVDQWdjd0pUQWpCZ2dyCkJnRUZCUWNDQVJZWGFIUjBjSE02THk5elpXTjBhV2R2TG1OdmJTOURVRk13Q0FZR1o0RU1BUUlCTUlHRUJnZ3IKQmdFRkJRY0JBUVI0TUhZd1R3WUlLd1lCQlFVSE1BS0dRMmgwZEhBNkx5OWpjblF1YzJWamRHbG5ieTVqYjIwdgpVMlZqZEdsbmIxSlRRVVJ2YldGcGJsWmhiR2xrWVhScGIyNVRaV04xY21WVFpYSjJaWEpEUVM1amNuUXdJd1lJCkt3WUJCUVVITUFHR0YyaDBkSEE2THk5dlkzTndMbk5sWTNScFoyOHVZMjl0TURrR0ExVWRFUVF5TURDQ0Z5b3UKYzJGdVpHSnZlQzVtYVc1MFpXTm9ZMmR1TG1sMGdoVnpZVzVrWW05NExtWnBiblJsWTJoaloyNHVhWFF3Z2dFRgpCZ29yQmdFRUFkWjVBZ1FDQklIMkJJSHpBUEVBZHdCOVB2TDRqLytJVldna3dzREtubEtKZVN2RkRuZ0pmeTVxCmwyaVpmaUx3MXdBQUFXL3NqelFwQUFBRUF3QklNRVlDSVFEbnE5UWZ5ZlVCeUJMOWpSSjM3ZTlDQ1B2akdTd1QKcmtsYnFDSmp2N0gwQkFJaEFLSzl3UkUxREhwRlJlUE5hTTJjT3FHWmF4MU5nT1NBMXhVVm5DaXJwUFRFQUhZQQpSSlJsTHJEdXpxL0VRQWZZcVA0b3dOcm1ncjdZeXpHMVA5TXpsclcyZ2FnQUFBRnY3STgwSEFBQUJBTUFSekJGCkFpRUE5NXBwLzlzQUFwQ1dWU29XNWpkeGFaS3NHVGZWaWl1UlY3MlhYLzRUeXBrQ0lBNjk4SGZOczFLQVNrQXcKOHFKZEE3Tm0vYWRZU09PbHEzVjhXMi9XT1kwek1BMEdDU3FHU0liM0RRRUJDd1VBQTRJQkFRQ1JxRjhOZm9wUgprTmtyV1QvWHlZNkJPVU9ySms5a1NUM013T21KbkQwODBzMmN3d21JdURpSFFDclVtZ3Aza0xqVG16TkJCeGRSCm44aWQzckdsWFNubHJjL3lVZXdPaytuRU5TRm9ycEF2eTA4NDQwVTA5MHAvem9tczkvcFQ4d3pyWmpaVk9HblIKaWorbHNhbVg2UDVQSm43THpsNnltQ0RwRE4zUVhjbDBMTUlYNzJobHZhVlRpWlB0Z2hGaDlvMGM3eUxMSWxsaAoxZzB3UEhMS2I4NlZxMk51REYwY1laYVhWbjlMOGtuNkdNT2VzK243Q1lmb0RZaC9NVkxFcHZoT25Gam81bjNJClRIaHZJdXNBSXVPREpON3lUYmtaL0J6dEc2ZDE1Ukp6MFl2bnJtMjY3Vm1KcG0wUXRFSUNnL1k1R3I5U1J5Z0cKZ2kwVnJ5OWlvL01CCi0tLS0tRU5EIENFUlRJRklDQVRFLS0tLS0K#g" deploy/kubernetes/resources/sdc-ingress/sdc-tls-secret.yaml'
            // replace base64_tls_key with the ssl key of actual host
            // sh 'sed -i "s#REPLACE_WITH_TLS_KEY#LS0tLS1CRUdJTiBSU0EgUFJJVkFURSBLRVktLS0tLQpNSUlFb3dJQkFBS0NBUUVBcE82MXRDamtuZ0dpVmIyT0puZ2NxUE1YUUJPblpSNEVVMzJmK1FBSW9LQlhXUUZSCkN0WUlCeFl1MFEydmN3aFBFcWJMTFZBT3RMY2prQVpTOWlIZ2YyMzdxNlpURnJ4VzlrSkUrSkxzRFlUMmF3MkcKRnRWRm55V1ZhL2NhSUUyeFltRHp3T1k2dSt4ZmJId1V1RXB4WnBUQTVNNEU0NkFjZldtaVJQRk12d2oxWWJRYwoxNG1oNzc0RzVpSVRqb0tWNVBPSGlZd0pLRmw4RzE0dE1oeVhybmZ0YTRjeit5bklVK3VxWWJJaE8reVBjQUNtCkFjZjY2QkZBMnRHL0FQeWZvSUs1UkRyeWRyN2JwTXZ2YVdXTmg0WVRIekFxUURlTytrdWlkWWFaenB3cjVuRVUKSlgvaitSRHZOSTdXUDhJQU9oWTcxL3JRelE4RkdYZVllbXlvRVFJREFRQUJBb0lCQUJlbGNiK21yVUJLRTdBZgpRU1lheW1FZW1STEN2cGtzdUlvUDFNT3FVWkpWNnJBRUZFNXRhVEU2NlBObjl1T1RLV01QTHNvTVZFOXNnbGkvCnoxMGlka0ZPejJwSXFsajBIN09teEtTdXk5RVVZdy91SEc5aG5GMjQwYmRzOGYzM3BacnBNNGxRZU5OQWpGM04KWEZzZnNTcDM0VC9zbVo3WThieTVURjUrQ2ZiSll3bDB4dWRFVlZadkppbGFWcDZLWU5DM1RGcEtBRzMzTDIrSApzUGxDWEFQOVRGM2xDTHhlZXhlUmlSQ2lQWXROYWtxSFZ4U3hwWXFlVjNQYVBhbGxObFJKREtoTUhhVUFxbG9aCm1XR3ZETXpZRzA0bndFVmZOdVM1ZVQ0UHdnbHpTZG9qYkQ4UHhybHRoZVFoU3NadjQvcVJnS3I0bjRUM25uaDYKdklXZVZGRUNnWUVBeEVxVlo4QTZuNStxUlh0YWtoM1o1ZHBNR29LaW9HZGhZRnZaMmpEVy9qM2ZGSXBaTGZ4VQowbm5mYWg3eE5LcDduUDBIdnJ6a3EyK0xUSUxBaWExNitBaWRjK2VqdUFpOURaMVNVbk9OZmpBR01YUVJxUkN4CkJ1dFdDUkhqN2pUcDZjdjFOQTNGb3dTYXpSL3N5NE9QWDBsWW1NTVBqcVE5M0svVXpIVU5DMWNDZ1lFQTF4b3QKTVRpUnNKd0dWWjZrTWkrUnRidS9YM2hNWm5kT3RobzIwdXl4UFVrY0RXNWlDNS9TQ0ZLc3AvUDZhRlNNWGdRegptZVA3alhtQjNaMnBNcHlPTWZYYkFEcms0VUJqc0UrRGkyOEZWRGk5NDNXaWFwaDlYcVdRTVJzR21zaTluSVVrCm45V1RGSkQwSXJDQUViZW9KNjlQbGVIRlg2Wi9VOGoyZCtCMXJ0Y0NnWUFNZG5QUDk0dUJVUURka254b3BJNk8KSW5NTWg5ak5lR0xkWEZlVG0rQTZtakVNdTgvM3RIcXFObVN4OXk3M1dnK3BJd09YMzZSaEloN2xCN1F6eVpqeQpJSkhtcHdGbEVPRDMrVklkdkFweXhaZlBFZ0NCREhkQTZqWTNNelNXampOL3paeE81c1R5bWJzK1pOV0RBbUphCitXd3Zuc3JoRXBwKzVZY01TQzVBMXdLQmdBRmphdDFCZHpkRG1vcmM3a1JncURrYkVCNG1vWiticjRkZlhmRTQKaytCSVk0VDJyRzFVeDc5RUFWZElMTnFWaVI1bU9vc0l2S3ZxRjl0OVZBVSttM0JaSE9QQWZZK3lvYklXb1V1NgpHTGUxY2d4UXl3NlRFMlFULzMxQnBtWkRXMjN5TWcxU0RKUDZaNGo5eUtYVW9LSTQ4SE9RTkh6c2p5L1VRVHIrCjJPT2JBb0dCQUlIS0o4bDFEZXBJWnRvZDQvR05UeENmWGV6SmdNcncrc00zQk9wMlJad3lrU0haQzJMeGkvaHUKVlVYcUVxYU1yWEtpYWpDa2lERVNzdTNKTThTck52NE91ak9Ed1FuL3pPbUFDTFZaTXdka3lwYkUxd0U5SFRjTgpCRktpdmdHcER0UVBVYy9hVWhyU1hsYjNIOWZiM2JyZ29hbWhqZnBLOVJnZit2ZEZKSjErCi0tLS0tRU5EIFJTQSBQUklWQVRFIEtFWS0tLS0tCg==#g" deploy/kubernetes/resources/sdc-ingress/sdc-tls-secret.yaml'
            sh 'kubectl apply --cluster gke_${GOOGLE_PROJECT_NAME}_${GOOGLE_ZONE_NAME}_${GOOGLE_TEST_CLUSTER_NAME} -f deploy/kubernetes/resources/kong/simple-ingress.yaml -n ${APPS_NAMESPACE}'
            // get ingress public ip
            // sh 'timeout 300 deploy/kubernetes/scripts/kubectl-wait-for-ips'
            // sh 'kubectl wait --cluster gke_${GOOGLE_PROJECT_NAME}_${GOOGLE_ZONE_NAME}_${GOOGLE_TEST_CLUSTER_NAME} --for=condition=Ready pod --all -n ${APPS_NAMESPACE} --timeout=300s'
            // sh 'kubectl get --cluster gke_${GOOGLE_PROJECT_NAME}_${GOOGLE_ZONE_NAME}_${GOOGLE_TEST_CLUSTER_NAME} -o jsonpath="{.status.loadBalancer.ingress[0].ip}" service -n ${APPS_NAMESPACE} kong-kong-proxy > clusterip.txt'
            // script {
              // CLUSTERIP = readFile('clusterip.txt').trim()
            // }
          }
        }

    stage ('Deploy Kafka and Rbac') {
      steps {

        // deploy kafka
        sh 'kubectl apply --cluster gke_${GOOGLE_PROJECT_NAME}_${GOOGLE_ZONE_NAME}_${GOOGLE_TEST_CLUSTER_NAME} -f deploy/kubernetes/resources/kafka/strimzi.yaml'
        sh 'kubectl apply --cluster gke_${GOOGLE_PROJECT_NAME}_${GOOGLE_ZONE_NAME}_${GOOGLE_TEST_CLUSTER_NAME} -f deploy/kubernetes/resources/kafka/kafka-persistent-single.yaml -n ${APPS_NAMESPACE}'

//         sh "kubectl apply --cluster gke_${GOOGLE_PROJECT_NAME}_${GOOGLE_ZONE_NAME}_${GOOGLE_TEST_CLUSTER_NAME} -f 'https://strimzi.io/install/latest?namespace=${APPS_NAMESPACE}' -n ${APPS_NAMESPACE}"
//         sh 'kubectl apply --cluster gke_${GOOGLE_PROJECT_NAME}_${GOOGLE_ZONE_NAME}_${GOOGLE_TEST_CLUSTER_NAME} -f deploy/kubernetes/resources/kafka/ -n ${APPS_NAMESPACE}'

        // deploy rbac
        sh 'kubectl apply --cluster gke_${GOOGLE_PROJECT_NAME}_${GOOGLE_ZONE_NAME}_${GOOGLE_TEST_CLUSTER_NAME} -f deploy/kubernetes/resources/rbac.yaml -n ${APPS_NAMESPACE}'
      }
    }

//       }
//     }

//     stage ('Create Secrets') {
//       environment {
//           DATAOBJECT_ELASTIC_DB_PASSWORD = "${TEST_DATAOBJECT_ELASTIC_DB_PASSWORD}"
//       }
//       steps {
//         sh "kubectl create secret generic simple-secret --from-literal=secret=$(openssl rand -base64 48)"
//         sh "kubectl create secret generic simple-secret --from-literal=SIMPLE_APPLICATION_SECRET=$(openssl rand -base64 32) -n ${APPS_NAMESPACE}"
//       }
//     }



    stage ('Deploy Services') {
      parallel {
        stage('simple') {
          steps {

            // deploy postgresql
             sh 'kubectl apply --cluster gke_${GOOGLE_PROJECT_NAME}_${GOOGLE_ZONE_NAME}_${GOOGLE_TEST_CLUSTER_NAME} -f deploy/kubernetes/resources/simple/postgres-simple.yaml -n ${APPS_NAMESPACE}'

            // deploy service
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
//   post {
//     always {
//       sh 'docker system prune --filter until=168h --force --all'
//       sh 'sudo sed -i "/${FULL_HOSTNAME}/d" /etc/hosts'
//     }
//     success {
//       emailext body: "${CLUSTERIP} - ${BASE_URL}",
//                       recipientProviders: [[$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider']],
//                       subject: "${PROJECT_SHORTNAME} - pipeline ok!",
//                       to: 'paolo.fabbro@cgn.it'
//       slackSend (message: "${PROJECT_SHORTNAME} - pipeline OK! - ${env.BASE_URL} - ${CLUSTERIP}")
//     }
//     failure {
//       emailext body: "${CLUSTERIP} - ${BASE_URL}",
//                       recipientProviders: [[$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider']],
//                       subject: "${PROJECT_SHORTNAME} - pipeline fallita",
//                       to: 'enrico.mitri@cgn.it'
//       slackSend (message: "${PROJECT_SHORTNAME} - pipeline FAILED! - ${env.BASE_URL} - ${CLUSTERIP}")
//     }
//   }
}
