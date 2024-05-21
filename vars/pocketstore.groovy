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
        networks: [:],
        services: [:]
    ]

    def servicesYml = [
        Services: [:]
    ]

    dockerCompose.networks[siteArgs.network] = [:]

    if(siteArgs.selfHosting) {
        dockerCompose.networks[siteArgs.network].external = true
    } else {
        dockerCompose.networks[siteArgs.network].driver = 'bridge'
    }

    def globalSettings = siteArgs.site
    if(siteArgs.template != '') {
        globalSettings = siteArgs.template
    }

    copyArtifacts parameters: "Name=${globalSettings}", projectName: 'PocketStore/Generate GlobalSetting', selector: lastSuccessful()
    def gameCode = sh(script: "jq -r '.GameCode' GlobalSettings.json", returnStdout:true).trim().toInteger()

    for(i = 1; i <=siteArgs.sharding; i++) {
        def serviceName = sprintf('GameService%02d', i)
        siteArgs.services[serviceName] = [
            Assembly: 'GameService',
            ServiceType: 'Game',
            ServiceIndex: 10+i
        ]
    }

    sh "rm -rf Instances services.yml"
    
    siteArgs.services.each { serviceName, service ->
        echo "Processing service: $serviceName"
        sh "mkdir -p Instances/${serviceName}"
        writeJSON file: "Instances/$serviceName/LocalSettings.json", json: service

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
            if(service.ServiceType == 'Login' || service.ServiceType == 'Game') {
                // SITE-SERVICE-1
                def host = siteArgs.site.toLowerCase() + '-' + serviceName + '-1'
                servicesYml.Services[port] = [
                    Name: service.Assembly,
                    Host: host,
                    Port: port
                ]
            }
        } else {
            dockerCompose.services[serviceName]['ports'] = ["${port}:${port}"]
        }
    }
    
    writeYaml file: 'docker-compose.yml', data: dockerCompose, overwrite: true
    if(siteArgs.selfHosting) {
        writeYaml file: 'services.yml', data: servicesYml
    }
    
}

def buildHttpDockerCompose(args) {

    def siteArgs = new BuildDockerComposeArgs(args)

    def dockerCompose = [
        version: '3',
        networks: [:],
        services: [:]
    ]

    dockerCompose.networks[siteArgs.network] = [:]
    dockerCompose.networks[siteArgs.network].driver = 'bridge'
    dockerCompose.networks['csp'] = [:]
    dockerCompose.networks['csp'].external = true

    def globalSettings = siteArgs.site
    if(siteArgs.template != '') {
        globalSettings = siteArgs.template
    }

    copyArtifacts parameters: "Name=${globalSettings}", projectName: 'PocketStore/Generate GlobalSetting', selector: lastSuccessful()
    def gameCode = sh(script: "jq -r '.GameCode' GlobalSettings.json", returnStdout:true).trim().toInteger()

    for(i = 1; i <=siteArgs.sharding; i++) {
        def serviceName = sprintf('GameService%02d', i)
        siteArgs.services[serviceName] = [
            Assembly: 'GameService',
            ServiceType: 'Game',
            ServiceIndex: 10+i
        ]
    }

    sh "rm -rf Instances services.yml"
    
    siteArgs.services.each { serviceName, service ->
        echo "Processing service: $serviceName"
        sh "mkdir -p Instances/${serviceName}"
        writeJSON file: "Instances/$serviceName/LocalSettings.json", json: service

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
            networks: [ siteArgs.network, 'csp' ]
        ]
    }
    
    writeNginxConfig(siteArgs, gameCode)

    writeYaml file: 'docker-compose.yml', data: dockerCompose, overwrite: true
    
}

def configureSite(args) {

    def siteArgs = new ConfigureSiteArgs(args)

    copyArtifacts parameters: "Name=${siteArgs.site}", projectName: 'PocketStore/Generate GlobalSetting', selector: lastSuccessful()
    def gameCode = sh(script: "jq -r '.GameCode' GlobalSettings.json", returnStdout:true).trim().toInteger()

    
    writeIndexFile(siteArgs)
    writeSite(siteArgs, gameCode + 1)

    def url = "http://dev.make-wish.club/pocketstore_web_config/${siteArgs.site}/IndexFile.json"

    currentBuild.description = "<a href='${url}'>${url}</a>"
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
    """.stripIndent()

    writeToWeb(siteArgs.site, "IndexFile.json", content)
}

def writeSite(siteArgs, serverId) {

    def gatewayAddress = siteArgs.gatewayAddress.replace("{site}", siteArgs.site.toLowerCase())

    def content = """
    {
        "BundleUrl": "${siteArgs.bundleUrl}",
        "GatewayAddress": [
            "${gatewayAddress}"
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
    """.stripIndent()

    writeToWeb(siteArgs.site, "Site.json", content)
}

def writeToWeb(site, filename, content) {
    writeFile file: filename, text: content

    def WEB_CONFIG_ROOT = "/home/ssl-web/pocketstore_web_config"

    sh "docker exec hfs sh -c 'mkdir -p $WEB_CONFIG_ROOT/$site'"
    sh "docker cp ${filename} hfs:$WEB_CONFIG_ROOT/$site/."
}

def writeNginxConfig(siteArgs, gameCode) {

    def env = siteArgs.site.toLowerCase()
    def content = '''
    map $http_x_target $upstream_backend {
    '''

    siteArgs.services.each { serviceName, service ->
        if(service.ServiceType == 'Login' || service.ServiceType == 'Game') {
            def port = gameCode + service.ServiceIndex
            content += """
                ${port} ${env}-${serviceName}-1:${port};
            """
        }
    }

    content += '''
    }

    server {
        listen 80;
        server_name _;

        resolver 127.0.0.11 valid=30s;

        location /''' + env + ''' {
            rewrite ^/''' + env + '''(/.*)$ $1 break;

            if (\$upstream_backend = "") {
                return 404;
            }

            proxy_pass http://\$upstream_backend;
            proxy_set_header Host \$host;
            proxy_set_header X-Real-IP \$remote_addr;
            proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto \$scheme;
        }
    }
    '''

    content = content.stripIndent()

    writeFile file: "/data/nginx/conf/${env}.conf", text: content
}