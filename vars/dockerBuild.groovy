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
					sh "rsync -ax${excludes} ${paramVars.projectRoot} ./"
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
