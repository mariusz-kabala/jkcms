def branch = '';

pipeline {
    agent { docker { image 'docker-registry.kabala.tech/alpine-terraform:latest' } }

    environment {
        CI = 'true'
        DOCKER_REGISTRY_USERNAME = credentials('docker-registry-username')
        DOCKER_REGISTRY_PASSWORD = credentials('docker-registry-password')
        AWS_ACCESS_KEY_ID = credentials('SCALEWAY_S3_ACCESS_KEY')
        AWS_SECRET_ACCESS_KEY = credentials('SCALEWAY_S3_ACCESS_SECRET_KEY')
        GIT_SSH_COMMAND = "ssh -o StrictHostKeyChecking=no"
    }

    stages {
        stage ('prepare') {
            steps {
                script {
                    try {
                        branch = env.GIT_LOCAL_BRANCH
                        branch = branch ?: env.GIT_BRANCH
                        if (branch == 'detached') {
                            branch = ''
                        }
                        branch = branch ?: env.ghprbActualCommit
                    } catch (e) {
                        println "GIT BRANCH not detected"
                    }

                    sh 'git config user.name "jenkins-kabala.tech"'
                    sh 'git config user.email "jenkins@kabala.tech"'

                    if (!branch) {
                        error "GIT branch to process not found"
                    }

                    if (branch.startsWith('origin/')) {
                        branch = branch.replaceAll('origin/', '')
                    }

                    println "GIT branch to process: ${branch}"
                    manager.addShortText(branch, "white", "navy", "1px", "navy")

                    sh "mkdir -p ~/.ssh/"
                    sh "ssh-keyscan -t rsa -p21598 145.239.89.193  >> ~/.ssh/known_hosts"
                    
                    sh "printenv"
                }
            }
        }
        stage ('Checkout') {
            steps {
                    checkout([
                            $class                           : 'GitSCM',
                            branches                         : [[name: "${branch}"]],
                            browser                          : [$class: 'GithubWeb', repoUrl: "https://github.com/mariusz-kabala/jkcms"],
                            doGenerateSubmoduleConfigurations: false,
                            userRemoteConfigs                : [[
                                credentialsId: 'github',
                                refspec      : '+refs/pull/*:refs/remotes/origin/pr/*',
                                url          : "git@github.com:mariusz-kabala/jkcms.git"
                            ]]
                    ])
            }
        }
        stage ('Deploy') {
            steps {
                dir("terraform") {
                    sshagent(['jenkins-local-ssh-key']) {
                        script {
                            docker.withRegistry('https://docker-registry.kabala.tech', 'docker-registry-credentials') {
                                sh "terraform init"
                                sh "terraform workspace select ${env.DEPLOY_ENVIRONMENT} || terraform workspace new ${env.DEPLOY_ENVIRONMENT}"
                                sh "terraform plan -out deploy.plan -var-file=${env.DEPLOY_ENVIRONMENT}.tfvars -var=\"tag=${env.GIT_TAG}\" -var=\"DOCKER_REGISTRY_USERNAME=${DOCKER_REGISTRY_USERNAME}\" -var=\"DOCKER_REGISTRY_PASSWORD=${DOCKER_REGISTRY_PASSWORD}\"" 
                                sh "terraform apply -auto-approve deploy.plan"
                            }
                        }
                    }
                }
            }
        }
    }
}
