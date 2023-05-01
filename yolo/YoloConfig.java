package net.runelite.client.plugins.yolo;

import net.runelite.client.config.*;

@ConfigGroup("yolo")
public interface YoloConfig extends Config
{
	@ConfigSection(
			name = "Save Path",
			description = "The save path of the directory",
			position = 0
	)
	String SavePath = "SavePath";

	@ConfigItem(
			keyName = "saveDirectory",
			name = "Save Directory",
			description = "Configures where to save the yolo annoted xml files and image screenshots",
			position = 0,
			section = SavePath
	)
	default String getSaveDirectory()
	{
		return "";
	}

	@ConfigItem(
			keyName = "snapTimer",
			name = "Snap Image Timer (sec)",
			description = "The number of seconds delay in the loop for the plugin to take the next snapshot"
	)
	@Range(min = 1, max = 10)
	default int yoloSnapTimer()
	{
		return 5;
	}
}
