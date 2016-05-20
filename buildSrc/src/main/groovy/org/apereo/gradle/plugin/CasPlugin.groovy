package org.apereo.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jose4j.jwk.JsonWebKey
import org.jose4j.jwk.OctJwkGenerator
import org.springframework.boot.gradle.SpringBootPlugin

import java.security.DigestInputStream
import java.security.MessageDigest

/**
 * CAS Gradle plugin implementing user-friendly DSL for building and deploying Apereo CAS server.
 *
 * @author Jonathan Johnson
 * @since 1.0.0
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
            println "CAS features enabled: ${project.cas.features}"
            project.dependencies {
                //This dependency contains main Boot CasWebApplication
                compile("org.apereo.cas:cas-server-webapp-init:${project.cas.version}")

                compile("org.apereo.cas:cas-server-webapp:${project.cas.version}:resources") {
                    transitive = true
                }
                project.cas.features.each {
                    compile("org.apereo.cas:cas-server-support-${it}:${project.cas.version}")
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

        def generateFileSignature = { File f ->
            f.withInputStream {
                new DigestInputStream(it, MessageDigest.getInstance('MD5')).withStream {
                    it.eachByte {}
                    it.messageDigest.digest().encodeHex() as String
                }
            }
        }

        def getCasResources = { File destination ->
            def resource = project.configurations.compile.find {
                it.name.matches('cas-server-webapp-.*-resources\\.jar')
            }
            project.copy {
                from project.zipTree(resource)
                into destination
            }
        }

        project.task('copyCasResources') {
            group = 'CAS'
            description = 'copy the resources from the CAS distribution'
            doLast {
                println "copying resources from CAS"
                def resourceRoot = project.file('src/main/resources')
                getCasResources temporaryDir
                project.fileTree(temporaryDir).visit { el ->
                    if (!el.file.isDirectory()) {
                        println "checking ${el.relativePath} (${generateFileSignature el.file})"
                        if (!project.file("${resourceRoot}/${el.relativePath}").exists() || (generateFileSignature(el.file) != generateFileSignature(project.file("${resourceRoot}/${el.relativePath}")))) {
                            project.copy {
                                from el.file
                                into project.file("${resourceRoot}/${el.relativePath}").parent
                                if (project.file("${resourceRoot}/${el.relativePath}").exists() && (generateFileSignature(el.file) != generateFileSignature(project.file("${resourceRoot}/${el.relativePath}")))) {
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
                getCasResources temporaryDir
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
                        if (orig.exists() && generateFileSignature(orig) == generateFileSignature(el.file)) {
                            el.file.delete()
                        }
                    }
                }
                // TODO: clean up empty directories
            }
        }
    }
}
