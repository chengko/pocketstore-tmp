// src/com/makewish/pocketstore/UploadArtifactsArgs.groovy

package com.makewish.pocketstore

class ConfigureSiteArgs implements Serializable {

    String site
    String gatewayAddress = '130.211.246.195:8590'
    String bundleUrl = 'http://13.114.245.218:17080/PS/bundles/GamaniaAWSDaily'

    ConfigureSiteArgs(Map source) {
        source.each{ key, value -> 
            if(this[key] instanceof Boolean) {
                this[key] = Boolean.valueOf(value)
            } else {
                this[key] = value
            }
        }
    }
}