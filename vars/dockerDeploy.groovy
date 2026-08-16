/**
  This file defines the process for deploying docker compose stacks
  Requirements:
    - previously built docker image(s)
	- docker compose file for the stack
  Parameters (via Map variable):
    - name (String): Name of docker stack to deploy
    - composeFile (String): Path to the docker compose file defining the stack to deploy
	- [OPTIONAL] configFileId (String): Id of the config file to use for the deploy template
 */
def call(Map paramVars) {
	if (!paramVars.name) {
		throw new IllegalArgumentException('Missing Stack Name')
	}
	
	if (!paramVars.composeFile) {
		throw new IllegalArgumentException('Missing Compose File')
	}

	def configFileId = 'deploy-template'

	if (paramVars.configFileId) {
		configFileId = paramVars.configFileId
	}
	
	pipeline {
		agent {
			label 'built-in'
		}
		stages {
			stage ('Fetch Stack Config') {
				steps {
					catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
						sh "cp ${paramVars.composeFile} ./docker-compose.yaml"
					}
				}
			}
			stage ('Populate Deploy Config') {
				steps {
					script {
						configFileProvider([configFile(fileId: configFileId, targetLocation: 'deploy-template.yaml')]) {
							Map deployTemplate = readYaml file: 'deploy-template.yaml'
							if (deployTemplate == null) {
				            	error "Deploy template from '${configFileId}' was empty or invalid YAML."
				            }
							def compose = readYaml file: 'docker-compose.yaml'
							if (compose == null) {
				              error "Compose file is empty or could not be parsed as YAML."
				            }
				
				            if (!compose.containsKey('services') || compose.services == null) {
				              echo "No 'services' section found. Writing back unmodified file for consistency."
				              writeYaml file: 'docker-compose.yaml', data: compose, overwrite: true
				              return
				            }
				            if (!(compose.services instanceof Map)) {
				              error "'services' in docker-compose.yaml is not a mapping/object; cannot process."
				            }
							compose.services.each { serviceName, serviceDef ->
				              // serviceDef is null when YAML contains "services:\n  foo:" with no mapping under foo
				              if (serviceDef == null) {
				                compose.services[serviceName] = [ deploy: deployTemplate ]
				                echo "Added deploy to empty service '${serviceName}'."
				                return
				              }
				              if (!(serviceDef instanceof Map)) {
				                echo "Warning: service '${serviceName}' definition is not a mapping/object; skipping modification."
				                return
				              }
				              if (!serviceDef.containsKey('deploy') || serviceDef.deploy == null) {
				                serviceDef.deploy = deployTemplate
				                echo "Added deploy to service '${serviceName}'."
				              } else {
				                echo "Service '${serviceName}' already has a deploy section — leaving it unchanged."
				              }
				            }
				            writeYaml file: 'docker-compose.yaml', data: compose, overwrite: true
						}
					}
				}
			}
			stage ('Deploy Stack') {
				steps {
					sh "docker stack deploy --detach=false --compose-file docker-compose.yaml ${paramVars.name}"
				}
			}
		}
	}
}
