def secrets = [
    [path: params.VAULT_PATH_SVC_ACCOUNT_EPHEMERAL, engineVersion: 1, secretValues: [
        [envVar: 'OC_LOGIN_TOKEN', vaultKey: 'oc-login-token'],
        [envVar: 'OC_LOGIN_SERVER', vaultKey: 'oc-login-server']]],
    [path: params.VAULT_PATH_QUAY_PUSH, engineVersion: 1, secretValues: [
        [envVar: 'QUAY_USER', vaultKey: 'user'],
        [envVar: 'QUAY_TOKEN', vaultKey: 'token']]],
    [path: params.VAULT_PATH_RHR_PULL, engineVersion: 1, secretValues: [
        [envVar: 'RH_REGISTRY_USER', vaultKey: 'user'],
        [envVar: 'RH_REGISTRY_TOKEN', vaultKey: 'token']]]
]

def configuration = [vaultUrl: params.VAULT_ADDRESS, vaultCredentialId: params.VAULT_CREDS_ID, engineVersion: 1]

pipeline {
    agent { label 'insights' }
    options {
        timestamps()
    }
    environment {
        // --------------------------------------------
        // Options that must be configured by app owner
        // --------------------------------------------
        APP_NAME="runtimes-inventory"  // name of app-sre "application" folder this component lives in
        COMPONENT_NAME="runtimes-inventory"  // name of app-sre "resourceTemplate" in deploy.yaml for this component

        IQE_PLUGINS="runtimes-inventory"  // name of the IQE plugin for this app.
        IQE_MARKER_EXPRESSION="not full_stack"  // This is the value passed to pytest -m
        IQE_FILTER_EXPRESSION=""  // This is the value passed to pytest -k
        IQE_CJI_TIMEOUT="30m"  // This is the time to wait for smoke test to complete or fail

        IMAGE="quay.io/cloudservices/insights-rbi-events" // set the name of the first image
        IMAGE_TAG_TMP=sh(script: "git rev-parse --short=7 HEAD", returnStdout: true).trim() // get the base image tag using the commit id
        IMAGE_TAG=sh(script: "if [ ! -z ${ghprbPullId} ]; then echo "pr-${ghprbPullId}-${IMAGE_TAG_TMP}"; else echo ${IMAGE_TAG_TMP}; fi", returnStdout: true).trim() // if this is a PR, use a different tag
        EXTRA_DEPLOY_ARGS="--set-image-tag quay.io/cloudservices/insights-rbi-rest=${IMAGE_TAG}" // pass the second image as an extra arg

        CICD_URL="https://raw.githubusercontent.com/RedHatInsights/cicd-tools/main"
    }
    stages {
        stage('Build the PR commit image') {
            steps {
                withVault([configuration: configuration, vaultSecrets: secrets]) {
                    sh './build_deploy.sh'
                }
            }
        }

        // stage('Run Tests') {
            // parallel {
            //     stage('Run unit tests') {
            //         steps {
            //             withVault([configuration: configuration, vaultSecrets: secrets]) {
            //                 sh 'bash -x scripts/unit_test.sh'
            //             }
            //         }
            //     }

                stage('Run smoke tests') {
                    environment {
                        DEPLOY_TIMEOUT="900"  // 15min
                    }
                    steps {
                        withVault([configuration: configuration, vaultSecrets: secrets]) {
                            sh '''
                                curl -s $CICD_URL/bootstrap.sh > .cicd_bootstrap.sh
                                source ./.cicd_bootstrap.sh
                                source "${CICD_ROOT}/deploy_ephemeral_env.sh"
                                source "${CICD_ROOT}/cji_smoke_test.sh"
                            '''
                        }
                    }
                }
            // }
        // }
    }

    post {
        always{
            archiveArtifacts artifacts: 'artifacts/**/*', fingerprint: true
            junit skipPublishingChecks: true, testResults: 'artifacts/*.xml'
        }
    }
}
