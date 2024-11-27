def call(Map params) {
	pipeline {
		agent {
			label 'docker'
		}
		stages {
			stage ('Shutdown') {
				steps {
					catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
						sh 'docker stop ${params.name}'
					}
				}
			}
			stage ('Remove Container') {
				steps {
					catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
						sh 'docker stop ${params.name}'
					}
				}
			}
			stage ('Rotate Logs') {
				steps {
				}
			}
			stage ('Setup') {
				steps {
				}
			}
			stage ('Start') {
				steps {
					sh 'docker start ${params.name}'
				}
			}
		}
	}
}
