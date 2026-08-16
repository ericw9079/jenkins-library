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
