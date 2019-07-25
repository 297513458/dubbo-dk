# 从git到k8s部署的pipeline配置信息
<pre>
pipeline {
    agent any
    parameters {
        gitParameter(branch: '', branchFilter: '.*', defaultValue: 'origin/master', description: '选择分支构建', name: 'branch', quickFilterEnabled: true, selectedValue: 'NONE', sortMode: 'NONE', tagFilter: '*', type: 'PT_BRANCH')
    }
    environment {
       def giturl='git地址'
        def project_module_path='collection-starter'
       //发布的名称 
       def deploy_name='collection-service'
       //hub或私服的用户名 
       def name ='songxiaocai'
       //k8s的namespace
       def namespace='sxc'
       //
       def buildbranch='dk_k8s'
       //hub地址
       def hub_host='registry-vpc.cn-hangzhou.aliyuncs.com'
       //
       def hub_name="$hub_host/$name"
       //git访问token
        def credentialsId='具体token'
       //dk私服密码 
       def credentialsId_hub='私服'
       def config_path='/opt/server-config'
       def replicas=1
       def   DEPLOY_NEW=true
   }
    stages{
       stage('初始化'){
            steps {
                script{
                    try{
                        if(DEPLOY_ROLLBACK!=null&&DEPLOY_ROLLBACK!="全新发布"){
                            echo "回滚"
                            DEPLOY_NEW=false
                            sh '''
                            kubectl set image deployments/$deploy_name $deploy_name=$DEPLOY_ROLLBACK --namespace=$namespace
                            '''
                        }
                    } catch (Exception e){
                    }
                    if(DEPLOY_NEW){
                        try{
                            if(branch!=buildbranch&&branch!="")
                                buildbranch=branch
                        } catch (Exception e){
                        } 
                        try{
                            if(branch!=buildbranch&&branch!="")
                                buildbranch=branch
                        } catch (Exception e){
                        } 
                        try{
                            buildbranch=buildbranch.minus("origin/")
                        } catch (Exception e){
                        }
                        echo "构建$buildbranch"
                    }
                }
            }
        }
      stage('从git拉取'  ) {
            steps {
            git credentialsId:"$credentialsId",branch: "$buildbranch", url: "$giturl"
       }
      }
      stage('替换配置'  ) {
           when {
                expression {  return  DEPLOY_NEW}
            }
            steps {
                sh '''
                    echo '替换配置'
                    cs="$config_path/$deploy_name"
                    if [ -d $cs ]; then
                        cd $project_module_path
                        cd "src/main/resources"
                        rm -f application*.yml
                        \\cp -r $cs/* .
                    fi
                  '''
            }
        }
        stage('maven构建') { 
            when {
                expression {  return  DEPLOY_NEW}
            }
            steps {
                echo 'mvn clean package'
                sh "echo `date +%Y%m%d%H-`$env.BUILD_ID > build.v"
                sh "mvn clean package -Dmaven.test.skip=true"
           }
       }
       stage('构建docker') {  
            when {
                expression {  return  DEPLOY_NEW}
            }
            steps {
                echo 'build docker'
                 sh '''
                    v=`cat build.v`
                    cd $project_module_path
                    
                    if [ -f $config_path/$deploy_name/Dockerfile ]; then
                        \\cp $config_path/$deploy_name/Dockerfile .
                    fi
                    echo '\nRUN echo \'PINPOINT_NAME=\"${deploy_name}\"\' >> /opt/pinpoint-env.sh' >> Dockerfile
                    build_env="$config_path/build_env"
                    if [ -f $build_env ]; then
                        build_env_value=`cat $build_env`
                        echo '\nRUN echo \'JAVA_OPTS=\"-Dspring.profiles.active=${build_env_value}\"\' >> /opt/JAVA_OPTS.sh' >> Dockerfile
                    fi
                    docker build -t $hub_name/${deploy_name}:$v .
                  '''
            }
       }
       stage('发布到私服 ') {
            when {
                expression {  return  DEPLOY_NEW}
            }
           steps { 
               echo 'deploy docker'
              withCredentials([usernamePassword(credentialsId:"$credentialsId_hub", passwordVariable: 'login_password', usernameVariable: 'login_name')]) {
                 sh '''
                 v=`cat build.v`
                 docker login -u $login_name -p $login_password $hub_host
                 docker push $hub_name/${deploy_name}:$v
                 '''
                }
           }
       }
        stage('触发k8s') {
             when {
                expression {  return  DEPLOY_NEW}
            }
           steps { 
               echo 'deploy k8s'
                sh '''
                   v=`cat build.v`
                   count=`kubectl get deploy ${deploy_name} --namespace=$namespace|wc -l`
                   if [ $count == 2 ]
                   then
                        echo "exec update"
                        kubectl set image deployments/$deploy_name $deploy_name=$hub_name/${deploy_name}:$v --namespace=$namespace
                    else
                        echo "exec deploy"
                    kubectl run ${deploy_name} --image=$hub_name/${deploy_name}:$v --replicas=$replicas --namespace=$namespace
                fi
             '''
           }
        }
        stage('记录历史') {
             when {
                expression {  return  DEPLOY_NEW}
            }
           steps { 
                 sh '''
                 v=`cat build.v`
                if [ -d $config_path/buildhistory ]; then
                    echo "$hub_name/${deploy_name}:$v">>$config_path/buildhistory/${deploy_name}
                fi
                 '''
           }
        }
   }
}
</pre>
# 从私服拉取,namespace独立配置
kubectl create secret docker-registry alisc --docker-server=registry.cn-hangzhou.aliyuncs.com --docker-username=用户名 --docker-password=密码 --docker-email=用户名 --namespace=app
