import com.makewish.pocketstore.ConfigureSiteArgs

def configureSite(args) {

    def siteArgs = new ConfigureSiteArgs(args)

    def result = [
        File: "BatchBuildSettings/WebConfig/WebConfig.${siteArgs.buildSetting}.json",
        Site: siteArgs.buildSetting
    ]
    if (siteArgs.buildSetting == '(Private)') {
        
        node('unityci') {
            def destDir = sh(returnStdout: true, script: "echo ~/Documents/config/pocketstore").toString().trim()
            
            result.File = "${destDir}/${siteArgs.privateSite}.json"
            result.Site = siteArgs.privateSite

            writeWebConfig(destDir, siteArgs.privateSite)
        }

        copyArtifacts parameters: "Name=${siteArgs.privateSite}", projectName: 'PocketStore/Generate GlobalSetting', selector: lastSuccessful()
        def gameCode = sh(script: "jq -r '.GameCode' GlobalSettings.json", returnStdout:true).trim().toInteger()

        
        writeIndexFile(siteArgs.privateSite)
        writeSite(siteArgs, gameCode + 1)
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

def writeSite(siteArgs, serverId) {

    def content = """
    {
        "BundleUrl": "${siteArgs.bundleUrl}",
        "GatewayAddress": [
            "${siteArgs.gatewayAddress}"
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

    writeToWeb(siteArgs.privateSite, "Site.json", content)
}

def writeToWeb(site, filename, content) {
    writeFile file: filename, text: content

    def WEB_CONFIG_ROOT = "/home/ssl-web/pocketstore_web_config"

    sh "docker exec hfs sh -c 'mkdir -p $WEB_CONFIG_ROOT/$site'"
    sh "docker cp ${filename} hfs:$WEB_CONFIG_ROOT/$site/"
}