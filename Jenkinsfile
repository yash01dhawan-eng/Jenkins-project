pipeline {

    agent any

    parameters {
        string(
            name: 'DEPLOY_APPROVER',
            defaultValue: 'yash',
            description: 'Jenkins user allowed to approve deployment'
        )
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

        stage('Manual Approval') {
            steps {
                input(
                    id: 'deployApproval',
                    message: "Deploy this build to EC2? Approver: ${params.DEPLOY_APPROVER}",
                    ok: 'Approve Deployment',
                    submitter: params.DEPLOY_APPROVER
                )
            }
        }

        stage('Deploy') {
            steps {
                echo '===== Deploying Application ====='

                sh '''
                    cp target/jenkins-project-1.0.0.jar /opt/jenkins-project/app.jar

                    sudo /usr/bin/systemctl restart jenkins-project

                    echo "===== Deployment Completed ====="

                    sudo /usr/bin/systemctl status jenkins-project --no-pager
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
