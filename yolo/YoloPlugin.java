package net.runelite.client.plugins.yolo;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneScapeProfileType;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientUI;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.util.ImageCapture;
import net.runelite.client.util.Text;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static net.runelite.api.Perspective.localToCanvas;
import static net.runelite.client.RuneLite.SCREENSHOT_DIR;


@PluginDescriptor(
		name = "Yolo Extracts",
		description = "takes screenshots and create yolo formatted txt files for Machine Learning",
		tags = {"external", "images", "imgur", "integration", "notifications"},
		enabledByDefault = false
)
@Slf4j
public class YoloPlugin extends Plugin
{
	@Inject
	private ClientUI clientUi;

	@Inject
	private Client client;

	@Inject
	private DrawManager drawManager;

	public long currentTime = 0;

	@Inject
	public YoloConfig config;
	@Inject
	private ScheduledExecutorService executor;

	public int MAX_DISTANCE = 2000;
	public int SNAP_TIMER = 3;
	public boolean turnOff = false;
	public String SAVE_DIRECTORY = null;
	@Inject
	private ImageCapture imageCapture;
	@Provides
	private YoloConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(YoloConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		SNAP_TIMER = config.yoloSnapTimer();
		System.out.println("Yolo Loaded!");
		final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
		executorService.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				try
				{
					if (turnOff == true){
						executorService.shutdown();
						System.out.println("Yolo Plugin Shutting Down!!!");
						return;
					}
					handleNPC();
				}
				finally
				{
				}

			}
		}, 1, SNAP_TIMER, TimeUnit.SECONDS);
	}
	@Override
	protected void shutDown() throws Exception
	{
		turnOff = true;
	}
	@VisibleForTesting
	void takeScreenshot(String fileName, String subDir)
	{
		if (client.getGameState() == GameState.LOGIN_SCREEN)
		{
			// Prevent the screenshot from being captured
			System.out.println("Login screenshot prevented");
			return;
		}

		Consumer<Image> imageCallback = (img) ->
		{
			// This callback is on the game thread, move to executor thread
			executor.submit(() -> takeScreenshot(fileName, subDir, img));
		};

		drawManager.requestNextFrameListener(imageCallback);
	}

	private void takeScreenshot(String fileName, String subDir, Image image)
	{
		if (client.getGameState() == GameState.LOGIN_SCREEN)
		{
			// Prevent the screenshot from being captured
			System.out.println("Login screenshot prevented");
			return;
		}
		if (subDir.isEmpty())
		{
			String playerDir = "";
			if (client.getLocalPlayer() != null && client.getLocalPlayer().getName() != null)
			{
				playerDir = client.getLocalPlayer().getName();
				RuneScapeProfileType profileType = RuneScapeProfileType.getCurrent(client);
				if (profileType != RuneScapeProfileType.STANDARD)
				{
					playerDir += "-" + Text.titleCase(profileType);
				}

				if (!Strings.isNullOrEmpty(subDir))
				{
					playerDir += File.separator + subDir;
				}
			}

			subDir = SCREENSHOT_DIR + "\\" + playerDir;
			System.out.println(SCREENSHOT_DIR + "\\" + playerDir);
		}


		BufferedImage screenshot = true
				? new BufferedImage(clientUi.getWidth(), clientUi.getHeight(), BufferedImage.TYPE_INT_ARGB)
				: new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);

		Graphics graphics = screenshot.getGraphics();

		int gameOffsetX = 0;
		int gameOffsetY = 0;

		// Draw the client frame onto the screenshot
		try
		{
			SwingUtilities.invokeAndWait(() -> clientUi.paint(graphics));
		}
		catch (InterruptedException | InvocationTargetException e)
		{
			System.out.println("unable to paint client UI on screenshot");
		}

		// Evaluate the position of the game inside the frame
		final Point canvasOffset = clientUi.getCanvasOffset();
		gameOffsetX = canvasOffset.getX();
		gameOffsetY = canvasOffset.getY();


		// Draw the game onto the screenshot
		graphics.drawImage(image, gameOffsetX, gameOffsetY, null);
		try
		{
			File screenshotFile = new File(subDir, fileName + ".png");

			// To make sure that screenshots don't get overwritten, check if file exists,
			// and if it does create file with same name and suffix.
			int i = 1;
			while (screenshotFile.exists())
			{
				screenshotFile = new File(subDir, fileName + String.format("(%d)", i++) + ".png");
			}

			ImageIO.write(screenshot, "PNG", screenshotFile);
		}
		catch (IOException e)
		{
			System.out.println("An error occurred while taking Screenshots.");
		}

	}
	@Subscribe
	public void onGameTick(GameTick tick)
	{
		System.out.println("Yolo Running!!!");

	}
	public void CreateFile(String datetime) {
		String path;
		path = SAVE_DIRECTORY + datetime + ".txt";
		try {
			File myObj = new File(path);
			if (myObj.createNewFile()) {
				System.out.println("File created: " + myObj.getName());
			} else {
				System.out.println("File already exists for txt file.");
			}
		} catch (IOException e) {
			System.out.println("An error occurred while creating txt File.");
		}
	}
	public void handleNPC()
	{
		SAVE_DIRECTORY = config.getSaveDirectory();
		if (SAVE_DIRECTORY.isEmpty())
		{
			String playerDir = "";
			if (client.getLocalPlayer() != null && client.getLocalPlayer().getName() != null)
			{
				playerDir = client.getLocalPlayer().getName();
				RuneScapeProfileType profileType = RuneScapeProfileType.getCurrent(client);
				if (profileType != RuneScapeProfileType.STANDARD)
				{
					playerDir += "-" + Text.titleCase(profileType);
				}

				if (!Strings.isNullOrEmpty(SAVE_DIRECTORY))
				{
					playerDir += File.separator + SAVE_DIRECTORY;
				}

			}
			SAVE_DIRECTORY = SCREENSHOT_DIR + "\\" + playerDir;
		}
		SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH-mm-ss");
		Date rawdate = new Date();
		String datetime = formatter.format(rawdate);
		java.util.List<NPC> npcs = client.getNpcs();
		Player player = client.getLocalPlayer();
		Rectangle r = client.getCanvas().getBounds();
		String minx = null, maxx = null, miny = null, maxy = null, name = null;
		//log.info("window size: " + h + ", " + w);
		int[] localXY = new int[]{0, 0};
		List<String> yolo = new ArrayList<>();
		List<String> yoloName = new ArrayList<>();
		List<String> yoloMinx = new ArrayList<>();
		List<String> yoloMaxx = new ArrayList<>();
		List<String> yoloMiny = new ArrayList<>();
		List<String> yoloMaxy = new ArrayList<>();
		for (NPC npc : npcs)
		{

			if (npc != null)
			{

				NPCComposition composition = npc.getComposition();
				if (composition == null || !composition.isInteractible())
				{
					return;
				}
				if (player.getLocalLocation().distanceTo(npc.getLocalLocation()) <= MAX_DISTANCE)
				{
					Shape objectClickbox = npc.getConvexHull();

					if (objectClickbox != null)
					{
						minx = String.valueOf( (int) objectClickbox.getBounds().getMinX());
						maxx = String.valueOf((int) objectClickbox.getBounds().getMaxX() + 4);
						miny = String.valueOf((int) objectClickbox.getBounds().getMinY() + 22);
						maxy = String.valueOf((int) objectClickbox.getBounds().getMaxY() + 27);
						LocalPoint npcLocation = npc.getLocalLocation();
						int playerPlane = player.getWorldLocation().getPlane();
						Point npcCanvasFinal = localToCanvas(client, npcLocation, playerPlane);
						localXY = new int[]{npcCanvasFinal.getX(), npcCanvasFinal.getY()};
						if (localXY[0] > 1 && localXY[0] < clientUi.getWidth())
						{
							if (localXY[1] > 1 && localXY[1] < clientUi.getHeight())
							{
								name = npc.getName();
								//String xy = Arrays.toString(localXY) + ", (" + npc.getWorldLocation().getX() + ", " + npc.getWorldLocation().getY() + ")";
								System.out.println(name + ": " + minx + ", " + maxx + ", " + miny + ", " + maxy);
								//double[] arr = convert_box(size, box);
								//System.out.println(Arrays.toString(arr));
								// + 4 on x and + 26 on y
								yolo.add(name + ": " + minx + " " + maxx + " " + miny + " " + maxy);
								yoloName.add(name);
								yoloMinx.add(minx);
								yoloMaxx.add(maxx);
								yoloMiny.add(miny);
								yoloMaxy.add(maxy);
							}

						}
					}
				}
			}
		}
		if(!yolo.isEmpty())
		{
			if (SAVE_DIRECTORY.substring(SAVE_DIRECTORY.length() - 1) != "\\"){
				SAVE_DIRECTORY += "\\";
			}
			System.out.println("SAVE DIRECTORY: " + SAVE_DIRECTORY);
			String path;
			CreateFile(datetime);

			path = SAVE_DIRECTORY + datetime + ".txt";
			Path output = Paths.get(path);
			try
			{
				Files.write(output, yolo);
			}
			catch (Exception e)
			{
				System.out.println("An error occurred writing yolo output to txt file.");
			}
			takeScreenshot(datetime, SAVE_DIRECTORY);
			try
			{
				annoteFiles(datetime, yoloName, yoloMinx, yoloMaxx, yoloMiny, yoloMaxy);
			}
			catch (Exception e)
			{
				System.out.println("An error occurred performing annoteFiles function.");
			}
		}
	}
	public void annoteFiles(String datetime, List yoloName, List yoloMinx, List yoloMaxx, List yoloMiny, List yoloMaxy){
		System.out.println("annote files:" + SAVE_DIRECTORY + datetime + ".png");
		String width = String.valueOf(clientUi.getWidth());
		String height = String.valueOf(clientUi.getHeight());
		String depth = "3";
		String file_name = datetime + ".png";
		String folder_name = SAVE_DIRECTORY;
		String file_path = SAVE_DIRECTORY;

		try {
			File myObj = new File(SAVE_DIRECTORY + datetime + ".xml");
			Path output = Paths.get(SAVE_DIRECTORY + datetime + ".xml");
			try
			{
				Files.write(output, "<annotation>\n".getBytes(), StandardOpenOption.CREATE,
						StandardOpenOption.APPEND);

				Files.write(output, "\t<folder>".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
				Files.write(output, folder_name.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
				Files.write(output, "</folder>\n".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

				Files.write(output, "\t<filename>".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
				Files.write(output, file_name.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
				Files.write(output, "</filename>\n".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

				Files.write(output, "\t<path>".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
				Files.write(output, file_path.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
				Files.write(output, "</path>\n".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

				Files.write(output, "\t<size>\n".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

				Files.write(output, "\t\t<width>".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
				Files.write(output, width.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
				Files.write(output, "</width>\n".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

				Files.write(output, "\t\t<height>".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
				Files.write(output, height.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
				Files.write(output, "</height>\n".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
				Files.write(output, "\t\t<depth>".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
				Files.write(output, depth.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
				Files.write(output, "</depth>\n".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
				Files.write(output, "\t</size>\n".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
				int endLoop = yoloName.size();
				System.out.println("endLoop: " + endLoop);
				for (int i = 0; i < endLoop; i++) {
					Files.write(output, "\t<object>\n".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
					add_objects(output, yoloName, yoloMinx, yoloMaxx, yoloMiny, yoloMaxy, i);
					Files.write(output, "\t</object>\n".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
				}
				Files.write(output, "</annotation>\n".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			}
			catch (IOException e)
			{
				System.out.println("Annote files, an error occurred.");
			}
			if (myObj.createNewFile()) {
				System.out.println("File created: " + myObj.getName());
			} else {
				System.out.println("File already exists for XML.");
			}
		} catch (IOException e) {
			System.out.println("XML file, An error occurred.");
		}

	}
	public void add_objects(Path output, List yoloName, List yoloMinx, List yoloMaxx, List yoloMiny, List yoloMaxy, int x){
		try
		{
			String name = (String) yoloName.get(x);
			String minx = (String) yoloMinx.get(x);
			String maxx = (String) yoloMaxx.get(x);
			String miny = (String) yoloMiny.get(x);
			String maxy = (String) yoloMaxy.get(x);
			//System.out.println("name: " + name);
			Files.write(output, "\t\t<name>".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			Files.write(output, name.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			Files.write(output, "</name>\n".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			Files.write(output, "\t\t<pose>".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			Files.write(output, "Unspecified".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			Files.write(output, "</pose>\n".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

			Files.write(output, "\t\t<truncated>".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			Files.write(output, "0".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			Files.write(output, "</truncated>\n".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

			Files.write(output, "\t\t<difficult>".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			Files.write(output, "0".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			Files.write(output, "</difficult>\n".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

			Files.write(output, "\t\t<bndbox>\n".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

			Files.write(output, "\t\t\t<xmin>".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			Files.write(output, minx.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			Files.write(output, "</xmin>\n".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

			Files.write(output, "\t\t\t<ymin>".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			Files.write(output, miny.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			Files.write(output, "</ymin>\n".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

			Files.write(output, "\t\t\t<xmax>".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			Files.write(output, maxx.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			Files.write(output, "</xmax>\n".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

			Files.write(output, "\t\t\t<ymax>".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			Files.write(output, maxy.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			Files.write(output, "</ymax>\n".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

			Files.write(output, "\t\t</bndbox>\n".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		}
		catch (IOException e) {
			System.out.println("adding objects xml, An error occurred.");
		}
	}

}


