def callPost(String urlString, String queryString) {
    def url = new URL(urlString)
    def connection = url.openConnection()
    connection.setRequestMethod("POST")
    connection.doInput = true
    connection.doOutput = true
    connection.setRequestProperty("content-type", "application/json;charset=UTF-8")

    def writer = new OutputStreamWriter(connection.outputStream)
    writer.write(queryString.toString())
    writer.flush()
    writer.close()
    connection.connect()

    new groovy.json.JsonSlurper().parseText(connection.content.text)
}

def callGetJira(String urlString) {
    withCredentials([[
                             $class          : 'UsernamePasswordMultiBinding',
                             credentialsId   : '65220318-765b-4be8-8244-e7ed8f84ecd7',
                             usernameVariable: 'JIRA_USERNAME',
                             passwordVariable: 'JIRA_PASSWORD'
                     ]]) {
        def url = new URL(urlString)
        def connection = url.openConnection()
        connection.setRequestMethod("GET")
        def encoded = ""
        encoded = (JIRA_USERNAME+":"+JIRA_PASSWORD).bytes.encodeBase64().toString()
        def basicauth = "Basic ${encoded}"
        connection.setRequestProperty("Authorization", basicauth)
        connection.connect()

        new groovy.json.JsonSlurper().parseText(connection.content.text)
    }
}

node {
    
    // GLOBAL VARIABLES
    def NAME = "Masterarbeitssoftware-frontend"
    def BASIC_INFO = ""
    def BUILDPACKSTRING = ""
    def LINKS = ""
    def JIRALINK = ""
    def BUSINESS_INFO = ""
    
    deleteDir()

    stage('Sources') {
        checkout([
                $class           : 'GitSCM',
                branches         : [[name: "refs/heads/master"]],
                extensions       : [[$class: 'CleanBeforeCheckout', localBranch: "master"]],
                userRemoteConfigs: [[
                                            credentialsId: 'cbf178fa-56ee-4394-b782-36eb8932ac64',
                                            url          : "https://github.com/Nicocovi/Microservice2"
                                    ]]
                ])
    }

    dir("") {
        stage("Build"){
            sh "mvn clean package"
        }
        
        stage("Validating Config"){
            //TODO
            //Validate jira link in links.config
            def currentDir = new File(".").absolutePath
            env.WORKSPACE = pwd() // present working directory.
            def file = readFile "${env.WORKSPACE}/links.config"
            def trimmedText = file.trim().replaceAll('\t',' ').replaceAll('\r\n',' ').replaceAll(" +",";").split(";")
            echo "trimmedText: ${trimmedText}"
            int index = -1;
            for (int i=0;i<trimmedText.length;i++) {
                if (trimmedText[i].contains("jira")) {
                    index = i+1;
                    break;
                }
            }
            JIRALINK = trimmedText[index]
            String regex = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]" //website regex
            //TODO 
            //JIRALINK matches regex
            for (i = 1; i <trimmedText.size(); i = i+2) {
                LINKS = LINKS+"\""+trimmedText[i]+"\":"+"\""+trimmedText[i+1]+"\","
            }
            LINKS = LINKS.substring(0, (LINKS.length())-1)//remove last coma
            echo LINKS
        }
        
        stage("Get Basic Jira Information"){
            //GET http://localhost:8099/rest/api/2/project/{projectIdOrKey}
            def jiraProject = callGetJira("http://localhost:8099/rest/api/2/project/MAS")
            BASIC_INFO = "\"id\": \""+jiraProject.id+"\", \"key\":\""+jiraProject.key+"\", \"name\": \""+jiraProject.name+"\", \"owner\": \""+jiraProject.lead.name+"\", \"description\": \""+jiraProject.description+"\", \"short_name\": \""+jiraProject.key+"\", \"type\": \""+jiraProject.projectTypeKey+"\","
            echo "BASIC INFO: ${BASIC_INFO}"
        }
        stage("Get Business Jira Information"){
            // customfield_10007: Domain
            // customfield_10008: Subdomain
            // customfield_10009: Product
            def response = callGetJira("http://localhost:8099/rest/api/2/search?jql=project=MAS")
            //echo "ISSUES: ${response}"
            List<String> domains = new ArrayList<String>()
            List<String> subdomains = new ArrayList<String>()
            List<String> products = new ArrayList<String>()
            for (i = 0; i <response.issues.size(); i++) {
                domain_tmp = response.issues[i].fields.customfield_10007.value
                subdomain_tmp = response.issues[i].fields.customfield_10008.value
                product_tmp = response.issues[i].fields.customfield_10009
                if(!domains.contains(domain_tmp)){
                    domains.add(domain_tmp)
                }
                if(!subdomains.contains(subdomain_tmp)){
                    subdomains.add(subdomain_tmp)
                }
                if(!products.contains(product_tmp)){
                    products.add(product_tmp)
                }
            }
            echo "DOMAIN: ${domains}"
            echo "DOMAIN: ${subdomains}"
            echo "DOMAIN: ${products}"
            BUSINESS_INFO = " \"domain\": \"${domains[0]}\", \"subdomain\": \"${subdomains[0]}\", \"product\": \"${products[0]}\" "       
        }
        
        stage('Deploy') {
            def branch = ['master']
            def path = "build/libs/gs-spring-boot-0.1.0.jar"
            def manifest = "manifest.yml"
            
               if (manifest == null) {
                throw new RuntimeException('Could not map branch ' + master + ' to a manifest file')
               }
               withCredentials([[
                                     $class          : 'UsernamePasswordMultiBinding',
                                     credentialsId   : '98c5d653-dbdc-4b52-81ba-50c2ac04e4f1',
                                     usernameVariable: 'CF_USERNAME',
                                     passwordVariable: 'CF_PASSWORD'
                             ]]) {
                sh 'cf login -a https://api.run.pivotal.io -u $CF_USERNAME -p $CF_PASSWORD --skip-ssl-validation'
                sh 'cf target -o ncorpan-org -s development'
                sh 'cf push '+NAME+' -f '+manifest+' --hostname '+NAME+' -p '+path
            }
        }
        
        
        stage("Get Runtime Information"){
            APP_STATUS = sh (
                script: 'cf app '+NAME,
                returnStdout: true
            )
            LENGTH = APP_STATUS.length()
            INDEX = APP_STATUS.indexOf("#0", 0)
            APP_SHORTSTATUS = (APP_STATUS.substring(INDEX,LENGTH-1)).replaceAll("\n","  ").replaceAll("   ",";").split(";")
            echo "SHORTSTATUS: ${APP_SHORTSTATUS}"
            
            APP_BUILDPACKS_INDEX = APP_STATUS.indexOf("buildpacks", 0)
            APP_TYPE_INDEX = APP_STATUS.indexOf("type", 0)
            APP_BUILDPACKS = (APP_STATUS.substring(APP_BUILDPACKS_INDEX+11,APP_TYPE_INDEX-1)).trim().replaceAll("\n","").replaceAll(" ",";").split(";") //trim for \n
            //+11 length of 'buildpacks'
            echo "APP_BUILDPACKS: ${APP_BUILDPACKS}"
            //include buildpacks
            def iterations = APP_BUILDPACKS.size()
            def buildpacks = "  \"service\": { \"buildpacks\":["
            for (i = 0; i <iterations; i++) {
                if(i==2){
                    //buildpack contains uncodedable chars (arrows)
                }else{
                    buildpacks = buildpacks+"\""+APP_BUILDPACKS[i]+"\","
                }
            }
            buildpacks = buildpacks.substring(0, (buildpacks.length())-1) //remove last coma
            BUILDPACKSTRING = buildpacks+"]"
            echo "buildpackstring: ${BUILDPACKSTRING}"
            //TODO network policies
            CF_NETWORK_POLICIES_SOURCE = sh (
                script: 'cf network-policies --source '+NAME,
                returnStdout: true
            )
            CF_NETWORK_POLICIES = CF_NETWORK_POLICIES_SOURCE.substring((CF_NETWORK_POLICIES_SOURCE.indexOf("ports", 0)+5), (CF_NETWORK_POLICIES_SOURCE.length())-1)
            CF_NETWORK_POLICIES = CF_NETWORK_POLICIES.trim().replaceAll('\t',' ').replaceAll('\n',' ').replaceAll('\r\n',' ')replaceAll(" +",";").split(";")
            echo "CF_NETWORK_POLICIES: ${CF_NETWORK_POLICIES}"
            APP_SERVICES = ",\"provides\": ["
            for (int i=0;i<(CF_NETWORK_POLICIES.size() / 4);i++) {
                APP_SERVICES = APP_SERVICES + "{\"service_name\": \""+CF_NETWORK_POLICIES[1+i*4]+"\"},"
            }
            APP_SERVICES = APP_SERVICES.substring(0, (APP_SERVICES.length())-1) //remove last coma
            APP_SERVICES = BUILDPACKSTRING + APP_SERVICES + "]}"
            echo "APP_SERVICES: ${APP_SERVICES}"            
        }//stage
        
        stage("Push Documentation"){
            def runtime = " \"runtime\": {\"ram\": \"${APP_SHORTSTATUS[4]}\", \"cpu\": \"${APP_SHORTSTATUS[3]}\", \"disk\": \"${APP_SHORTSTATUS[5]}\", \"host_type\": \"cloudfoundry\" }"
            echo "LINKS: ${LINKS}"
            def jsonstring = "{"+BASIC_INFO+BUSINESS_INFO+","+runtime+","+LINKS+","+APP_SERVICES+"}"
            echo "JSONSTRING: ${jsonstring}"
            try {
                    callPost("http://192.168.99.100:9123/document", jsonstring) //Include protocol
                } catch(e) {
                    // if no try and catch: jenkins prints an error "no content-type" but post request succeeds
                }
        }//stage
       
    }

}
