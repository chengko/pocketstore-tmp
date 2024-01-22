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

                    def content = """
                    {
                      "SkipWebConfig": false,
                      "WebConfigIndexFileUrl": "http://dev.make-wish.club/pocketstore-web-config/${privateSite}/IndexFile.json",
                      "LocalSite": "Private_${privateSite}"
                    }
                    """
                    
                    writeFile file: BUILD_SETTINGS, text: content
                }
            }
        }
    }
}