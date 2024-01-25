def configureSite(buildSetting, privateSite) {

    def result = [
        File: "BatchBuildSettings/WebConfig/WebConfig.${buildSetting}.json",
        Site: buildSetting
    ]
    if (buildSetting == '(Private)') {
        
        node('unityci') {
            def destDir = sh(returnStdout: true, script: "echo ~/Documents/config/pocketstore").toString().trim()
            
            result.File = "${destDir}/${privateSite}.json"
            result.Site = privateSite

            writeWebConfig(destDir, privateSite)

            copyArtifacts parameters: "Name=${privateSite}", projectName: 'PocketStore/Generate GlobalSetting', selector: lastSuccessful()
            def gameCode = sh(script: "jq -r '.GameCode' GlobalSettings.json", returnStdout:true).trim().toInteger()
            writeIndexFile(privateSite)
            writeSite(privateSite, '130.211.246.195:8590', gameCode + 1)
        }
    }

    return result
}

def writeWebConfig(destDir, site) {

    def content = """
    {
        "SkipWebConfig": false,
        "WebConfigIndexFileUrl": "http://dev.make-wish.club/pocketstore_web_config/${site}/IndexFile.json",
        "LocalSite": "Private_${site}"
    }
    """

    sh "mkdir -p ${destDir}"

    writeFile file: "${destDir}/${site}.json", text: content
}

def writeIndexFile(site) {
    echo "writeIndexFile"
    def content = """
    {
        "BaseUrl": "http://dev.make-wish.club/pocketstore-web-config/",
        "Files": [
            "CommonConfig.json",
            "Announcement.json",
            "${site}/Site.json"		
        ]
    }
    """

    writeToWeb(site, "IndexFile.json", content)
}

def writeSite(site, gateway, serverId) {
    echo "writeSite"
    def content = """
    {
        "BundleUrl": "http://13.114.245.218:17080/PS/bundles/GamaniaAWSDaily",
        "GatewayAddress": [
            "${gateway}"
        ],
        "LoginServiceIds": [
            ${serverId}
        ],
        "RemoteLogConfig": {
            "URL": "",
            "ShowFPS": false,
            "LogPerformaceInterval": 60.0
        }
    }
    """

    writeToWeb(site, "Site.json", content)
}

def writeToWeb(site, filename, content) {

    //sh "echo -e '${content}' > ${filename}"
    writeFile file: filename, text: content

    def WEB_CONFIG_ROOT = "/home/ssl-web/pocketstore_web_config"

    sh "docker exec hfs sh -c 'mkdir -p $WEB_CONFIG_ROOT/$site'"
    sh "docker cp ${file} hfs:$WEB_CONFIG_ROOT/$site/"
}