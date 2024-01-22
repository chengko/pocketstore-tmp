def configure(String projectName, String buildSetting, String privateSite) {
    stage('Configure') {
        agent { 
            node {
                label 'unityci' 
            }
        }
        steps {
            script {
                if (buildSetting == '(Private)') {
                    
                    def destDir = sh(returnStdout: true, script: "echo ~/Documents/config/$projectName").toString().trim()
                    
                    BUILD_SETTINGS = "${destDir}/${privateSite}.json"
                    
                    sh "mkdir -p ${destDir}"
                    configFileProvider([configFile(fileId: "pocketstore-webconfig-${privateSite}", targetLocation: 'webconfig.json', variable: 'webconfigFile')]) {
                        sh "cp webconfig.json ${BUILD_SETTINGS}"
                    }    
                }
            }
        }
    }
}