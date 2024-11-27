/**
  This file defines the common process for building docker images for deployment
  Requirements:
    - Dockerfile in the project root
  Parameters (via Map variable):
    - name (String): Name of the image to build
    - projectRoot (String): The root directory of the project
    - deployJob (String): Name of the Jenkins job to deploy the image
    - [OPTIONAL] excludes (ArrayList<String>): List of additional files and folders to exclude from the build context (logs directory is excluded by default)
 */
def call(Map paramVars) {
	def excludes = ''

	if(paramVars.excludes) {
		for(def exclude in paramVars.excludes) {
			excludes += " --exclude ${exclude}"
		}
	}
	
	pipeline {
		agent {
			label 'docker'
		}
		stages {
			stage ('Checkout') {
				steps {
					sh "rsync -ax --exclude logs/${excludes} ${paramVars.projectRoot} ./"
				}
			}
			stage ('Build') {
				steps {
					configFileProvider([configFile(fileId: 'npmrc', targetLocation: '.npmrc')]) {
						sh "docker build --rm --secret id=npmrc,src=.npmrc -t ${paramVars.name} ."
					}
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
