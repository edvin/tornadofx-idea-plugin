#!/usr/bin/env bash

# zip adds this subdirectory, so we need to move to it so as to omit it.
cd template-projects

# Look at it relative to template-projects.
PROJECT_TEMPLATES=../resources/projectTemplates

zip_template() {
    zip -r ${PROJECT_TEMPLATES}/$1 $1
}

zip_template "tornadofx-maven-project"
zip_template "tornadofx-maven-osgi-project"
zip_template "tornadofx-maven-osgi-ds-project"
zip_template "tornadofx-gradle-project"

# Go back after zipping.
cd ..
