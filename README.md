Mule Application Kit
====================

The Mule Application Kit allows the development of Mule application based on Maven tooling. This kit includes archetypes for building regular Mule applications.

Creating a Mule Application
--------------------------

Creating a mule application using the mule archetype project is extremely easy. Just invoke it as follows:

     mvn archetype:generate -DarchetypeGroupId=org.mule.tools.appkit -DarchetypeArtifactId=mule-appkit-archetype-mule-app \
	-DarchetypeVersion=3.3-SNAPSHOT -DgroupId=org.mule -DartifactId=mule-test-archetype -Dversion=1.0-SNAPSHOT \
	-DmuleVersion=3.2.1 -Dpackage=org.mule -Dtransports=file,http,jdbc,jms,vm -Dmodules=client,cxf,management,scripting,sxc,xml \
	-DstudioNature=false 
						
Archetype Parameters:

|parameter|description|default|
|:--------|:----------|:----------|
|archetypeGroupId|The group Id of the archetype This value must ALWAYS org.mule.tools|org.mule.tools|
|archetypeArtifactId|The artifact Id of the archetype| This value must ALWAYS mule-archetype-project|mule-archetype-project|
|archetypeVersion|The version of the archetype. This value can change as we release new versions of the archetype. Always use the latest non-SNAPSHOT version available.|1.5|
|groupId|The group Id of the application you are creating. A good value would be the reserve name of your company domain name, like: com.mulesoft.app or org.mule.app||
|artifactId|The artifact Id of the application you are creating. ||
|version|The version of your application. Usually 1.0-SNAPSHOT.|1.0-SNAPSHOT|
|muleVersion|The version of the mule runtime you are going to use. Mule 2.2.x is no longer supported|3.2.1|
|addAppToClasspath|A flag to either add the src/main/app/ folder as a resource folder to easily access it within your IDE|false|
|transports|A comma separated list of the transport you are going to use within your application.|file,http,jdbc,jms,vm |
|modules|A comma separated list of the modules you are going to use within your application. |client,cxf,management,scripting,sxc,xml |
|studioNature|A flag to enable studio nature, in the future will allow you to import the project to Studio |false |
|EE|A flag to import the EE counterpart of the transports/modules you are using. |false |
