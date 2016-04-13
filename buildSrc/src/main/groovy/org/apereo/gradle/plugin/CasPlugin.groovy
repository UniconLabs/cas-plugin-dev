package org.apereo.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.file.archive.ZipFileTree
import org.jose4j.jwk.JsonWebKey
import org.jose4j.jwk.OctJwkGenerator
import org.springframework.boot.gradle.SpringBootPlugin

import java.security.DigestInputStream
import java.security.MessageDigest

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

        project.task('generateKeys') {
            group = 'CAS'
            description = 'generate keys for CAS. These keys can be added to your application.properties file'
            doLast {
                println 'Generating keys for CAS'
                ['tgc.encryption.key': 256, 'tgc.signing.key': 512, 'webflow.encryption.key': 96, 'webflow.signing.key': 512].each { key, size ->
                    def octetKey = OctJwkGenerator.generateJwk(size)
                    def params = octetKey.toParams(JsonWebKey.OutputControlLevel.INCLUDE_SYMMETRIC)
                    println "${key}=${params.get('k')}"
                }
            }
        }

        def generateMD5 = { File f ->
            f.withInputStream {
                new DigestInputStream(it, MessageDigest.getInstance('MD5')).withStream {
                    it.eachByte {}
                    it.messageDigest.digest().encodeHex() as String
                }
            }
        }

        project.task('copyCasResources') {
            group = 'CAS'
            description = 'copy the resources from the CAS distribution'
            doLast {
                println "copying resources from CAS"
                def resourceRoot = project.file('src/main/resources')
                if (!resourceRoot) {
                    project.file('src/main/resources').mkdirs()
                }
                def resource = project.configurations.compile.find {
                    it.name.matches('cas-server-webapp-.*-resources\\.jar')
                }
                println "found: ${resource}"
                project.copy {
                    from project.zipTree(resource)
                    into temporaryDir
                }
                project.fileTree(temporaryDir).visit { el ->
                    if (!el.file.isDirectory()) {
                        println "checking ${el.relativePath} (${generateMD5 el.file})"
                        if (!project.file("${resourceRoot}/${el.relativePath}").exists() || (generateMD5(el.file) != generateMD5(project.file("${resourceRoot}/${el.relativePath}")))) {
                            project.copy {
                                from el.file
                                into project.file("${resourceRoot}/${el.relativePath}").parent
                                if (project.file("${resourceRoot}/${el.relativePath}").exists() && (generateMD5(el.file) != generateMD5(project.file("${resourceRoot}/${el.relativePath}")))) {
                                    rename {String fileName ->
                                        "${fileName}.casorig"
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        project.task('cleanCasResources') {
            group = 'CAS'
            description = 'remove default resources from tree'
            doLast {
                println 'cleaning up resources from CAS'
                project.copy {
                    from project.zipTree(project.configurations.compile.find { it.name.matches('cas-server-webapp-.*-resources\\.jar')})
                    into temporaryDir
                }
                //clean up casorig files
                project.fileTree('src/main/resources').matching {
                    include '**/*.casorig'
                }.each {
                    it.delete()
                }
                // clean up files that haven't changed
                project.fileTree('src/main/resources').visit { el ->
                    if (!el.file.isDirectory()) {
                        println "checking ${el.relativePath}"
                        def orig = project.file("${temporaryDir}/${el.relativePath}")
                        if (orig.exists() && generateMD5(orig) == generateMD5(el.file)) {
                            el.file.delete()
                        }
                    }
                }
            }
        }
    }
}
