# dubbo-dk
<pre>
pipeline {
    agent any
    environment {
       def deploy_name='dk'
       def password='密码'
       def target_port=20880
   }
    parameters {
        text(name: 'giturl', defaultValue: 'https://github.com/297513458/dubbo-dk.git', description: 'git地址')
        text(name: 'build_path', defaultValue: 'dubbo-demo-api-provider', description: '构建目录')
    }
    stages{
      stage('init') {  
          steps {
            echo 'init'
             sh "echo `date +%Y%m%d%H-`$env.BUILD_ID > build.v"
          }
       }
       stage('拉取git'  ) {
           steps {
                git "$giturl"
           }
       }
       stage('构建') { 
           steps {
                echo 'mvn clean package'
                sh "mvn clean package -Dmaven.test.skip=true"
           }
       }
        stage('构建docker') {  
            steps {
                echo 'build docker'
                 sh '''
                    v=`cat build.v`
                    cd $build_path
                    docker build -t 297513458/${deploy_name}:$v .
                  '''}
       }
       stage('发布到私服 ') {
           steps { 
               echo 'deploy docker'
                sh '''
                 v=`cat build.v`
                 docker login -u 297513458 -p $password
                 docker push 297513458/${deploy_name}:$v
                 '''
           }
       }
        stage('触发k8s') {
           steps { 
               echo 'deploy k8s'
                sh '''
                   v=`cat build.v`
                   count=`kubectl get deploy ${deploy_name} --namespace=app|wc -l`
                   if [ $count == 2 ]
                   then
                        echo "exec update"
                        kubectl set image deployments/$deploy_name $deploy_name=297513458/${deploy_name}:$v --namespace=app
                    else
                        echo " exec deploy"
                    kubectl run ${deploy_name} --image=297513458/${deploy_name}:$v --replicas=3 --namespace=app
                    echo "开放服务k8s"
                    kubectl expose deployment/${deploy_name} --port=${target_port}  --target-port=${target_port} --type=LoadBalancer --namespace=app
                fi
             '''
           }
        }
   }
}
</pre>
