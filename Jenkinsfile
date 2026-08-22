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
        IMAGE_NAME = 'javaimage'
        CONTAINER_NAME = 'javacontainer'
        APP_PORT = '8081'
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
                    docker build -t ${IMAGE_NAME}:${BUILD_NUMBER} .
                    
                    echo "===== Docker Image Created ====="
                    docker images ${IMAGE_NAME}
                '''

                echo '===== Docker Build Completed ====='
            }
        }

        stage('Manual Approval') {
            steps {
                input(
                    id: 'deployApproval',
                    message: "Deploy Docker build ${env.BUILD_NUMBER} to EC2? Approver: ${params.DEPLOY_APPROVER}",
                    ok: 'Approve Deployment',
                    submitter: params.DEPLOY_APPROVER
                )
            }
        }

        stage('Docker Deploy') {
            steps {
                echo '===== Docker Deployment Started ====='

                sh '''
                    echo "===== Stopping Existing Container ====="

                    docker stop ${CONTAINER_NAME} || true

                    echo "===== Removing Existing Container ====="

                    docker rm ${CONTAINER_NAME} || true

                    echo "===== Starting New Container ====="

                    docker run -d \
                        --name ${CONTAINER_NAME} \
                        -p ${APP_PORT}:${APP_PORT} \
                        ${IMAGE_NAME}:${BUILD_NUMBER}

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

                    curl --fail http://localhost:${APP_PORT}

                    echo ""
                    echo "===== Application Health Check Passed ====="
                '''
            }
        }
    }

    post {
        success {
            echo '===== Pipeline Completed Successfully ====='
        }

        failure {
            echo '===== Pipeline Failed ====='
        }

        aborted {
            echo '===== Pipeline Aborted ====='
        }
    }
}
