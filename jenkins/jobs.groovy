def projects = [
        'allure-framework/allure-core',
        'allure-framework/allure-report-builder',
        'allure-framework/allure-maven-plugin',
        'allure-framework/allure-teamcity-plugin',
        'allure-framework/allure-cli',
        'allure-framework/allure-cucumber-jvm-adaptor',
        'allure-examples/allure-examples-parent',
        'camelot-framework/clay',
        'camelot-framework/yatomata',
        'camelot-framework/yatomata-camel',
        'camelot-framework/camelot',
        'camelot-framework/camelot-utils',
        'yandex-qatools/ashot',
        'yandex-qatools/beanloader',
        'yandex-qatools/htmlelements',
        'yandex-qatools/embedded-services',
        'yandex-qatools/postgresql-embedded',
        'qatools/properties'
]

projects.each {
    println 'create job for ' + it

    def projectUrl = it
    def projectName = (projectUrl =~ /.*[\/](.*)/)[0][1]

    println 'project name ' + projectName

    mavenJob(projectName + '_master-deploy') {

        if (projectName.equals('properties'))  {
            label('maven')
        } else {
            label('maven-old')
        }
        previousNames(projectName + '_deploy')
        scm {
            git {
                remote {
                    github projectUrl
                }
                branch('master')
                localBranch('master')
            }
        }
        triggers {
            githubPush()
        }

        goals 'org.jacoco:jacoco-maven-plugin:0.7.4.201502262128:prepare-agent clean deploy'

        wrappers {
            mavenRelease {
                releaseGoals('release:clean release:prepare release:perform')
                dryRunGoals('-DdryRun=true release:prepare')
                numberOfReleaseBuildsToKeep(10)
            }
        }

        configure { project ->
            project / publishers << 'hudson.plugins.sonar.SonarPublisher' {
                jdk('(Inherit From Job)')
                branch()
                language()
                mavenOpts("-Xmx1024m -Xms256m")
                jobAdditionalProperties()
                settings(class: 'jenkins.mvn.DefaultSettingsProvider')
                globalSettings(class: 'jenkins.mvn.DefaultGlobalSettingsProvider')
                usePrivateRepository(false)
            }
        }

        publishers {
            if (projectName.equals('allure-core'))  {
                archiveArtifacts {
                    pattern('allure-report-preview/target/allure-report/')
                }
            }
        }

    }

    mavenJob(projectName + '_pull-request') {
        label('maven-old')
        scm {
            git {
                remote {
                    github projectUrl
                    refspec('+refs/pull/*:refs/remotes/origin/pr/*')
                }
                branch('${sha1}')
            }
        }


        triggers {
            pullRequest {
                orgWhitelist(['allure-framework', 'qatools', 'yandex-qatools', 'allure-examples'])
                permitAll()
                useGitHubHooks()
            }
        }

        goals 'clean verify'

        publishers {
            if (projectName.equals('allure-core'))  {
                archiveArtifacts {
                    pattern('allure-report-preview/target/allure-report/')
                }
            }
        }

    }
}