# 
<pre>
pipeline {
    agent any
    environment {
        //发布的名称 
       def deploy_name='dk'
       //hub或私服的用户名 
       def hub_name ='297513458'
       //hub或私服的密码
       def hub_password='密码'
       //服务开放的端口
       def target_port=20880
       //k8s的namespace
       def namespace='app'
   }
    parameters {
        string(name: 'giturl', defaultValue: 'https://github.com/297513458/dubbo-dk.git', description: 'git地址')
        string(name: 'project_module_path', defaultValue: 'dubbo-demo-api-provider', description: '构建目录')
    }
    stages{
      stage('初始化') {  
          steps {
            echo 'init'
             sh "echo `date +%Y%m%d%H-`$env.BUILD_ID > build.v"
          }
       }
       stage('从git拉取'  ) {
           steps {
                git "$giturl"
           }
       }
       stage('maven构建') { 
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
                    cd $project_module_path
                    echo $project_module_path
                    pwd
                    docker build -t $hub_name/${deploy_name}:$v .
                  '''}
       }
       stage('发布到私服 ') {
           steps { 
               echo 'deploy docker'
                sh '''
                 v=`cat build.v`
                 docker login -u $hub_name -p $hub_password
                 docker push $hub_name/${deploy_name}:$v
                 '''
           }
       }
        stage('触发k8s') {
           steps { 
               echo 'deploy k8s'
                sh '''
                   v=`cat build.v`
                   count=`kubectl get deploy ${deploy_name} --namespace=$namespace|wc -l`
                   if [ $count == 2 ]
                   then
                        echo "exec update"
                        kubectl set image deployments/$deploy_name $deploy_name=$√/${deploy_name}:$v --namespace=$namespace
                    else
                        echo " exec deploy"
                    kubectl run ${deploy_name} --image=$hub_name/${deploy_name}:$v --replicas=3 --namespace=$namespace
                    echo "开放服务k8s"
                    kubectl expose deployment/${deploy_name} --port=${target_port}  --target-port=${target_port} --type=LoadBalancer --namespace=$namespace
                fi
             '''
           }
        }
   }
}
</pre>
