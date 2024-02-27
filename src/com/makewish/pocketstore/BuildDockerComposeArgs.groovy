// src/com/makewish/pocketstore/UploadArtifactsArgs.groovy

package com.makewish.pocketstore

class BuildDockerComposeArgs implements Serializable {

    String site
    String gameCode
    String globalSettingsPath = './GlobalSettings.json'
    String network = 'csp-local'
    String[] services
    Boolean isLocal

    BuildDockerComposeArgs(Map source) {
        source.each{ key, value -> 
            if(this[key] instanceof Boolean) {
                this[key] = Boolean.valueOf(value)
            } else {
                this[key] = value
            }
        }
    }
}