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
        // APPLICATION
        // =========================================================
        IMAGE_NAME = 'javaimage'
        APP_PORT = '8081'

        // =========================================================
        // DOCKER
        // =========================================================
        DOCKER_NETWORK = 'app-network'

        CONTAINER_1 = 'javacontainer'
        CONTAINER_2 = 'javacontainer-2'
        CONTAINER_3 = 'javacontainer-3'

        // =========================================================
        // AWS / ECR
        // =========================================================
        AWS_REGION = 'us-east-1'
        AWS_ACCOUNT_ID = '394281571893'
        ECR_REPOSITORY = 'jenkins-project/cicd'

        ECR_REGISTRY =
            "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"

        ECR_IMAGE =
            "${ECR_REGISTRY}/${ECR_REPOSITORY}:${BUILD_NUMBER}"

        // =========================================================
        // DEPLOYMENT STATE
        // =========================================================
        DEPLOYMENT_STARTED = 'false'
        CONTAINER_1_CHANGED = 'false'
        CONTAINER_2_CHANGED = 'false'
        CONTAINER_3_CHANGED = 'false'

        PREVIOUS_IMAGE_1 = ''
        PREVIOUS_IMAGE_2 = ''
        PREVIOUS_IMAGE_3 = ''
    }

    stages {

        // =========================================================
        // APPROVAL 1
        // =========================================================
        stage('Approval 1 - Start Pipeline') {
            steps {

                echo '''
==================================================
APPROVAL 1
==================================================
GitHub change detected.

The CI pipeline is ready to start.

Approve to continue with Maven build and Docker build.
==================================================
'''

                input(
                    id: 'startPipelineApproval',
                    message: "Start CI pipeline for Build #${env.BUILD_NUMBER}?",
                    ok: 'Start Pipeline',
                    submitter: params.DEPLOY_APPROVER
                )
            }
        }

        // =========================================================
        // MAVEN BUILD
        // =========================================================
        stage('Maven Build') {
            steps {

                echo '===== Maven Build Started ====='

                sh '''
                    set -e

                    mvn clean package

                    echo ""
                    echo "===== Build Artifacts ====="
                    ls -lah target/

                    echo ""
                    echo "===== Maven Build Completed ====="
                '''
            }
        }

        // =========================================================
        // DOCKER BUILD
        // =========================================================
        stage('Docker Build') {
            steps {

                echo '===== Docker Build Started ====='

                sh '''
                    set -e

                    docker build \
                        -t ${IMAGE_NAME}:${BUILD_NUMBER} \
                        .

                    echo ""
                    echo "===== Docker Image Created ====="

                    docker images ${IMAGE_NAME}
                '''

                echo '===== Docker Build Completed ====='
            }
        }

        // =========================================================
        // APPROVAL 2
        // =========================================================
        stage('Approval 2 - Promote Artifact') {
            steps {

                echo '''
==================================================
APPROVAL 2
==================================================
Maven build and Docker image creation succeeded.

Approve to continue with ECR login, tagging and push.
==================================================
'''

                input(
                    id: 'artifactApproval',
                    message: "Promote Docker artifact ${IMAGE_NAME}:${env.BUILD_NUMBER} to ECR?",
                    ok: 'Promote Artifact',
                    submitter: params.DEPLOY_APPROVER
                )
            }
        }

        // =========================================================
        // ECR LOGIN
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
        // DOCKER TAG
        // =========================================================
        stage('Docker Tag') {
            steps {

                echo '===== Docker Tag Started ====='

                sh '''
                    set -e

                    docker tag \
                        ${IMAGE_NAME}:${BUILD_NUMBER} \
                        ${ECR_IMAGE}

                    echo ""
                    echo "===== Image Tagged ====="
                    echo "${ECR_IMAGE}"
                '''
            }
        }

        // =========================================================
        // ECR PUSH
        // =========================================================
        stage('Push Image to ECR') {
            steps {

                echo '===== ECR Push Started ====='

                sh '''
                    set -e

                    docker push ${ECR_IMAGE}

                    echo ""
                    echo "===== Image Successfully Pushed ====="
                    echo "${ECR_IMAGE}"
                '''
            }
        }

        // =========================================================
        // ECR VERIFICATION
        // =========================================================
        stage('Verify ECR Image') {
            steps {

                echo '===== ECR Image Verification Started ====='

                sh '''
                    set -e

                    aws ecr describe-images \
                        --repository-name ${ECR_REPOSITORY} \
                        --image-ids imageTag=${BUILD_NUMBER} \
                        --region ${AWS_REGION}

                    echo ""
                    echo "===== ECR Image Verification Successful ====="
                '''
            }
        }

        // =========================================================
        // APPROVAL 3
        // =========================================================
        stage('Approval 3 - Deploy to EC2') {
            steps {

                echo '''
==================================================
APPROVAL 3
==================================================
Docker image is successfully available and verified
in Amazon ECR.

Approve to deploy this image to EC2.
==================================================
'''

                input(
                    id: 'deploymentApproval',
                    message: "Deploy ${env.ECR_IMAGE} to EC2?",
                    ok: 'Deploy to EC2',
                    submitter: params.DEPLOY_APPROVER
                )
            }
        }

        // =========================================================
        // CAPTURE CURRENT STATE
        // =========================================================
        stage('Capture Current Deployment') {
            steps {

                echo '''
==================================================
CAPTURING CURRENT DEPLOYMENT STATE
==================================================
'''

                script {

                    def image1 = sh(
                        script: """
                            docker inspect ${CONTAINER_1} \
                            --format '{{.Config.Image}}' 2>/dev/null || true
                        """,
                        returnStdout: true
                    ).trim()

                    def image2 = sh(
                        script: """
                            docker inspect ${CONTAINER_2} \
                            --format '{{.Config.Image}}' 2>/dev/null || true
                        """,
                        returnStdout: true
                    ).trim()

                    def image3 = sh(
                        script: """
                            docker inspect ${CONTAINER_3} \
                            --format '{{.Config.Image}}' 2>/dev/null || true
                        """,
                        returnStdout: true
                    ).trim()

                    env.PREVIOUS_IMAGE_1 = image1
                    env.PREVIOUS_IMAGE_2 = image2
                    env.PREVIOUS_IMAGE_3 = image3

                    echo "Container 1 previous image: ${image1}"
                    echo "Container 2 previous image: ${image2}"
                    echo "Container 3 previous image: ${image3}"
                }
            }
        }

        // =========================================================
        // MARK DEPLOYMENT STARTED
        // =========================================================
        stage('Start Deployment') {
            steps {

                script {
                    env.DEPLOYMENT_STARTED = 'true'
                }

                echo '''
==================================================
DEPLOYMENT STARTED
==================================================
'''
            }
        }

        // =========================================================
        // DEPLOY CONTAINER 1
        // =========================================================
        stage('Deploy Container 1') {
            steps {

                echo "===== Deploying ${CONTAINER_1} ====="

                script {

                    try {

                        sh """
                            set -e

                            echo "Stopping ${CONTAINER_1}..."
                            docker stop ${CONTAINER_1} 2>/dev/null || true

                            echo "Removing ${CONTAINER_1}..."
                            docker rm ${CONTAINER_1} 2>/dev/null || true

                            echo "Starting ${CONTAINER_1}..."

                            docker run -d \
                                --name ${CONTAINER_1} \
                                --network ${DOCKER_NETWORK} \
                                ${ECR_IMAGE}

                            echo "Waiting for application..."
                            sleep 5

                            echo "Running health check..."

                            docker exec ${CONTAINER_1} \
                                curl --fail \
                                http://localhost:${APP_PORT}

                            echo ""
                            echo "${CONTAINER_1} health check PASSED"
                        """

                        env.CONTAINER_1_CHANGED = 'true'

                    } catch (Exception e) {

                        echo "ERROR: ${CONTAINER_1} deployment failed."

                        throw e
                    }
                }
            }
        }

        // =========================================================
        // DEPLOY CONTAINER 2
        // =========================================================
        stage('Deploy Container 2') {
            steps {

                echo "===== Deploying ${CONTAINER_2} ====="

                script {

                    try {

                        sh """
                            set -e

                            echo "Stopping ${CONTAINER_2}..."
                            docker stop ${CONTAINER_2} 2>/dev/null || true

                            echo "Removing ${CONTAINER_2}..."
                            docker rm ${CONTAINER_2} 2>/dev/null || true

                            echo "Starting ${CONTAINER_2}..."

                            docker run -d \
                                --name ${CONTAINER_2} \
                                --network ${DOCKER_NETWORK} \
                                ${ECR_IMAGE}

                            echo "Waiting for application..."
                            sleep 5

                            echo "Running health check..."

                            docker exec ${CONTAINER_2} \
                                curl --fail \
                                http://localhost:${APP_PORT}

                            echo ""
                            echo "${CONTAINER_2} health check PASSED"
                        """

                        env.CONTAINER_2_CHANGED = 'true'

                    } catch (Exception e) {

                        echo "ERROR: ${CONTAINER_2} deployment failed."

                        throw e
                    }
                }
            }
        }

        // =========================================================
        // DEPLOY CONTAINER 3
        // =========================================================
        stage('Deploy Container 3') {
            steps {

                echo "===== Deploying ${CONTAINER_3} ====="

                script {

                    try {

                        sh """
                            set -e

                            echo "Stopping ${CONTAINER_3}..."
                            docker stop ${CONTAINER_3} 2>/dev/null || true

                            echo "Removing ${CONTAINER_3}..."
                            docker rm ${CONTAINER_3} 2>/dev/null || true

                            echo "Starting ${CONTAINER_3}..."

                            docker run -d \
                                --name ${CONTAINER_3} \
                                --network ${DOCKER_NETWORK} \
                                ${ECR_IMAGE}

                            echo "Waiting for application..."
                            sleep 5

                            echo "Running health check..."

                            docker exec ${CONTAINER_3} \
                                curl --fail \
                                http://localhost:${APP_PORT}

                            echo ""
                            echo "${CONTAINER_3} health check PASSED"
                        """

                        env.CONTAINER_3_CHANGED = 'true'

                    } catch (Exception e) {

                        echo "ERROR: ${CONTAINER_3} deployment failed."

                        throw e
                    }
                }
            }
        }

        // =========================================================
        // VERIFY ALL CONTAINERS
        // =========================================================
        stage('Verify Deployment') {
            steps {

                echo '''
==================================================
VERIFYING DEPLOYMENT
==================================================
'''

                sh '''
                    set -e

                    for CONTAINER in \
                        ${CONTAINER_1} \
                        ${CONTAINER_2} \
                        ${CONTAINER_3}
                    do

                        echo ""
                        echo "Checking ${CONTAINER}..."

                        RUNNING=$(docker inspect ${CONTAINER} \
                            --format '{{.State.Running}}')

                        IMAGE=$(docker inspect ${CONTAINER} \
                            --format '{{.Config.Image}}')

                        echo "Running: ${RUNNING}"
                        echo "Image:   ${IMAGE}"

                        if [ "${RUNNING}" != "true" ]; then
                            echo "ERROR: ${CONTAINER} is not running."
                            exit 1
                        fi

                        if [ "${IMAGE}" != "${ECR_IMAGE}" ]; then
                            echo "ERROR: ${CONTAINER} is running wrong image."
                            exit 1
                        fi

                        echo "${CONTAINER}: verification PASSED"

                    done
                '''
            }
        }

        // =========================================================
        // NGINX FINAL CHECK
        // =========================================================
        stage('Nginx Verification') {
            steps {

                echo '===== Nginx Verification Started ====='

                sh '''
                    set -e

                    echo "Testing application through Nginx..."

                    curl --fail http://localhost

                    echo ""
                    echo "===== Nginx Health Check PASSED ====="
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
Docker image built.
Artifact approved.
Image pushed to ECR.
ECR image verified.
Deployment approved.
All application containers deployed.
All application containers passed health checks.
Image versions verified.
Nginx traffic verified.
==================================================
'''
        }

        failure {

            script {

                echo '''
==================================================
PIPELINE FAILURE DETECTED
==================================================
'''

                if (env.DEPLOYMENT_STARTED == 'true') {

                    echo '''
Deployment had already started.
Automatic rollback will be attempted.
==================================================
'''

                    try {

                        // -------------------------------------------------
                        // ROLLBACK CONTAINER 1
                        // -------------------------------------------------
                        if (env.CONTAINER_1_CHANGED == 'true' &&
                            env.PREVIOUS_IMAGE_1?.trim()) {

                            echo "Rolling back ${CONTAINER_1}..."

                            sh """
                                set -e

                                docker stop ${CONTAINER_1} 2>/dev/null || true
                                docker rm ${CONTAINER_1} 2>/dev/null || true

                                docker run -d \
                                    --name ${CONTAINER_1} \
                                    --network ${DOCKER_NETWORK} \
                                    ${PREVIOUS_IMAGE_1}

                                sleep 5

                                docker exec ${CONTAINER_1} \
                                    curl --fail \
                                    http://localhost:${APP_PORT}

                                echo "${CONTAINER_1} rollback PASSED"
                            """
                        }

                        // -------------------------------------------------
                        // ROLLBACK CONTAINER 2
                        // -------------------------------------------------
                        if (env.CONTAINER_2_CHANGED == 'true' &&
                            env.PREVIOUS_IMAGE_2?.trim()) {

                            echo "Rolling back ${CONTAINER_2}..."

                            sh """
                                set -e

                                docker stop ${CONTAINER_2} 2>/dev/null || true
                                docker rm ${CONTAINER_2} 2>/dev/null || true

                                docker run -d \
                                    --name ${CONTAINER_2} \
                                    --network ${DOCKER_NETWORK} \
                                    ${PREVIOUS_IMAGE_2}

                                sleep 5

                                docker exec ${CONTAINER_2} \
                                    curl --fail \
                                    http://localhost:${APP_PORT}

                                echo "${CONTAINER_2} rollback PASSED"
                            """
                        }

                        // -------------------------------------------------
                        // ROLLBACK CONTAINER 3
                        // -------------------------------------------------
                        if (env.CONTAINER_3_CHANGED == 'true' &&
                            env.PREVIOUS_IMAGE_3?.trim()) {

                            echo "Rolling back ${CONTAINER_3}..."

                            sh """
                                set -e

                                docker stop ${CONTAINER_3} 2>/dev/null || true
                                docker rm ${CONTAINER_3} 2>/dev/null || true

                                docker run -d \
                                    --name ${CONTAINER_3} \
                                    --network ${DOCKER_NETWORK} \
                                    ${PREVIOUS_IMAGE_3}

                                sleep 5

                                docker exec ${CONTAINER_3} \
                                    curl --fail \
                                    http://localhost:${APP_PORT}

                                echo "${CONTAINER_3} rollback PASSED"
                            """
                        }

                        // -------------------------------------------------
                        // VERIFY NGINX AFTER ROLLBACK
                        // -------------------------------------------------
                        echo "Verifying Nginx after rollback..."

                        sh '''
                            set -e

                            curl --fail http://localhost

                            echo ""
                            echo "Nginx rollback verification PASSED"
                        '''

                        echo '''
==================================================
ROLLBACK COMPLETED SUCCESSFULLY
==================================================
Previous deployment has been restored.
==================================================
'''

                    } catch (Exception rollbackError) {

                        echo '''
==================================================
CRITICAL: ROLLBACK FAILED
==================================================
Manual investigation is required.
==================================================
'''

                        echo "Rollback error: ${rollbackError}"

                        throw rollbackError
                    }

                } else {

                    echo '''
==================================================
NO DEPLOYMENT WAS STARTED
==================================================
No application rollback is required.
EC2 deployment was not modified.
==================================================
'''
                }
            }
        }

        aborted {

            echo '''
==================================================
PIPELINE ABORTED
==================================================
'''

            script {

                if (env.DEPLOYMENT_STARTED == 'true') {

                    echo '''
Deployment had already started before pipeline abort.
Manual deployment verification is recommended.
==================================================
'''

                } else {

                    echo '''
Deployment had not started.
Existing application remains untouched.
==================================================
'''
                }
            }
        }

        always {

            echo '''
==================================================
PIPELINE EXECUTION FINISHED
==================================================
'''
        }
    }
}
