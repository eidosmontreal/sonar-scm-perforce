![Support](https://img.shields.io/badge/Support-Best%20Effort-yellow.svg)

# SonarQube Perforce plugin
[![Build Status](https://api.travis-ci.com/eidosmontreal/sonar-scm-perforce.svg?branch=sonarqube-v9)](https://app.travis-ci.com/github/eidosmontreal/sonar-scm-perforce)

## Description
This plugin implements SCM dependent features of SonarQube for [Perforce](http://www.perforce.com/) projects. It's a pure Java implementation so no need to have p4 command line tool installed on the computer doing the SQ analysis.

## Usage
There is no autodetection so you should activate the provider using -Dsonar.scm.provider=perforce.

Mandatory properties:

Key | Description
--- | -----------
sonar.perforce.port | The host and port number of the Perforce service with which to communicate. Format is host:port.
sonar.perforce.username | Username to be used for Perforce authentication. Mandatory.
sonar.perforce.password.secured | Password to be used for Perforce authentication. If not set it will try to reuse existing ticket for provided user.
sonar.perforce.clientName | Name of the client/workspace the project belongs to.

You can also configure some optional properties:

Key | Description                                                                                                                  | Default value
--- |------------------------------------------------------------------------------------------------------------------------------| -------------
sonar.perforce.useSsl | Use SSL protocol (p4javassl://) to connect to server                                                                         | false
sonar.perforce.charset | Character set used for translation of unicode files (P4CHARSET)                                                              
sonar.perforce.sockSoTimeout | Perforce socket read timeout for communicating with the Perforce service (milliseconds)                                      | 30000 (30s)
sonar.perforce.clientImpersonatedHostname | Name of the host computer to impersonate (P4HOST)                                                                            |
sonar.perforce.swarm | The full url of your swarm or p4view browser, eg (http://swarm.yourcompany.com/)                                             |
sonar.perforce.workingDirectory | The actual path to the source code (same as option `-d` in `p4`); defaults to the one configured in the workspace definition |

## Known Limitations
* No auto-detection since nothing in workspace seems to show this is under Perforce control management (like .git folder for Git workspace).

## Developer information
The plugin uses the p4java pure Java implementation of Perforce client: https://www.perforce.com/manuals/p4java/Content/P4Java/chapter.p4java.html
