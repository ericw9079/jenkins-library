/**
  This file defines the common process for building docker compose stacks for deployment
  Requirements:
    - docker-compose.yaml file in the project root
  Parameters (via Map variable):
    - projectRoot (String): The root directory of the project
    - deployJob (String): Name of the Jenkins job to deploy the image
 */
def call(Map paramVars) {
	if (!paramVars.projectRoot) {
		throw new IllegalArgumentException('Project Root is required')
	}

	if (!paramVars.deployJob) {
		throw new IllegalArgumentException('Missing deploy job')
	}
	pipeline {
		agent {
			label 'built-in'
		}
		stages {
			stage ('Checkout') {
				steps {
					sh "rsync -ax ${paramVars.projectRoot} ./"
				}
			}
			stage ('Patch Compose File') {
				steps {
					script {
						if (!fileExists('docker-compose.yaml')) {
							error("Compose file not found in workspace")
						}
	
						// Read docker-compose.yaml into a Groovy map
						def compose = readYaml file: 'docker-compose.yaml'
						if (compose == null) {
							error("Failed to parse compose file")
						}
	
						// Ensure top-level secrets map and declare npmrc -> .npmrc
						if (compose.secrets == null) {
							compose.secrets = [:]
						}
						if (compose.secrets['npmrc'] == null) {
							compose.secrets['npmrc'] = [file: '.npmrc']
						}
	
						// Ensure services that have a build section include the npmrc secret
						if (compose.services != null) {
							compose.services.each { svcName, svcDef ->
								if (svcDef?.build) {
									// normalize build when it's a scalar (context shorthand)
									if (svcDef.build instanceof String) {
										svcDef.build = [context: svcDef.build]
									}
	
									if (svcDef.build.secrets == null) {
										svcDef.build.secrets = []
									}
	
									// detect presence of npmrc in various forms
									def hasNpmrc = svcDef.build.secrets.any { entry ->
										if (entry instanceof String) {
											return entry == 'npmrc'
										} else if (entry instanceof Map) {
											// possible map shapes: { source: npmrc } or { name: npmrc } or { npmrc: {} }
											if (entry.containsValue('npmrc')) return true
											if (entry['source'] == 'npmrc' || entry['name'] == 'npmrc') return true
											// sometimes a map may be { npmrc: null } or { npmrc: {} }
											return entry.keySet().any { k -> k == 'npmrc' }
										}
										return false
									}
	
									if (!hasNpmrc) {
										// append simple string entry; this is accepted by compose
										svcDef.build.secrets << 'npmrc'
										echo "Added npmrc secret to service '${svcName}'."
									}
								}
							}
						} else {
							echo "No services found"
						}
	
						// Overwrite the compose file in workspace with the modified map
						writeYaml file: 'docker-compose.yaml', data: compose, overwrite: true
					}
				}
			}
			stage ('Build Docker Image(s)') {
				steps {
					configFileProvider([configFile(fileId: 'npmrc', targetLocation: '.npmrc')]) {
						sh 'docker compose build'
					}
				}
			}
			stage ('Push Docker Image(s)') {
	            steps {
	                sh 'docker compose push'
	            }
	        }
			stage ('Clean') {
				steps {
					cleanWs()
				}
			}
			stage ('Deploy') {
				steps {
					build paramVars.deployJob
				}
			}
		}
	}
}
