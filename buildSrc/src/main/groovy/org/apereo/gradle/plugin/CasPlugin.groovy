package org.apereo.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jose4j.jwk.JsonWebKey
import org.jose4j.jwk.OctJwkGenerator
import org.springframework.boot.gradle.SpringBootPlugin

/**
 * Created by jj on 4/12/16.
 */
class CasPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        if (!project.plugins.hasPlugin(SpringBootPlugin)) {
            project.apply plugin: SpringBootPlugin
        }

        project.extensions.create('cas', CasPluginExtension)
        project.repositories {
            mavenLocal()
            mavenCentral()
            jcenter()

            maven { url "https://oss.sonatype.org/content/repositories/snapshots" }
            maven { url "http://repo.maven.apache.org/maven2" }
            maven { url "https://jitpack.io" }
            maven { url "http://developer.jasig.org/repo/content/groups/m2-legacy/" }
            maven { url "https://build.shibboleth.net/nexus/content/repositories/releases" }
            maven { url "http://files.couchbase.com/maven2" }
            maven { url "http://repo.spring.io/milestone" }
            maven { url "https://dl.bintray.com/uniconiam/maven"}
        }

        project.afterEvaluate {
            println "CAS Version: ${project.cas.version}"
            println "CAS support modules: ${project.cas.support}"
            println "CAS integration modules: ${project.cas.integration}"
            project.dependencies {
                compile("org.jasig.cas:cas-server-webapp:${project.cas.version}:resources") {
                    transitive = true
                }
                project.cas.support.each {
                    compile("org.jasig.cas:cas-server-support-${it}:${project.cas.version}")
                }
                project.cas.integration.each {
                    compile("org.jasig.cas:cas-server-integration-${it}:${project.cas.version}")
                }
            }
        }

        project.task('generateKeys') << {
            println 'Generating keys for CAS'
            ['tgc.encryption.key': 256, 'tgc.signing.key': 512, 'webflow.encryption.key': 96, 'webflow.signing.key': 512].each { key, size ->
                def octetKey = OctJwkGenerator.generateJwk(size)
                def params = octetKey.toParams(JsonWebKey.OutputControlLevel.INCLUDE_SYMMETRIC)
                println "${key}=${params.get('k')}"
            }
        }
    }
}
