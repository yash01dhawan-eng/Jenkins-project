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

        // =========================================================
        // APPLICATION CONFIGURATION
        // =========================================================
        IMAGE_NAME = 'javaimage'
        APP_PORT = '8081'

        // =========================================================
        // DOCKER CONFIGURATION
        // =========================================================
        DOCKER_NETWORK = 'app-network'

        // Stable container names
        CONTAINER_1 = 'javacontainer'
        CONTAINER_2 = 'javacontainer-2'
        CONTAINER_3 = 'javacontainer-3'

        // =========================================================
        // AWS / ECR CONFIGURATION
        // =========================================================
        AWS_REGION = 'us-east-1'
        AWS_ACCOUNT_ID = '394281571893'
        ECR_REPOSITORY = 'jenkins-project/cicd'

        ECR_REGISTRY = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"

        // Build-specific immutable image
        ECR_IMAGE = "${ECR_REGISTRY}/${ECR_REPOSITORY}:${BUILD_NUMBER}"
    }

    stages {

        // =========================================================
        // 1. MAVEN BUILD
        // =========================================================
        stage('Build') {
            steps {
                echo '===== Maven Build Started ====='

                sh '''
                    set -e

                    mvn clean package

                    echo "===== Build Artifacts ====="
                    ls -lah target/

                    echo "===== Maven Build Completed ====="
                '''
            }
        }

        // =========================================================
        // 2. DOCKER BUILD
        // =========================================================
        stage('Docker Build') {
            steps {
                echo '===== Docker Build Started ====='

                sh '''
                    set -e

                    docker build \
                        -t ${IMAGE_NAME}:${BUILD_NUMBER} \
                        .

                    echo "===== Docker Image Created ====="
                    docker images ${IMAGE_NAME}
                '''

                echo '===== Docker Build Completed ====='
            }
        }

        // =========================================================
        // 3. ECR LOGIN
        // =========================================================
        stage('ECR Login') {
            steps {
                echo '===== ECR Login Started ====='

                sh '''
                    set -e

                    aws ecr get-login-password \
                        --region ${AWS_REGION} \
                    | docker login \
                        --username AWS \
                        --password-stdin ${ECR_REGISTRY}

                    echo "===== ECR Login Successful ====="
                '''
            }
        }

        // =========================================================
        // 4. TAG IMAGE
        // =========================================================
        stage('Docker Tag') {
            steps {
                echo '===== Docker Tag Started ====='

                sh '''
                    set -e

                    docker tag \
                        ${IMAGE_NAME}:${BUILD_NUMBER} \
                        ${ECR_IMAGE}

                    echo "===== Image Tagged ====="
                    echo "${ECR_IMAGE}"

                    docker images ${IMAGE_NAME}
                '''

                echo '===== Docker Tag Completed ====='
            }
        }

        // =========================================================
        // 5. PUSH IMAGE TO ECR
        // =========================================================
        stage('Push Image to ECR') {
            steps {
                echo '===== ECR Push Started ====='

                sh '''
                    set -e

                    docker push ${ECR_IMAGE}

                    echo "===== Image Successfully Pushed to ECR ====="
                    echo "${ECR_IMAGE}"
                '''

                echo '===== ECR Push Completed ====='
            }
        }

        // =========================================================
        // 6. VERIFY IMAGE IN ECR
        // =========================================================
        stage('Verify ECR Image') {
            steps {
                echo '===== Verifying Image in ECR ====='

                sh '''
                    set -e

                    aws ecr describe-images \
                        --repository-name ${ECR_REPOSITORY} \
                        --image-ids imageTag=${BUILD_NUMBER} \
                        --region ${AWS_REGION}

                    echo "===== ECR Image Verification Successful ====="
                '''
            }
        }

        // =========================================================
        // 7. MANUAL APPROVAL
        // =========================================================
        stage('Manual Approval') {
            steps {
                input(
                    id: 'deployApproval',
                    message: "Deploy ${ECR_IMAGE} to EC2?",
                    ok: 'Approve Deployment',
                    submitter: params.DEPLOY_APPROVER
                )
            }
        }

        // =========================================================
        // 8. PULL EXACT IMAGE
        // =========================================================
        stage('Pull Image from ECR') {
            steps {
                echo '===== Pulling Exact Image from ECR ====='

                sh '''
                    set -e

                    docker pull ${ECR_IMAGE}

                    echo "===== Exact ECR Image Pulled ====="

                    docker inspect ${ECR_IMAGE} \
                        --format='Image ID: {{.Id}}'
                '''
            }
        }

        // =========================================================
        // 9. DEPLOY CONTAINER 1
        // =========================================================
        stage('Deploy Container 1') {
            steps {
                echo "===== Deploying ${CONTAINER_1} ====="

                sh '''
                    set -e

                    echo "===== Stopping ${CONTAINER_1} ====="
                    docker stop ${CONTAINER_1} 2>/dev/null || true

                    echo "===== Removing ${CONTAINER_1} ====="
                    docker rm ${CONTAINER_1} 2>/dev/null || true

                    echo "===== Starting ${CONTAINER_1} ====="

                    docker run -d \
                        --name ${CONTAINER_1} \
                        --network ${DOCKER_NETWORK} \
                        ${ECR_IMAGE}

                    echo "===== Waiting for Application ====="
                    sleep 5

                    echo "===== Health Check: ${CONTAINER_1} ====="

                    docker exec ${CONTAINER_1} \
                        curl --fail \
                        http://localhost:${APP_PORT}

                    echo ""
                    echo "===== ${CONTAINER_1} Healthy ====="
                '''
            }
        }

        // =========================================================
        // 10. DEPLOY CONTAINER 2
        // =========================================================
        stage('Deploy Container 2') {
            steps {
                echo "===== Deploying ${CONTAINER_2} ====="

                sh '''
                    set -e

                    echo "===== Stopping ${CONTAINER_2} ====="
                    docker stop ${CONTAINER_2} 2>/dev/null || true

                    echo "===== Removing ${CONTAINER_2} ====="
                    docker rm ${CONTAINER_2} 2>/dev/null || true

                    echo "===== Starting ${CONTAINER_2} ====="

                    docker run -d \
                        --name ${CONTAINER_2} \
                        --network ${DOCKER_NETWORK} \
                        ${ECR_IMAGE}

                    echo "===== Waiting for Application ====="
                    sleep 5

                    echo "===== Health Check: ${CONTAINER_2} ====="

                    docker exec ${CONTAINER_2} \
                        curl --fail \
                        http://localhost:${APP_PORT}

                    echo ""
                    echo "===== ${CONTAINER_2} Healthy ====="
                '''
            }
        }

        // =========================================================
        // 11. DEPLOY CONTAINER 3
        // =========================================================
        stage('Deploy Container 3') {
            steps {
                echo "===== Deploying ${CONTAINER_3} ====="

                sh '''
                    set -e

                    echo "===== Stopping ${CONTAINER_3} ====="
                    docker stop ${CONTAINER_3} 2>/dev/null || true

                    echo "===== Removing ${CONTAINER_3} ====="
                    docker rm ${CONTAINER_3} 2>/dev/null || true

                    echo "===== Starting ${CONTAINER_3} ====="

                    docker run -d \
                        --name ${CONTAINER_3} \
                        --network ${DOCKER_NETWORK} \
                        ${ECR_IMAGE}

                    echo "===== Waiting for Application ====="
                    sleep 5

                    echo "===== Health Check: ${CONTAINER_3} ====="

                    docker exec ${CONTAINER_3} \
                        curl --fail \
                        http://localhost:${APP_PORT}

                    echo ""
                    echo "===== ${CONTAINER_3} Healthy ====="
                '''
            }
        }

        // =========================================================
        // 12. FINAL DEPLOYMENT VERIFICATION
        // =========================================================
        stage('Deployment Verification') {
            steps {
                echo '===== Deployment Verification Started ====='

                sh '''
                    set -e

                    echo "========================================"
                    echo "RUNNING APPLICATION CONTAINERS"
                    echo "========================================"

                    docker ps \
                        --filter "name=${CONTAINER_1}" \
                        --filter "name=${CONTAINER_2}" \
                        --filter "name=${CONTAINER_3}"

                    echo ""
                    echo "========================================"
                    echo "VERIFYING DEPLOYED IMAGES"
                    echo "========================================"

                    for CONTAINER in \
                        ${CONTAINER_1} \
                        ${CONTAINER_2} \
                        ${CONTAINER_3}
                    do
                        echo ""
                        echo "Container: ${CONTAINER}"

                        DEPLOYED_IMAGE=$(docker inspect ${CONTAINER} \
                            --format '{{.Config.Image}}')

                        echo "Expected Image: ${ECR_IMAGE}"
                        echo "Actual Image:   ${DEPLOYED_IMAGE}"

                        if [ "${DEPLOYED_IMAGE}" != "${ECR_IMAGE}" ]; then
                            echo "ERROR: ${CONTAINER} is running the wrong image."
                            exit 1
                        fi

                        echo "${CONTAINER}: IMAGE VERIFICATION PASSED"
                    done

                    echo ""
                    echo "========================================"
                    echo "DOCKER NETWORK"
                    echo "========================================"

                    docker network inspect ${DOCKER_NETWORK} \
                        --format '{{range .Containers}}{{.Name}} -> {{.IPv4Address}}{{"\\n"}}{{end}}'

                    echo ""
                    echo "========================================"
                    echo "NGINX VERIFICATION"
                    echo "========================================"

                    docker ps \
                        --filter "name=nginxcontainer" \
                        --format "Name: {{.Names}} | Status: {{.Status}} | Ports: {{.Ports}}"

                    echo ""
                    echo "Testing application through Nginx..."

                    curl --fail http://localhost

                    echo ""
                    echo "===== Nginx Health Check Passed ====="
                    echo "===== DEPLOYMENT VERIFICATION PASSED ====="
                '''
            }
        }
    }

    // =============================================================
    // POST ACTIONS
    // =============================================================
    post {

        success {
            echo '''
==================================================
PIPELINE COMPLETED SUCCESSFULLY
==================================================
Docker image built successfully.
Image pushed to ECR.
ECR image verified.
All application containers deployed.
All application containers passed health checks.
Deployed image versions verified.
Nginx traffic routing verified.
==================================================
'''
        }

        failure {
            echo '''
==================================================
PIPELINE FAILED
==================================================
Check the failed stage and Jenkins console output.
==================================================
'''
        }

        aborted {
            echo '===== Jenkins Pipeline Aborted ====='
        }

        always {
            echo '===== Jenkins Pipeline Execution Finished ====='
        }
    }
}
