node {
    
    // GLOBAL VARIABLES
    def NAME = "mock-microservice2"
    def BASIC_INFO = ""
    def BUILDPACKSTRING = ""
    def LINKS = ""
    def JIRALINK = ""
    def BUSINESS_INFO = ""
    
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

    dir("") {
        stage("Validating Config"){
            //TODO
            //Validate jira link in links.config
            def currentDir = new File(".").absolutePath
            echo "Debugg: ${currentDir}"
            env.WORKSPACE = pwd() // present working directory.
            def file = readFile "${env.WORKSPACE}/links.config"
		
            def trimmedText = file.trim().replaceAll("\\r\\n|\\r|\\n", " ").replaceAll(" +",";").split(";")
            echo "trimmedText: ${trimmedText}"
            int index = -1;
            for (int i=0;i<trimmedText.length;i++) {
                if (trimmedText[i].contains("jira")) {
                    index = i+1;
                    break;
                }
            }
            
            JIRALINK = trimmedText[index]
            echo "JIRALINK: ${JIRALINK}"
            String regex = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]" //website regex
            //TODO 
            //JIRALINK matches regex
            for (i = 0; i <trimmedText.size()-1; i = i+2) {
                echo "${trimmedText[i]} : ${trimmedText[i+1]}"
                LINKS = LINKS+"\""+trimmedText[i]+"\":"+"\""+trimmedText[i+1]+"\","
            }
            LINKS = LINKS.substring(0, (LINKS.length())-1)//remove last coma
            echo LINKS
        }
      
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
        }
        
        

        

        
       
    }

}
