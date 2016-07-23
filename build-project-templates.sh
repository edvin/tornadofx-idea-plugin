#!/usr/bin/env bash

rm resources/projectTemplates/*.zip
cd template-projects
zip -r ../resources/projectTemplates/tornadofx-maven-project.zip tornadofx-maven-project
zip -r ../resources/projectTemplates/tornadofx-gradle-project.zip tornadofx-gradle-project