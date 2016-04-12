package org.apereo.gradle.plugin

class CasPluginExtension {
    String version
    Set integration = []
    Set support = []

    def cas(Closure c) {
        c.delegate = this
        c()
    }

    def methodMissing(String name, args) {
        this."${name}".addAll(args)
    }
}
