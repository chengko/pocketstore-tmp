// src/com/makewish/pocketstore/BuildDockerComposeArgs.groovy

package com.makewish.pocketstore

class BuildDockerComposeArgs implements Serializable {

    String site
    String template = ''
    Integer sharding = 1
    String network = 'csp-local'
    String version = 'snapshot'
    Boolean selfHosting

    Map<String, Map> services = [
        MasterService:  [
            Assembly: 'MasterService',
            IsMasterService: true,
            ServiceType: 'Master',
            ServiceIndex: 0
        ], 
        LoginService: [
            Assembly: 'LoginService',
            ServiceType: 'Login',
            ServiceIndex: 1
        ],
        GlobalService: [
            Assembly: 'GlobalService',
            ServiceType: 'Global',
            ServiceIndex: 6
        ],
        WebService: [
            Assembly: 'WebService',
            ServiceType: 'Web',
            ServiceIndex: 900,
            SaveAllRedisDataCountMaximum: 10000
        ]
    ]

    BuildDockerComposeArgs(Map source) {
        source.each{ key, value -> 
            if(this[key] instanceof Boolean) {
                this[key] = Boolean.valueOf(value)
            } else if(this[key] instanceof Integer) {
                this[key] = Integer.parseInt(value)
            } else {
                this[key] = value
            }
        }
    }
}