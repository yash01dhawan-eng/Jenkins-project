pipeline {

    agent any

    parameters {
        string(
            name: 'DEPLOY_APPROVER',
            defaultValue: 'yash',
            description: 'Jenkins user allowed to approve deployment'
        )
    }

    environment {
        AWS_REGION = 'us-east-1'
        AWS_ACCOUNT_ID = '394281571893'

        ECR_REPOSITORY = 'jenkins-project/cicd'
        ECR_REGISTRY = '394281571893.dkr.ecr.us-east-1.amazonaws.com'

        IMAGE_NAME = 'javaimage'
        CONTAINER_NAME = 'javacontainer'
        APP_PORT = '8081'

        ECR_IMAGE = "${ECR_REGISTRY}/${ECR_REPOSITORY}:${BUILD_NUMBER}"
    }

    stages {

        stage('Build') {
            steps {
                echo '===== Maven Build Started ====='

                sh '''
                    mvn clean package

                    echo "===== Build Artifacts ====="
                    ls -lah target/
                '''

                echo '===== Maven Build Completed ====='
            }
        }

        stage('Docker Build') {
            steps {
                echo '===== Docker Build Started ====='

                sh '''
                    docker build \
                        -t ${IMAGE_NAME}:${BUILD_NUMBER} \
                        .

                    echo "===== Docker Image Created ====="

                    docker images ${IMAGE_NAME}
                '''

                echo '===== Docker Build Completed ====='
            }
        }

        stage('ECR Login') {
            steps {
                echo '===== ECR Login Started ====='

                sh '''
                    aws ecr get-login-password \
                        --region ${AWS_REGION} | \
                    docker login \
                        --username AWS \
                        --password-stdin ${ECR_REGISTRY}
                '''

                echo '===== ECR Login Completed ====='
            }
        }

        stage('Tag Docker Image') {
            steps {
                echo '===== Tagging Docker Image ====='

                sh '''
                    docker tag \
                        ${IMAGE_NAME}:${BUILD_NUMBER} \
                        ${ECR_IMAGE}

                    echo "===== ECR Image ====="
                    echo "${ECR_IMAGE}"

                    docker images
                '''

                echo '===== Docker Image Tagged ====='
            }
        }

        stage('Push Image to ECR') {
            steps {
                echo '===== Pushing Docker Image to ECR ====='

                sh '''
                    docker push ${ECR_IMAGE}
                '''

                echo '===== Docker Image Successfully Pushed to ECR ====='
            }
        }

        stage('Manual Approval') {
            steps {
                input(
                    id: 'deployApproval',
                    message: "Deploy ECR image ${ECR_IMAGE} to EC2? Approver: ${params.DEPLOY_APPROVER}",
                    ok: 'Approve Deployment',
                    submitter: params.DEPLOY_APPROVER
                )
            }
        }

        stage('Docker Deploy') {
            steps {
                echo '===== Docker Deployment Started ====='

                sh '''
                    echo "===== Pulling Exact Image from ECR ====="

                    docker pull ${ECR_IMAGE}

                    echo "===== Stopping Existing Container ====="

                    docker stop ${CONTAINER_NAME} || true

                    echo "===== Removing Existing Container ====="

                    docker rm ${CONTAINER_NAME} || true

                    echo "===== Starting New Container ====="

                    docker run -d \
                        --name ${CONTAINER_NAME} \
                        -p ${APP_PORT}:${APP_PORT} \
                        ${ECR_IMAGE}

                    echo "===== Container Status ====="

                    docker ps

                    echo "===== Application Logs ====="

                    sleep 5
                    docker logs ${CONTAINER_NAME}
                '''

                echo '===== Docker Deployment Completed ====='
            }
        }

        stage('Health Check') {
            steps {
                echo '===== Application Health Check ====='

                sh '''
                    sleep 2

                    curl --fail \
                        http://localhost:${APP_PORT}

                    echo ""
                    echo "===== Application Health Check Passed ====="
                '''
            }
        }
    }

    post {

        success {
            echo '===== Pipeline Completed Successfully ====='
            echo "===== Deployed Image: ${ECR_IMAGE} ====="
        }

        failure {
            echo '===== Pipeline Failed ====='
        }

        aborted {
            echo '===== Pipeline Aborted ====='
        }
    }
}
