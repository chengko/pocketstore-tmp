def configureBuild(String projectName, String buildSetting, String privateSite) {

    node('unityci') {
        def result = [
            File: "BatchBuildSettings/WebConfig/WebConfig.${buildSetting}.json",
            Site: buildSetting
        ]
        if (buildSetting == '(Private)') {
            
            def destDir = sh(returnStdout: true, script: "echo ~/Documents/config/$projectName").toString().trim()
            
            result.File = "${destDir}/${privateSite}.json"
            result.Site = privateSite

            sh "mkdir -p ${destDir}"

            echo destDir

            def content = """
            {
                "SkipWebConfig": false,
                "WebConfigIndexFileUrl": "http://dev.make-wish.club/pocketstore-web-config/${privateSite}/IndexFile.json",
                "LocalSite": "Private_${privateSite}"
            }
            """
            
            writeFile file: result.File, text: content
        }

        return result
    }
}