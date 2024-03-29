import java.text.SimpleDateFormat
import groovy.json.JsonSlurper

pipeline{

agent none
  environment {

    def mybuildverison = getBuildVersion(env.BUILD_NUMBER)
    def projektname = env.JOB_NAME.replace("/master","").replace("projectyouthclubstage/","")
    def registry = "192.168.233.1:5000/${projektname}"
    def dns = "${projektname}.youthclubstage.de"
    def dnsblue = "${projektname}-blue.youthclubstage.de"
    def port = "8080"

  }


stages{

    // Run Maven build, skipping tests
    stage('Build'){
    agent {
        docker {
            image 'arm64v8/maven'
        }
    }
     steps {
        sh "mvn -v"
        sh "java -version"
        sh "mvn -B clean install -DskipTests=true"
        }
    }

    // Run Maven unit tests
    stage('Unit Test'){
    agent {
       docker {
            image 'arm64v8/maven'
          }
    }
     steps {
        sh "mvn -B test"
        }
    }

       stage('docker build')
       {
          agent {
               label 'master'
           }
           steps{
            script{
                if (env.BRANCH_NAME == 'master') {
               dockerImage = docker.build registry + ":$mybuildverison"
               dockerImage.push()
               }
              }
           }
       }

       stage('Docker deploy'){
                 agent {
                      label 'master'
                  }
                  steps{



                   script{
                      if (env.BRANCH_NAME == 'master') {
                        dockerDeploy(mybuildverison,registry,projektname,dns,dnsblue,port)
                      }

                     }
                   }
       }
   }
     post {
       failure {
         agent {
             label 'master'
         }
         steps{
         script{
           sh "docker stack rm $projektname-$mybuildverison"
         }
         }
       }
     }
}

def getBuildVersion(String buildnr){
    def dateFormat = new SimpleDateFormat("yyyyMMddHHmm")
    def date = new Date()
    return dateFormat.format(date)+buildnr
}

def dockerDeploy(String mybuildverison,String registry, String projektname, String dns, String dnsblue, String port){
                      sh "mkdir -p target"
                      sh "rm -f target/*.yml"
                      def regescape = registry.replace("/","\\/")
                      sh "cat docker-compose-template.yml | sed -e 's/{image}/$regescape:$mybuildverison/g;s/{alias}/$projektname-$mybuildverison/g' >> target/docker-compose.yml"
                      def version = sh (
                          script: 'docker stack ls |grep '+projektname+'| cut -d \" \" -f1',
                          returnStdout: true
                      ).trim()
                      //sh "docker stack rm "+version
                      sh "docker stack deploy --compose-file target/docker-compose.yml "+projektname+"-"+"$mybuildverison"

                      sleep 240 // second
                      def id = registerUrl(dnsblue,projektname+"-$mybuildverison"+":$port")
                      //Health blue
                      health("https://$dnsblue/actuator/health")
                      deleleUrl(id)

                      //Green
                      registerUrl(dns,projektname+"-$mybuildverison"+":$port")
                      health("https://$dns/actuator/health")

                      if(version != "")
                      {
                        sh "docker stack rm "+version
                      }
}

def registerUrl(url, target){
  def context = """
    {"source": "$url",
    "target": "$target"}
  """
   
   retry (3) {
    sleep 5
    def response = httpRequest acceptType: 'APPLICATION_JSON_UTF8', contentType: 'APPLICATION_JSON_UTF8', httpMode: 'POST', requestBody: context, url: "http://192.168.233.1:9099/v1/dns", validResponseCodes: '200'
       def json = new JsonSlurper().parseText(response.content)
       echo "Status: ${response.status}"
       echo "ID: ${json}"
       return json.id
   }

}

def health(url){
  retry (3) {
    sleep 5
    httpRequest url:url, validResponseCodes: '200'
  }  

}

def deleleUrl(id){
   retry (3) {
    sleep 5
    httpRequest httpMode: 'DELETE', url: "http://192.168.233.1:9099/v1/dns/${id}", validResponseCodes: '204'
   }
}