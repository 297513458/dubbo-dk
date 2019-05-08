# dubbo-dk
<pre>
node {
  //设置版本
   sh "echo `date +%Y%m%d%H-`$env.BUILD_ID > build.v"
   stage('拉取git'  ) {
     git "$giturl"
   }
   stage('构建') {
      echo 'mvn clean package'
      sh "mvn clean package -Dmaven.test.skip=true"
   }
    stage('构建docker') {
      echo 'build docker'
      sh '''
        v=`cat build.v`
        cd dubbo-demo-api-provider
        docker build -t 297513458/${env.JOB_NAME}:$v .
      '''
   }
   stage('发布到私服 ') {
      echo 'deploy docker'
      sh '''
        v=`cat dk.v`
        docker login -u 297513458 -p $password
        docker push 297513458/${env.JOB_NAME}:$v
      '''
   }
    stage('触发k8s') {
      echo 'deploy k8s'
      sh '''
       v=`cat build.v`
       count=`kubectl get deploy ${env.JOB_NAME} --namespace=app|wc -l`
       if [ $count == 2 ]
       then
            echo "exec update"
            kubectl set image deployments/${env.JOB_NAME} $env.JOB_NAME=297513458/${env.JOB_NAME}:$v --namespace=app
        else
            echo " exec deploy"
            kubectl run ${env.JOB_NAME} --image=297513458/${env.JOB_NAME}:$v --replicas=3 --namespace=app
            echo "开放服务k8s"
            kubectl expose deployment/${env.JOB_NAME} --port=20880 --target-port=20880 --type=LoadBalancer --namespace=app
        fi
        '''
   }
}
</pre>
