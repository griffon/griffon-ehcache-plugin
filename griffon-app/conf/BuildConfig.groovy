griffon.project.dependency.resolution = {
    inherits "global"
    log "warn"
    repositories {
        griffonHome()
        mavenCentral()
        mavenLocal()
    }
    dependencies {
        String ehcacheVersion = '2.6.5'
        compile("net.sf.ehcache:ehcache-core:$ehcacheVersion") {
            exclude group: 'org.slf4j'
            excludes 'servlet-api'
        }
        build('org.eclipse.jdt:org.eclipse.jdt.core:3.6.0.v_A58') {
            export = false
        }
        String lombokIdea = '0.5'
        build("de.plushnikov.lombok-intellij-plugin:processor-api:$lombokIdea",
              "de.plushnikov.lombok-intellij-plugin:processor-core:$lombokIdea",
              "de.plushnikov.lombok-intellij-plugin:intellij-facade-factory:$lombokIdea",
              "de.plushnikov.lombok-intellij-plugin:intellij-facade-api:$lombokIdea",
              "de.plushnikov.lombok-intellij-plugin:intellij-facade-9:$lombokIdea",
              "de.plushnikov.lombok-intellij-plugin:intellij-facade-10:$lombokIdea",
              "de.plushnikov.lombok-intellij-plugin:intellij-facade-11:$lombokIdea") {
            export = false
            transitive = false
        }
        String ideaVersion = '11.1.4'
        build("org.jetbrains.idea:idea-openapi:$ideaVersion",
              "org.jetbrains.idea:extensions:$ideaVersion",
              "org.jetbrains.idea:util:$ideaVersion",
              "org.jetbrains.idea:annotations:$ideaVersion") {
            export = false
        }
    }
}

log4j = {
    // Example of changing the log pattern for the default console
    // appender:
    appenders {
        console name: 'stdout', layout: pattern(conversionPattern: '%d [%t] %-5p %c - %m%n')
    }

    error 'org.codehaus.griffon',
          'org.springframework',
          'org.apache.karaf',
          'groovyx.net'
    warn  'griffon'
}