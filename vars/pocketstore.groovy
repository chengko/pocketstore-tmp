def configureBuild(String projectName, String buildSetting, String privateSite) {

    node('unityci') {
        if (buildSetting == '(Private)') {
            

            echo BUILD_SETTINGS

            def destDir = sh(returnStdout: true, script: "echo ~/Documents/config/$projectName").toString().trim()
            
            BUILD_SETTINGS = "${destDir}/${privateSite}.json"

            sh "mkdir -p ${destDir}"

            echo destDir

            echo BUILD_SETTINGS

            def BUILD_SETTINGS_CONTENT = """
            {
                "SkipWebConfig": false,
                "WebConfigIndexFileUrl": "http://dev.make-wish.club/pocketstore-web-config/${privateSite}/IndexFile.json",
                "LocalSite": "Private_${privateSite}"
            }
            """
            
            writeFile file: BUILD_SETTINGS, text: BUILD_SETTINGS_CONTENT
        }

        return BUILD_SETTINGS
    }
}