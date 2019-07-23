//@Library('open-ead-library') _
@Library('ead-jenkins-library@extractReadme') _

node {
    
    // GLOBAL VARIABLES
    def NAME = "mock-microservice2"
    def BASIC_INFO = ""
    def BUILDPACKSTRING = ""
    def LINKS = ""
    def JIRALINK = ""
    def BUSINESS_INFO = ""
	
	environment {
        ORG = "ead-tool"
        SPACE = "development"
        CF_CREDEDNTIALS_ID = "05487704-f456-43cb-96c3-72aaffdba62f"
}
    
    deleteDir()

    stage('Get source code') {
        checkout([
                $class           : 'GitSCM',
                branches         : [[name: "refs/heads/master"]],
                extensions       : [[$class: 'CleanBeforeCheckout', localBranch: "master"]],
                userRemoteConfigs: [[                     
                    url          : "https://github.com/ludwigachhammer/Microservice2"
                                    ]]
                ])
	    

    }

    /*
    dir("") {
      
        stage("Build"){
            bat "gradlew build"
        }

        
        stage('Deploy') {
            def branch = ['master']
            def path = "build/libs/gs-spring-boot-0.1.0.jar"
            def manifest = "manifest.yml"
            echo '\"'+'$CF_PASSWORD'+'\"'
            
               if (manifest == null) {
                throw new RuntimeException('Could not map branch ' + master + ' to a manifest file')
               }
               withCredentials([[
                                     $class          : 'UsernamePasswordMultiBinding',
                                     credentialsId   : '05487704-f456-43cb-96c3-72aaffdba62f',
                                     usernameVariable: 'CF_USERNAME',
                                     passwordVariable: 'CF_PASSWORD'
                             ]]) {
                bat "cf login -a https://api.run.pivotal.io -u $CF_USERNAME -p \"$CF_PASSWORD\" --skip-ssl-validation"
                bat 'cf target -o ead-tool -s development'
                bat 'cf push '+NAME+' -f '+manifest+' --hostname '+NAME+' -p '+path
            }
	    
	    stage('start EAD-process') {
 		   //build 'EAD-process.groovy'
		   workdir = bat (
			script: 'cd',
			returnStdout: true
			)
		   echo "Workdir: ${workdir}" 
		   workdir = workdir.substring((workdir.indexOf("cd", 0)+3), (workdir.length())).replaceAll("\\\\", "/").trim()
		   basedir = workdir.substring(0, (workdir.indexOf('workspace', 0)+9)).replaceAll("\\\\", "/").trim()
		   echo "Workdir: ${workdir}"
		echo "basedir: ${basedir}"
		//build "${basedir}/EAD-process", parameters: [string(name: 'workdir', value: workdir)]
		    build job: 'EAD-process', parameters: [[$class: 'StringParameterValue', name: 'WORKDIR', value: "${workdir}" ]]
		   // build(job: 'EAD-process', 'WORKDIR' : "${workdir}")
	    }*/


            //def file = readFile "${WORKSPACE}/links.config"
            //def manifest = readFile "${WORKSPACE}/manifest.yml"

            //TODO should be extracted either from manifest or from pom.xml
            //def manifest = readManifest file: 'target/mockmicroservice-0.1.0.jar'
            //echo "manifest: ${manifest}"

            //def eadjson = readJSON file: 'ead.json'
            //echo "eadjson: ${eadjson}"

            try{
                eadprocess.ead(workDir: "${WORKSPACE}") {}
            } catch (NoSuchMethodError error) {
                println (error)
            }


    /*
		stage('start library EAD-process') {
 		  steps {
                script {
			        eadtest 'test'
                    }
                }
	    }
	    */


	}
       
