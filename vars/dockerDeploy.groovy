/**
  This file defines the process for deploying docker images
  Requirements:
    - previously built docker image
  Parameters (via Map variable):
    - name (String): Name of docker container to deploy
    - imageName (String): Name of the image to launch the container from
    - [OPTIONAL] projectRoot (String): The root directory of the project (used for mounts and rotating the logs)
    - [OPTIONAL] ports (ArrayList<String>): List of ports to bind (no ports are bound by default)
    - [OPTIONAL] networks (ArrayList<String>): List of networks to connect the container to (connects to bridge network by default per docker default)
    - [OPTIONAL] mounts (ArrayList<String>): List of files/folders in the root directory to mount (nothing is mounted by default)
    - [OPTIONAL] volumes (Map<String,String>): Docker volumes to mount to the container in the form "<name>: <mount location>" (nothing is mounted by default)
    - [OPTIONAL] networkAlias (String): DNS alias to use on additional container networks (the first connected network will not use the alias)
 */
def call(Map paramVars) {
	def mounts = ''
	def network = ''
	def ports = ''
	def volumes = ''
	
	if(params.projectRoot && paramVars.mounts) {
		for(def mount in paramVars.mounts) {
			if(mount == 'logs') {
				mounts += " --mount type=bind,source=${paramVars.projectRoot}/${mount},target=/${mount}"
			} else {
				mounts += " --mount type=bind,source=${paramVars.projectRoot}/${mount},target=/app/${mount}"
			}
		}
	}

	if(paramVars.volumes) {
		for(def entry in paramVars.volumes) {
			volumes += " -v ${entry.key}:${entry.value}"
		}
	}
	
	if(paramVars.networks){
		network = " --network ${paramVars.networks.pop()}"
	}

	if(paramVars.ports) {
		for(def port in paramVars.ports) {
			ports += " -p ${port}"
		}
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
						sh "docker rm ${paramVars.name}"
					}
				}
			}
			stage ('Rotate Logs') {
				when {
					expression {
						return paramVars.projectRoot
					}
				}
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
						sh "docker create --name ${paramVars.name} --restart always${mounts}${volumes}${network} ${paramVars.imageName}"
						if (paramVars.networks && paramVars.networks.size() >= 1) {
							def networkAlias = paramVars.networkAlias ? " --alias ${paramVars.networkAlias}" : ''
							for(def additionalNetwork in paramVars.networks) {
								sh "docker network connect ${additionalNetwork} ${paramVars.name}${networkAlias}"
							}
						}
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
