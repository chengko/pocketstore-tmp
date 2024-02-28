import com.makewish.pocketstore.BuildDockerComposeArgs
import com.makewish.pocketstore.ConfigureSiteArgs

def selectSite(buildSetting, privateSite) {
    def result = [
        File: "BatchBuildSettings/WebConfig/WebConfig.${buildSetting}.json",
        Site: buildSetting
    ]

    if (buildSetting == '(Private)') {
        
        node('unityci') {
            def destDir = sh(returnStdout: true, script: "echo ~/Documents/config/pocketstore").toString().trim()
            
            result.File = "${destDir}/${privateSite}.json"
            result.Site = privateSite

            def content = """
            {
                "SkipWebConfig": false,
                "WebConfigIndexFileUrl": "http://dev.make-wish.club/pocketstore_web_config/${privateSite}/IndexFile.json",
                "LocalSite": "Private_${privateSite}"
            }
            """

            sh "mkdir -p ${destDir}"

            writeFile file: "${destDir}/${privateSite}.json", text: content
        }
    }

    return result
}

def buildDockerCompose(args) {

    def siteArgs = new BuildDockerComposeArgs(args)

    def dockerCompose = [
        version: '3',
        networks: [
            "${siteArgs.network}": [:]
        ],
        services: [:]
    ]

    def servicesYml = [
        Services: [:]
    ]

    if(siteArgs.selfHosting) {
        dockerCompose.networks[siteArgs.network]['external'] = true
    } else {
        dockerCompose.networks[siteArgs.network]['driver'] = 'bridge'
    }

    def globalSettings = siteArgs.site
    if(siteArgs.template != '') {
        globalSettings = siteArgs.template
    }

    copyArtifacts parameters: "Name=${globalSettings}", projectName: 'PocketStore/Generate GlobalSetting', selector: lastSuccessful()
    def gameCode = sh(script: "jq -r '.GameCode' GlobalSettings.json", returnStdout:true).trim().toInteger()

    sh "mkdir Instances"

    for(i = 1; i <=siteArgs.sharding; i++) {
        def serviceName = sprintf('GameService%02d', i)
        siteArgs.services[serviceName] = [
            Assembly: 'GameService',
            ServiceType: 'Game',
            ServiceIndex: 10+i
        ]
    }
    
    siteArgs.services.each { serviceName, service ->
        echo "Processing service: $serviceName"
        sh "mkdir Instances/${serviceName}"
        writeJSON file: "$serviceName/LocalSettings.json", json: service, overwrite: true

        def port = gameCode + service.ServiceIndex
        
        dockerCompose.services[serviceName] =  
        [
            image: "pocketstore:${siteArgs.version}",
            environment: [
                HOSTNAME: serviceName,
                SERVICE: service.Assembly
            ],
            volumes: [
                "./GlobalSettings.json:/app/Deployment/DeployCore/Instances/GlobalSettings.json",
                "./Instances/${serviceName}:/app/Deployment/DeployCore/Instances/${serviceName}"
            ],
            networks: [ siteArgs.network ]
        ]

        if(siteArgs.selfHosting) {
            // SITE-SERVICE-1
            def host = siteArgs.site.toLower() + '-' + serviceName + '-1'
            servicesYml.Services[port] = [
                Name: service.Assembly,
                Host: host,
                Port: port
            ]
        } else {
            dockerCompose.services[serviceName]['ports'] = ["${port}:${port}"]
        }
    }
    
    writeYaml file: 'docker-compose.yml', data: dockerCompose, overwrite: true

    if(siteArgs.selfHosting) {
        writeYaml file: 'services.yml', data: servicesYml, overwrite: true
    }

}

def configureSite(args) {

    def siteArgs = new ConfigureSiteArgs(args)

    copyArtifacts parameters: "Name=${siteArgs.site}", projectName: 'PocketStore/Generate GlobalSetting', selector: lastSuccessful()
    def gameCode = sh(script: "jq -r '.GameCode' GlobalSettings.json", returnStdout:true).trim().toInteger()

    
    writeIndexFile(siteArgs)
    writeSite(siteArgs, gameCode + 1)
}

def writeIndexFile(siteArgs) {

    def content = """
    {
        "BaseUrl": "http://dev.make-wish.club/pocketstore_web_config/",
        "Files": [
            "CommonConfig.json",
            "Announcement.json",
            "${siteArgs.site}/Site.json"		
        ]
    }
    """

    writeToWeb(siteArgs.site, "IndexFile.json", content)
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

    writeToWeb(siteArgs.site, "Site.json", content)
}

def writeToWeb(site, filename, content) {
    writeFile file: filename, text: content

    def WEB_CONFIG_ROOT = "/home/ssl-web/pocketstore_web_config"

    sh "docker exec hfs sh -c 'mkdir -p $WEB_CONFIG_ROOT/$site'"
    sh "docker cp ${filename} hfs:$WEB_CONFIG_ROOT/$site/."
}