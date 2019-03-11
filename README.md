# Transmissible spongiform encephalopathies tool
The TSE data reporting tool is an open source Java client tool developed for the members of the Scientific Network for Zoonoses monitoring. The tool allows countries to submit and edit their data and automatically upload them into the EFSA Data Collection Framework (DCF) as XML data files.

<p align="center">
    <img src="icons/app-icon.png" alt="TSE icon"/>
    <img src="http://www.efsa.europa.eu/profiles/efsa/themes/responsive_efsa/logo.png" alt="European Food Safety Authority"/>
</p>

## Dependencies
The project needs the following dependencies in order to work properly:
* https://github.com/openefsa/Dcf-webservice-framework
* https://github.com/openefsa/EFSA-RCL
* https://github.com/openefsa/email-generator
* https://github.com/openefsa/http-manager
* https://github.com/openefsa/http-manager-gui
* https://github.com/openefsa/java-exception-to-string
* https://github.com/openefsa/Progress-bar
* https://github.com/openefsa/sql-script-executor
* https://github.com/openefsa/version-manager
* https://github.com/openefsa/java-swt-window-size-save-and-restore
* https://github.com/openefsa/zip-manager

## Import the project in Eclipse IDE
In order to import the TSE project correctly into the Eclipse development environment, it is necessary to download the TSE together with all its dependencies. Next, in order to allow an easy import into the IDE, extract all the zip packets inside the eclipse workspace. 
At this stage you can simply open the IDE and and import all the projects just extracted one by one.

_Note that the TSE and its dependencies make use of the Maven technology which automatically download and set up all the jar files useful for the proper functioning of the tool._

The only projects which require to manually configure the build path are the **TSE**, **EFSA-RCL** and the **HttpManager GUI** which are using the **Jface** jar file downloadable from the following [link](http://www.java2s.com/Code/JarDownload/org.eclipse/org.eclipse.jface-3.8.jar.zip).

For further information on how to use the tool and how to correctly install it in your local computer refer to the wiki page.

