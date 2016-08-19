#!/usr/bin/env bash

# Remember to build each template via Tools -> "Save project as template"
IDEA_PROJECT_TEMPLATES=~/.IntelliJIdea2016.2/config/projectTemplates

#IDEA_PROJECT_TEMPLATES=~/Library/Preferences/IntelliJIdea2016.2/projectTemplates

PLUGIN_PROJECT_TEMPLATES=resources/projectTemplates

cp ${IDEA_PROJECT_TEMPLATES}/tornadofx-maven-project.zip ${PLUGIN_PROJECT_TEMPLATES}
cp ${IDEA_PROJECT_TEMPLATES}/tornadofx-maven-osgi-project.zip ${PLUGIN_PROJECT_TEMPLATES}
cp ${IDEA_PROJECT_TEMPLATES}/tornadofx-maven-osgi-ds-project.zip ${PLUGIN_PROJECT_TEMPLATES}
cp ${IDEA_PROJECT_TEMPLATES}/tornadofx-gradle-project.zip ${PLUGIN_PROJECT_TEMPLATES}
