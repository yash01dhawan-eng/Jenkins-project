Java Application CI/CD Pipeline on AWS

A production-style CI/CD learning project that automates the build, containerization, image publishing, and deployment of a Java/Maven application on an AWS EC2 instance.

The project uses GitHub, Jenkins, Maven, Docker, Amazon ECR, Docker networking, and Nginx. The Jenkins pipeline also includes manual approval gates, health checks, deployment verification, and failure-aware automatic rollback.

Architecture

                         GitHub
                            |
                       git push
                            |
                            v
                    GitHub Webhook
                            |
                            v
                         Jenkins
                            |
                     Approval #1
                    Start Pipeline?
                            |
                            v
                      Maven Build
                            |
                            v
                      Docker Build
                            |
                     Approval #2
                  Promote Artifact?
                            |
                            v
                       Amazon ECR
                            |
                     ECR Verification
                            |
                     Approval #3
                   Deploy to EC2?
                            |
                            v
                          EC2
                            |
                +-----------+-----------+
                |                       |
              Nginx              Docker Network
              :80                       |
                |              +--------+--------+
                |              |        |        |
                +-----------> App-1   App-2    App-3
                             8081     8081     8081
                                |        |        |
                                +--------+--------+
                                         |
                                  Health Checks
                                         |
                                         v
                                   Verification
                                         |
                              +----------+----------+
                              |                     |
                           SUCCESS                FAILURE
                                                    |
                                                    v
                                              Auto Rollback

Technology Stack

GitHub

Jenkins

Java

Maven

Docker

Amazon ECR

Amazon EC2

AWS IAM Role

Nginx

Docker Custom Network

Linux / Ubuntu

Project Flow

1. Developer Pushes Code

Application source code and the Jenkinsfile are stored in GitHub.

git push
   |
   v
GitHub
   |
   v
Jenkins Webhook
   |
   v
Pipeline Triggered

Jenkins is configured with a GitHub push webhook, so a new commit can automatically trigger the pipeline.

2. Approval #1 - Start Pipeline

The pipeline first asks for manual approval.

Purpose:

Decide whether the CI pipeline should actually start after a GitHub change is detected.

GitHub Push
    |
    v
Approval #1
    |
    v
Maven Build

If the approval is rejected/aborted before deployment starts, no EC2 deployment changes are made.

3. Maven Build

Jenkins builds the Java application using:

mvn clean package

The generated JAR is placed under:

target/

Jenkins also displays the generated build artifacts in the console.

4. Docker Build

The application is packaged into a Docker image.

The image uses the Jenkins build number as its version:

docker build -t javaimage:${BUILD_NUMBER} .

Example:

Build #11
    |
    v
javaimage:11

This gives every Jenkins build a unique image version.

5. Approval #2 - Promote Artifact

After Maven and Docker build successfully complete, Jenkins asks for a second approval.

Purpose:

Approve promotion of the generated Docker artifact to Amazon ECR.

Maven Build
    |
Docker Build
    |
Approval #2
    |
ECR

6. AWS IAM Authentication

AWS credentials are not hardcoded inside the Jenkinsfile.

The EC2 instance uses an IAM role that allows Jenkins to interact with Amazon ECR.

The identity can be verified with:

sudo -u jenkins aws sts get-caller-identity

This provides role-based authentication without putting AWS access keys inside the pipeline.

7. Amazon ECR

The Docker image is tagged with the ECR repository and Jenkins build number.

Current repository:

jenkins-project/cicd

Region:

us-east-1

Example image:

394281571893.dkr.ecr.us-east-1.amazonaws.com/jenkins-project/cicd:11

The pipeline performs:

Docker Build
      |
      v
Docker Tag
      |
      v
Docker Push
      |
      v
Amazon ECR
      |
      v
ECR Verification

The pipeline verifies that the image actually exists in ECR before deployment is allowed to continue.

8. Approval #3 - Deploy to EC2

After the image is successfully pushed and verified in ECR, Jenkins asks for the third approval.

Purpose:

Explicitly approve deployment of the verified ECR image to the EC2 environment.

ECR Image
    |
ECR Verification
    |
Approval #3
    |
    v
EC2 Deployment

Docker Environment

Custom Docker Network

A custom Docker network named:

app-network

is used for application and Nginx communication.

It can be created using:

docker network create app-network

Containers on this network can communicate using Docker DNS/container names.

Example:

docker network inspect app-network

Application Containers

Three instances of the Java application are deployed:

javacontainer
javacontainer-2
javacontainer-3

All three use the same ECR image version during a deployment.

Example:

javacontainer    -> cicd:11
javacontainer-2  -> cicd:11
javacontainer-3  -> cicd:11

The Java application listens on:

8081

The backend containers do not need to expose their application port directly to the EC2 host because Nginx communicates with them through the Docker network.

Nginx

Nginx runs as a separate Docker container:

nginxcontainer

Nginx listens on:

EC2 :80

Traffic flow:

Client
  |
  v
EC2 Port 80
  |
  v
Nginx
  |
  v
Java Application Containers

Nginx is connected to the same Docker network as the application containers.

Health Checks

Each application container is checked after deployment.

The Jenkins pipeline performs a health check similar to:

docker exec <container> curl --fail http://localhost:8081

Example:

javacontainer    -> HEALTHY
javacontainer-2  -> HEALTHY
javacontainer-3  -> HEALTHY

If the health check fails, the deployment pipeline fails and the failure-aware rollback logic is triggered when deployment has already started.

Deployment Verification

After all containers are deployed, Jenkins verifies:

1. Container is running

The pipeline checks:

.State.Running == true

2. Correct image is running

The pipeline compares the expected image with the image actually running inside the container.

Example:

Expected:
.../cicd:11

Actual:
.../cicd:11

If the images do not match, the pipeline fails.

3. Nginx traffic works

The pipeline finally checks:

curl --fail http://localhost

This validates the complete path:

Jenkins
   |
EC2
   |
Nginx
   |
Docker Network
   |
Java Application

Failure-Aware Deployment

The Jenkinsfile keeps track of the previous deployment state before making application changes.

Example:

Current deployment:

App-1 -> cicd:10
App-2 -> cicd:10
App-3 -> cicd:10

New deployment:

cicd:11

Before deployment, Jenkins captures the previous image for each container.

If a deployment fails after one or more containers have been changed, Jenkins attempts to restore the previously running images.

Example:

App-1 -> cicd:11  SUCCESS
App-2 -> cicd:11  FAILED
App-3 -> cicd:10  untouched

Rollback:

App-1 -> cicd:10
App-2 -> cicd:10
App-3 -> cicd:10

Rollback health checks are also performed.

Failure Handling

The pipeline distinguishes between failures that require rollback and failures that do not.

Build Failure

Maven Build
    |
    X
Pipeline FAILED

No deployment rollback is required because EC2 was not modified.

ECR Failure

Docker Build
    |
ECR Push
    |
    X
Pipeline FAILED

Deployment does not start, so the existing application remains untouched.

Approval Rejected

If a deployment approval is rejected before deployment starts:

Pipeline Aborted
       |
       v
Existing deployment remains unchanged

Deployment Failure

If an application container fails during deployment:

Deployment
    |
Container Failure
    |
    v
Pipeline Failure
    |
    v
Automatic Rollback

Rollback Failure

If rollback itself fails, the pipeline reports:

ROLLBACK FAILED

and manual investigation is required.

The pipeline intentionally does not hide rollback failures.

Current Deployment Strategy

The current implementation is:

Sequential Deployment + Health Checks + Automatic Rollback

It is not yet a true zero-downtime rolling deployment.

Current replacement flow:

Old Container
     |
   STOP
     |
   REMOVE
     |
New Container
     |
 Health Check

Therefore, a short reduction in available backend capacity can occur during replacement.

The next planned evolution is:

Zero-Downtime Rolling Deployment

followed by Kubernetes/EKS and GitOps.

Current Successful Deployment

The latest verified deployment used:

ECR Image:
394281571893.dkr.ecr.us-east-1.amazonaws.com/jenkins-project/cicd:11

Running containers:

javacontainer    -> cicd:11
javacontainer-2  -> cicd:11
javacontainer-3  -> cicd:11
nginxcontainer   -> running

Jenkins verified:

Docker image built
Artifact approved
Image pushed to ECR
ECR image verified
Deployment approved
All application containers deployed
All application containers passed health checks
Image versions verified
Nginx traffic verified

Pipeline result:

Finished: SUCCESS

Jenkinsfile Stages

The current failure-aware Jenkins pipeline contains these major stages:

1. Approval 1 - Start Pipeline
2. Maven Build
3. Docker Build
4. Approval 2 - Promote Artifact
5. ECR Login
6. Docker Tag
7. Push Image to ECR
8. Verify ECR Image
9. Approval 3 - Deploy to EC2
10. Capture Current Deployment
11. Start Deployment
12. Deploy Container 1
13. Deploy Container 2
14. Deploy Container 3
15. Verify Deployment
16. Nginx Verification

Post actions handle:

SUCCESS
FAILURE
ABORTED
ROLLBACK
ROLLBACK VERIFICATION

GitOps Evolution

The current project follows Git-centric CI/CD practices, but it is not pure GitOps yet.

Current:

GitHub
   |
Jenkins
   |
ECR
   |
Jenkins deploys to EC2

Future GitOps architecture:

GitHub
   |
Jenkins
   |
ECR
   |
GitOps Repository
   |
Argo CD
   |
EKS
   |
Kubernetes

In a GitOps implementation, Git would contain the desired deployment state, while a GitOps controller such as Argo CD would continuously reconcile that desired state with the Kubernetes environment.

Future Improvements

Planned evolution:

Zero-downtime rolling deployment

Blue/Green deployment

Kubernetes/EKS

Kubernetes Deployment and Service

Readiness and Liveness Probes

Kubernetes Ingress / AWS Load Balancer

Helm or Kustomize

Argo CD

Full GitOps workflow

Automated deployment history and rollback

Production-grade observability with Prometheus/Grafana/CloudWatch

Key Learning Outcomes

This project demonstrates practical understanding of:

Git-based source control

GitHub webhooks

Jenkins CI/CD

Maven builds

Docker image creation

Docker image versioning

Amazon ECR

AWS IAM roles

EC2 deployments

Docker networking

Docker DNS/service discovery

Nginx reverse proxy

Multiple application containers

Manual deployment approvals

Health checks

Deployment verification

Failure-aware deployments

Automatic rollback

Immutable image versions

CI/CD troubleshooting

Current Status

GitHub CI/CD                  ✅
Jenkins Webhook               ✅
Maven Build                   ✅
Docker Build                  ✅
Amazon ECR                    ✅
IAM Role Authentication       ✅
3 Docker Application Nodes    ✅
Custom Docker Network         ✅
Nginx Reverse Proxy           ✅
Health Checks                 ✅
Image Verification            ✅
3 Approval Gates              ✅
Failure-Aware Pipeline        ✅
Automatic Rollback            ✅
