def call(Map paramVars) {
	def mounts = ''
	def network = ''
	if(paramVars.mounts) {
		for(mount in paramVars.mounts) {
			if(mount === 'logs') {
				mounts += " --mount type=bind,source=${paramVars.projectRoot}/${mount},target=/${mount}"
			} else {
				mounts += " --mount type=bind,source=${paramVars.projectRoot}/${mount},target=/app/${mount}"
			}
		}
	}
	if(paramVars.networks){
		network = " --network ${paramVars.networks.pop()}"
	}
	
	pipeline {
		agent {
			label 'docker'
		}
		stages {
			stage ('Shutdown') {
				steps {
					catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
						sh "docker stop ${paramVars.name}"
					}
				}
			}
			stage ('Remove Container') {
				steps {
					catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
						sh "docker stop ${paramVars.name}"
					}
				}
			}
			stage ('Rotate Logs') {
				steps {
					dir(paramVars.projectRoot) {
						catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
							sh 'rotatelogs'
						}
					}
				}
			}
			stage ('Setup') {
				steps {
					script {
						sh "docker create --name ${paramVars.name} --restart always${mounts}${network} ${paramVars.imageName}"
						
					}
				}
			}
			stage ('Start') {
				steps {
					sh "docker start ${paramVars.name}"
				}
			}
		}
	}
}
