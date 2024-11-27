def call(Map params) {
	pipeline {
		agent {
			label 'docker'
		}
		stages {
			stage ('Checkout') {
				steps {
				}
			}
			stage ('Build') {
				steps {
          configFileProvider([configFile(fileId: 'npmrc', targetLocation: '.npmrc')]) {
            sh 'docker build --rm --secret id=npmrc,src=.npmrc -t ${params.name} .'
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
          build params.deployJob
				}
			}
		}
	}
}
