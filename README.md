![TornadoFX Logo](https://raw.githubusercontent.com/edvin/tornadofx/master/graphics/tornado-fx-logo.png?raw=true "TornadoFX")

# TornadoFX IntelliJ IDEA Plugin

[![Apache License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)

## Download

This plugin is available in the [JetBrains Plugin Repository](https://plugins.jetbrains.com/plugin/8339) so you can download it directly from your IDE. 

## Issues or questions?

Please report any [issues](https://github.com/edvin/tornadofx-idea-plugin/issues)

### Features

#### Run Configuration

Run a TornadoFX Application or a View ad hoc, specify Live reloading of stylesheets and/or Views

#### Add View

Create new View class based on Code or FXML automatically

#### Inject Component

Search for Components (View, Fragment, Controller) and create injected val
 
### Convert fields to JavaFX Properties

Converts fields to JavaFX Properties and fields delegated by them
 
### Add TableView Columns

### Generate ViewModel

Generate ViewModel based on a class and its fields.

### Translation support

Fold `message["key"]` expression to their translated value. 

Add translation intention action.

#### Project Templates

- Maven Application
- Gradle Application
- OSGi Application
- OSGi Dynamic View
- OSGi Dynamic Stylesheet

## Running from source

Clone the project and open it in IntelliJ IDEA. Select the Gradle task `intellij > runIde` to start a sandboxed instance of IntelliJ IDEA with the plugin installed.
