import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import javax.imageio.ImageIO;

public class MainGenerator {
	BufferedImage image;
	PathCell[][] mazeConnections;
	long randomSeed;

	// generation settings
	public int width = 30; //width of the grid
	public int height = 20; //height of the grid
	
	//size of visuals settings
	public int scaling = 27; // by how much the image gets scaled from the small image into the large and textured versions
	public int wallPathRatio = 3; // how wide the path in a cell is compared to the walls, there are always 1unit thick walls
	
	//only applies to scaled untextured
	public boolean pathTiling = true; // if the white paths should have a tiling form when scaled
	
	//Settings for maze design and routes
	public boolean limitedExtraPaths = true; //if the algorithm should be forbidden from creating multiroutes randomly. Turning this off will mostly result in more/larger rooms as it is a more concentrated effect
	public boolean shortLoopToRoom = true; // If four neighboring connected cells should be a room
	public boolean multiPaths = true; // If more paths should be added 
	public int deadEndRemovalRate = 100;//the percentage chance of removing a found dead end
	
	//settings for texturing methods
	public boolean blackWall = false; //if the walls should be textured black
	public boolean fullWall = false; //if the entire wall areas should be textured. Not recommended as it will probably look horrible
	
	//textures
	public String floorTexture = "wack.jpg"; //texture used for the floor
	public String wallTexture = "wall2.jpg"; //texture used for the walls
	
	//Obsolete settings
	public int wallWidthScaled = 5;//obsolete, was meant to allow varied texture thickness of walls, but the system did not allow it. Purpose can be fulfilled with wallPathRatio
	public int range = 70; //obsolete, was meant for texturing randomly, but new method was found

	
	String settingInfo = "";

	public MainGenerator() {
		randomSeed = PathCell.randomizer.nextLong();
		//randomSeed = 1692654678779504029L; // for recreating maze with new setting
		PathCell.randomizer.setSeed(randomSeed);
		
		//add setting information to filenames
		settingInfo += randomSeed + "L_" + width + "x" + height + "_S" + scaling + "_Rat" + wallPathRatio + "_DeRa"
				+ deadEndRemovalRate + "_";
		if (pathTiling)
			settingInfo += "T";
		if (shortLoopToRoom)
			settingInfo += "R";
		if (multiPaths)
			settingInfo += "M";
		System.out.println(settingInfo);
		
		//setup array
		mazeConnections = new PathCell[height][width];
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				mazeConnections[i][j] = new PathCell();
				PathCell.directionShuffle();
				for (int dir : PathCell.directions) {
					mazeConnections[i][j].unlinked.add(dir);
				}
			}
		}
		
		//clear edge of impossible paths
		PathCell cell;
		for (int i = 0; i < height; i++) {
			cell = mazeConnections[i][0];
			cell.semiLink(3);
			cell.linked.remove(cell.linked.size()-1);
		}
		for (int i = 0; i < height; i++) {
			cell = mazeConnections[i][width - 1];
			cell.semiLink(1);
			cell.linked.remove(cell.linked.size()-1);
		}
		for (int j = 0; j < width; j++) {
			cell = mazeConnections[0][j];
			cell.semiLink(0);
			cell.linked.remove(cell.linked.size()-1);
		}
		for (int j = 0; j < width; j++) {
			cell = mazeConnections[height - 1][j];
			cell.semiLink(2);
			cell.linked.remove(cell.linked.size()-1);
		}
	}

	public static void main(String[] args) {

		long old = System.nanoTime();
		MainGenerator gen = new MainGenerator();
		gen.generate();
		System.out.println("Post Gen: "+(System.nanoTime() - old) * 0.000001);
		if (gen.multiPaths) {
			gen.generateMultiRoutes();
			System.out.println("Post Routes: "+(System.nanoTime() - old) * 0.000001);
		}
		if (gen.deadEndRemovalRate > 0) {
			gen.removeShortDeadEnds();
			System.out.println("Post Ends: "+(System.nanoTime() - old) * 0.000001);
		}
		gen.createImage();
		System.out.println("Post Small Image: "+(System.nanoTime() - old) * 0.000001);
		if (gen.shortLoopToRoom) {
			gen.drawRooms();
			System.out.println("Post Room: "+(System.nanoTime() - old) * 0.000001);
		}
		gen.drawSmallImage();
		System.out.println("Post Draw small: "+(System.nanoTime() - old) * 0.000001);
		//gen.scaleImage();
		System.out.println("Post Draw Scale: "+(System.nanoTime() - old) * 0.000001);
		//gen.colorImage();
		gen.colorImageTextured(gen.floorTexture, gen.wallTexture);
		System.out.println("Post Draw Color: "+(System.nanoTime() - old) * 0.000001);
	}

	//draws the small image of the maze into image
	private void createImage() {
		image = new BufferedImage(width * (wallPathRatio + 2), height * (wallPathRatio + 2),
				BufferedImage.TYPE_INT_RGB);
		System.out.println(image.getWidth() + " " + image.getHeight());
		Graphics2D drawer = image.createGraphics();
		PathCell cell;
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				
				//get cell and scale positions for ratios
				cell = mazeConnections[i][j];
				int x = (wallPathRatio + 2) * j;
				int y = (wallPathRatio + 2) * i;

				if (!cell.disconnected)	{//dont color if disconnected	
					//draw cell center
					drawer.setColor(Color.WHITE);
					drawer.fillRect(x + 1, y + 1, (wallPathRatio), (wallPathRatio));
				}
				else {
					continue; 
				}
				
				//setup wall texturing if desired
				if(!fullWall&&!blackWall) 
					drawer.setColor(Color.GRAY);
				else 
					drawer.setColor(Color.BLACK);
				//draw cell border
				drawer.drawRect(x, y, (wallPathRatio + 1), (wallPathRatio + 1));
				
				//draw cell exits
				drawer.setColor(Color.WHITE);
				if (cell.linked.contains(0))
					drawer.fillRect(x + 1, y, (wallPathRatio), 1);
				if (cell.linked.contains(1))
					drawer.fillRect(x + (wallPathRatio + 1), y + 1, 1, (wallPathRatio));
				if (cell.linked.contains(2))
					drawer.fillRect(x + 1, y + (wallPathRatio + 1), (wallPathRatio), 1);
				if (cell.linked.contains(3))
					drawer.fillRect(x, y + 1, 1, (wallPathRatio));
			}
		}
		drawer.dispose();
	}
	//stores the small image into the respective file
	public void drawSmallImage() {
		File file = new File(settingInfo + "_maze.png");
		try {
			ImageIO.write(image, "png", file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	//scale image using the small image and store it in the respective file
	public void scaleImage() {
		BufferedImage imageScaled = new BufferedImage(width * (wallPathRatio + 2) * scaling,
				height * (wallPathRatio + 2) * scaling, BufferedImage.TYPE_INT_RGB);
		System.out.println(imageScaled.getWidth() + " " + imageScaled.getHeight());
		
		//determine tiling
		int tilingSetting = pathTiling ? 1 : 0;
		Graphics2D drawer = imageScaled.createGraphics();
		
		//draw scaled image
		for (int i = 0; i < image.getHeight(); i++) {
			for (int j = 0; j < image.getWidth(); j++) {
				drawer.setColor(new Color(image.getRGB(j, i)));
				drawer.fillRect(j * scaling, i * scaling, 
						scaling - 1 * tilingSetting, scaling - 1 * tilingSetting);
			}
		}
		drawer.dispose();
		File file = new File(settingInfo + "_maze_scaled.png");
		try {
			ImageIO.write(imageScaled, "png", file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//scale up the image from the small image and texture it depending on the colors of the small image and settings
	//and then store the image in the respective file
	public void colorImageTextured(String floorTexturePath, String wallTexturePath) {
		BufferedImage imageColored = new BufferedImage(width * (wallPathRatio + 2) * scaling,
				height * (wallPathRatio + 2) * scaling, BufferedImage.TYPE_INT_RGB);
		System.out.println(imageColored.getWidth() + " " + imageColored.getHeight());
		BufferedImage floor;
		BufferedImage wall;
		try {
			//read out textures
			floor = ImageIO.read(new File(floorTexturePath));
			wall = ImageIO.read(new File(wallTexturePath));
			
			//color the entire image based on the floor
			for(int x = 0;x<imageColored.getWidth();x++) {
				for(int y = 0;y<imageColored.getHeight();y++) {
					
				}
			}
			
			Graphics2D drawer = imageColored.createGraphics();
			drawer.setColor(Color.BLACK);
			for (int i = 0; i < image.getHeight(); i++) {
				for (int j = 0; j < image.getWidth(); j++) {
					//color wall where black with black, if fullWall is enabled use wallTexture instead
					if (new Color(image.getRGB(j, i)).equals(Color.BLACK)){
						//black coloring
						if(blackWall||!fullWall) {
							drawer.fillRect(j * scaling, i * scaling, scaling, scaling);
							continue;
						}
						//texture coloring
						for(int x = 0;x<scaling;x++) {
							for(int y = 0;y<scaling;y++) {
								imageColored.setRGB(j*scaling+x,i*scaling+ y, 
										wall.getRGB((j*scaling+x)%wall.getWidth(), (i*scaling+y)%wall.getHeight()));
							}
						}
					}

					//color wall where gray
					else if (new Color(image.getRGB(j, i)).equals(Color.GRAY)){
						for(int x = 0;x<scaling;x++) {
							for(int y = 0;y<scaling;y++) {
								imageColored.setRGB(j*scaling+x,i*scaling+ y, 
										wall.getRGB((j*scaling+x)%wall.getWidth(), (i*scaling+y)%wall.getHeight()));
							}
						}
					}
					//color floor where white
					else if (new Color(image.getRGB(j, i)).equals(Color.WHITE)){
						for(int x = 0;x<scaling;x++) {
							for(int y = 0;y<scaling;y++) {
								imageColored.setRGB(j*scaling+x,i*scaling+ y, 
										floor.getRGB((j*scaling+x)%floor.getWidth(), (i*scaling+y)%floor.getHeight()));
							}
						}
					}
				}
			}

			File file = new File(settingInfo + "_maze_textured.png");
			try {
				ImageIO.write(imageColored, "png", file);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	//obsolete so far, checks if the postion would be border of a cell using wallWidthScaled as determiner
	private boolean wallEdgeCheck(int x, int y) {
		if(x<=scaling/wallWidthScaled
				||x>=scaling-scaling/wallWidthScaled
				||y<=scaling/wallWidthScaled
				||y>=scaling-scaling/wallWidthScaled)
			return true;
		
		return false;
	}
	
	//obsolete
	//color the image with randomly generated gray tiling by scaling the small image up and store it in the respective file
	public void colorImage() {
		BufferedImage imageColored = new BufferedImage(width * (wallPathRatio + 2) * scaling,
				height * (wallPathRatio + 2) * scaling, BufferedImage.TYPE_INT_RGB);
		System.out.println(imageColored.getWidth() + " " + imageColored.getHeight());
		int tilingSetting = pathTiling ? 1 : 0;
		Graphics2D drawer = imageColored.createGraphics();
		Random r = new Random();
		for (int i = 0; i < image.getHeight(); i++) {
			for (int j = 0; j < image.getWidth(); j++) {
				if (new Color(image.getRGB(j, i)).equals(Color.BLACK))
					continue;
				for (int x = tilingSetting; x < scaling - tilingSetting; x++) {
					for (int y = tilingSetting; y < scaling - tilingSetting; y++) {
						int c = r.nextInt(range) + 130;
						drawer.setColor(new Color(c, c, c));
						drawer.fillRect(x + j * scaling, y + i * scaling, 1, 1);
					}
				}
			}
		}
		drawer.dispose();
		File file = new File(settingInfo + "_maze_colored.png");
		try {
			ImageIO.write(imageColored, "png", file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//union find: union function
	public void union(PathCell cellA, PathCell cellB) {
		find(cellA).representative = find(cellB);

	}

	//union find: find function
	public PathCell find(PathCell cell) {
		if (cell.representative != cell) {
			cell.representative = find(cell.representative);
			return cell.representative;
		}
		return cell;
	}
	
	//navigate the maze and randomly union the cells to create paths
	public void generate() {
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				randomUnion(j, i);
			}
		}
	}

	//randomly select cells in the maze to union again in order to create more routes
	public void generateMultiRoutes() {
		int connections = (int) Math.sqrt(width * width + height * height) * 2;
		int randX;
		int randY;
		for (int i = 0; i < connections; i++) {
			randX = PathCell.randomizer.nextInt(width);
			randY = PathCell.randomizer.nextInt(height);
			PathCell cellA = mazeConnections[randY][randX];
			if (cellA.unlinked.size() == 0) {
				connections++;
				continue;
			}
			int dir = cellA.unlinked.get(0);
			PathCell cellB;
			cellB = getNeighborCell(randX, randY, dir);
			cellA.semiLink(dir);
			cellB.semiLink(flipDirections(dir));
			union(cellA, cellB);
		}
	}

	//turn the small circles into rooms by removing the walls in the connected corner
	public void drawRooms() {
		Graphics2D drawer = image.createGraphics();
		drawer.setColor(Color.WHITE);
		PathCell cell;
		for (int i = 0; i < height - 1; i++) {
			for (int j = 0; j < width - 1; j++) {
				cell = mazeConnections[i][j];
				//check if there is a connected corner
				if (!cell.unlinked.contains(1) && !cell.unlinked.contains(2)
						&& !getNeighborCell(j, i, 1).unlinked.contains(2)
						&& !getNeighborCell(j, i, 2).unlinked.contains(1)) {// if connected below&&right, right
																			// connected below and below connected right
					drawer.fillRect(j * (wallPathRatio + 2) + (1 + wallPathRatio),
							i * (wallPathRatio + 2) + (1 + wallPathRatio), 2, 2);
				}
			}
		}
	}

	//navigate the maze and remove dead ends with the given probability from the settings
	public void removeShortDeadEnds() {
		PathCell cell;
		for (int i = 0; i < height - 1; i++) {
			for (int j = 0; j < width - 1; j++) {
				cell = mazeConnections[i][j];
				if (cell.linked.size() == 1 && PathCell.randomizer.nextInt(100) + 1 <= deadEndRemovalRate) {
					cell.disconnected = true;
					int dir = cell.linked.get(0);
					PathCell cellB;
					cellB = getNeighborCell(j, i, dir);
					cell.unSemiLink(dir);
					cellB.unSemiLink(flipDirections(dir));
				}
			}
		}
	}

	
	//pick a random unlinked neighbor and link with it
	public void randomUnion(int x, int y) {
		PathCell cellA = mazeConnections[y][x];
		if (cellA.unlinked.size() == 0)
			return;
		int dir = cellA.unlinked.get(0);
		PathCell cellB;
		cellB = getNeighborCell(x, y, dir);
		cellA.semiLink(dir);
		cellB.semiLink(flipDirections(dir));
		if (find(cellA) == find(cellB)) {
			if(!limitedExtraPaths) {
				cellA.linked.remove(cellA.linked.size()-1);
				cellB.linked.remove(cellB.linked.size()-1);
			}
			randomUnion(x, y);
		} else
			union(cellA, cellB);
	}

	//find the neighbor of a cell in the given direction
	//0: North   1: East    2: South    3: West
	private PathCell getNeighborCell(int x, int y, int dir) {
		PathCell cellB;
		switch (dir) {
		case 0:
			cellB = mazeConnections[y - 1][x];
			break;
		case 1:
			cellB = mazeConnections[y][x + 1];
			break;
		case 2:
			cellB = mazeConnections[y + 1][x];
			break;
		default:
			cellB = mazeConnections[y][x - 1];
			break;
		}
		return cellB;
	}

	//flip the given direction
	//0: North  <->  2: South || 1: East  <->  3: West 
	private int flipDirections(int dir) {
		return (dir + 2) % 4;
	}

}
