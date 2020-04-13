def SHORT_COMMIT;
def branch = '';

pipeline {
    agent { docker { image 'docker-registry.kabala.tech/node12-with-git:latest' } }
    environment {
        app = ''
        CI = 'true'
        GIT_SSH_COMMAND = "ssh -o StrictHostKeyChecking=no"
        GH_TOKEN = credentials('jenkins-github-accesstoken')
    }

    stages {
        stage ('checkout') {
            steps {
                checkout([
                        $class                           : 'GitSCM',
                        branches                         : [[name: "${ghprbActualCommit}"]],
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
        stage ('prepare') {
            steps {
                script {
                    SHORT_COMMIT = GIT_COMMIT.take(7)
                    env.SHORT_COMMIT = SHORT_COMMIT
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

                    env.GIT_BRANCH = branch
                    env.GIT_LOCAL_BRANCH = branch

                    sh "printenv"
                }
            }
        }
        stage ('Install dependencies') {
            steps {
                script {
                    sh "yarn"
                }
            }
        }
        stage ('Release') {
            steps {
                sshagent(['jenkins-ssh-key']) {
                    script {
                        sh "git checkout ${branch}"
                        sh "yarn semantic-release"
                    }
                }
            }
        }
    }
}
