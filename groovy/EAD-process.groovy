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
                             credentialsId   : 'ecacc38b-6ba5-40b8-a6a6-4d4bd2202d6a',
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

/*
def jsonBuilder(JSON_parts) {
	def string = "################################### test #####################################"
	def JSON_full = ""
	echo "JSON_parts: ${JSON_parts}"
	echo "JSON_full: ${JSON_full}"
	echo "Return: ${string}"
}
*/

node {
    
    // GLOBAL VARIABLES
    def NAME = "mock-microservice2"
    def BASIC_INFO = ""
    def BUILDPACKSTRING = ""
    def LINKS = ""
    def JIRALINK = ""
    def BUSINESS_INFO = ""
	
	//newly added
	def ORG_NAME = "ead-tool"
	def JSON_parts = new String[7]
    
    deleteDir()
    
    /*
    stage('Sources') {
        checkout([
                $class           : 'GitSCM',
                branches         : [[name: "refs/heads/master"]],
                extensions       : [[$class: 'CleanBeforeCheckout', localBranch: "master"]],
                userRemoteConfigs: [[                     
                    url          : "https://github.com/ludwigachhammer/Microservice2"
                                    ]]
                ])
    }
    */

    dir("C:/Program Files (x86)/Jenkins/workspace/simplebuild") {
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
        
        /*
        stage("Build"){
            bat "gradlew build"
        }
        */
        
        stage("Get Basic Jira Information"){
            //GET http://jira-url:port/rest/api/2/project/{projectIdOrKey}
            //def jiraProject = callGetJira("http://vmmatthes32.informatik.tu-muenchen.de:6000/rest/api/2/project/ED")
            //BASIC_INFO = "\"id\": \""+jiraProject.id+"\", \"key\":\""+jiraProject.key+"\", \"name\": \""+jiraProject.name+"\", \"owner\": \""+jiraProject.lead.name+"\", \"description\": \""+jiraProject.description+"\", \"short_name\": \""+jiraProject.key+"\", \"type\": \""+jiraProject.projectTypeKey+"\","
            BASIC_INFO = "\"id\": \""+"1234"+"\", \"key\":\""+"a1b2"+"\", \"name\": \""+"mock-microservice2"+"\", \"owner\": \""+"leader"+"\", \"description\": \""+"testdescription"+"\", \"short_name\": \""+"abc"+"\", \"type\": \""+"microservice"+"\","
            echo "BASIC INFO: ${BASIC_INFO}"
        }
        stage("Get Business Jira Information"){
            // customfield_10007: Domain
            // customfield_10008: Subdomain
            // customfield_10009: Product
            // changed due to Jira structure: Components
            //def response = callGetJira("http://vmmatthes32.informatik.tu-muenchen.de:6000/rest/api/2/search?jql=project=ED")
            //echo "ISSUES: ${response}"
            //List<String> domains = new ArrayList<String>()
            //List<String> subdomains = new ArrayList<String>()
            //List<String> products = new ArrayList<String>()
            /*for (i = 0; i <response.issues.size(); i++) {
                domain_tmp = 'IT'//response.issues[i].fields.customfield_10007.value
                subdomain_tmp = 'IT-2'//response.issues[i].fields.customfield_10008.value
                product_tmp = 'EA Documentation' //response.issues[i].fields.customfield_10009
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
            */
            //BUSINESS_INFO = " \"domain\": \"${domains[0]}\", \"subdomain\": \"${subdomains[0]}\", \"product\": \"${products[0]}\" " 
            BUSINESS_INFO = " \"domain\": \"drumset\", \"subdomain\": \"cymbals\", \"product\": \"crashride\" " 
        }
        
        /*
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
        */
        
        
        stage("Get Runtime Information"){
            APP_STATUS = bat (
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
            CF_NETWORK_POLICIES_SOURCE = bat (
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
        
        stage("Get CF-Contact information") {
		CF_CONTACT = bat (
			script: 'cf org-users '+ORG_NAME,
			returnStdout: true
		)
		ORG_MANAGER = CF_CONTACT.substring((CF_CONTACT.indexOf("ORG MANAGER", 0)+14), (CF_CONTACT.indexOf("BILLING MANAGER", -1))).trim()
		echo "ORG_MANAGER: ${ORG_MANAGER}"
		echo "CF_CONTACT: ${CF_CONTACT}"
						   
		BILLING_MANAGER = CF_CONTACT.substring((CF_CONTACT.indexOf("BILLING MANAGER", 0)+18), (CF_CONTACT.indexOf("ORG AUDITOR", -1))).trim()
		echo "BILLING_MANAGER: ${BILLING_MANAGER}"
						   
		ORG_AUDITOR = CF_CONTACT.substring((CF_CONTACT.indexOf("ORG AUDITOR", 0)+14), (CF_CONTACT.length())).trim()
		echo "ORG_AUDITOR: ${ORG_AUDITOR}"
		
		ALLCONTACTS = "\"orgmanager\":\"${ORG_MANAGER}\",\"billingmanager\":\"${BILLING_MANAGER}\",\"auditmanager\":\"${ORG_AUDITOR}\""
		echo "CONTACT: ${ALLCONTACTS}"
		}
        
        stage("Push Documentation"){
            def runtime = " \"status\":\"${APP_SHORTSTATUS[1]}\", \"runtime\": {\"ram\": \"${APP_SHORTSTATUS[4]}\", \"cpu\": \"${APP_SHORTSTATUS[3]}\", \"disk\": \"${APP_SHORTSTATUS[5]}\", \"host_type\": \"cloudfoundry\" }"
            echo "LINKS: ${LINKS}"
            def jsonstring = "{"+BASIC_INFO+BUSINESS_INFO+","+runtime+","+LINKS+","+APP_SERVICES+","+ALLCONTACTS+"}"
            echo "JSONSTRING: ${jsonstring}"
            try {
                    //callPost("http://192.168.99.100:9123/document", jsonstring) //Include protocol
                    callPost("http://localhost:8080/document/", jsonstring) //Include protocol
                } catch(e) {
                    // if no try and catch: jenkins prints an error "no content-type" but post request succeeds
                }
        }//stage
       
    }

}
