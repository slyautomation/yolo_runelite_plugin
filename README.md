# yolo_runelite_plugin

Check out these guides on using this plugin: 

https://www.slyautomation.com/blog/how-to-use-the-yolo-extracts-plugin-for-runelite/
https://www.slyautomation.com/blog/how-to-configure-the-yolo-plugin-in-java/

## Import Required Packages:

Begin by importing the necessary packages in your Java code, specifically the net.runelite.client.config package.
Define YoloConfig Interface:

Create the YoloConfig interface that extends the Config interface, containing configuration options for the YOLO plugin.
## Configure Save Path:

Use the @ConfigSection annotation to set the save path for the directory where annotated XML files and image screenshots will be stored.
## Configure Save Directory:

Define the save directory option using the @ConfigItem annotation, specifying keyName, name, description, position, and section parameters.
## Configure Snap Image Timer:

Set up the snap image timer using the @ConfigItem annotation with the @Range annotation to define acceptable values for the delay between snapshots.
## Implement Customization:

Modify default values and ranges for configuration options as needed, adding additional options following the same pattern.
## Conclusion:

Successfully configure the YOLO plugin in your Java application by importing packages, defining the YoloConfig interface, and configuring options like save path, save directory, and snap image timer. Additional information on using the YOLO plugin is provided in the conclusion.
