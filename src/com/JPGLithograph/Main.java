package com.JPGLithograph;

import javax.imageio.ImageIO;
import javax.vecmath.Vector3d;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;

class Pixel{

    public int x, y;
    public float v;

    public Pixel(float v, int x, int y){
        this.v = v;
        this.x = x;
        this.y = y;
    }
}

public class Main {

    static Pixel[][] colors;

    static Vector3d[] verts, temp;

    static boolean[] f;

    static BufferedImage image;

    static float maxDepth, minDepth, frameHeight, frameWidth, pixelHeight, pixelWidth, borderWidth;

    static int currentIndex = 0, tem;

    public static String path = new File("main.java").getParent()+"";

    static File outStlFile = new File(path+"out.stl");

    static FileOutputStream outStl;

    static BufferedReader settings;

    static {
        try {
            outStl = new FileOutputStream(outStlFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        try{
            f = new boolean[4];
            path = new File(".").getCanonicalPath()+"\\";
            try {
                settings = new BufferedReader(new FileReader(path + "settings.txt"));String line = settings.readLine();
                String[] setting;
                int eq;
                while(true){
                    while(true){
                        eq = 0;
                        setting = new String[]{"", ""};
                        for(int i = 0; i < line.length(); i++){
                            if(line.charAt(i) == '=')
                                eq = 1;
                            else if (line.charAt(i) == '#')
                                eq = 2;
                            if(eq == 0 && line.charAt(i) != ' ')
                                setting[0] += line.charAt(i);
                            else if(eq == 1 && line.charAt(i) != '=' && line.charAt(i) != ' ')
                                setting[1] += line.charAt(i);
                        }
                        if(eq == 0)
                            setting[0] = "";
                        else if(!setting[1].equals(""))
                            break;
                        else{
                            line = settings.readLine();
                        }
                    }
                    switch(setting[0].toLowerCase()){
                        case "mindepth":
                            minDepth = Float.parseFloat(setting[1]);
                            break;
                        case "maxdepth":
                            maxDepth = Float.parseFloat(setting[1]);
                            break;
                        case "frameheight":
                            frameHeight = Float.parseFloat(setting[1])*10;
                            break;
                        case "framewidth":
                            frameWidth = Float.parseFloat(setting[1])*10;
                            break;
                        case "borderwidth":
                            borderWidth = Float.parseFloat(setting[1])*10;
                            break;
                    }
                    line = settings.readLine();
                    if(line == null)
                        break;
                }
            }catch(FileNotFoundException exc){
                System.out.println(exc.toString());
                System.exit(-1);
            }
            File input;
            try{
                input = new File(path+"in.jpg");
                image = ImageIO.read(input);
            }catch(NullPointerException ex){
                System.out.println(ex.toString());
                System.exit(-1);
            }
            colors = new Pixel[image.getWidth()][image.getHeight()];
            pixelHeight = frameHeight/image.getHeight();
            pixelWidth = frameWidth/image.getWidth();
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    colors[y][x] = new Pixel(map(maxDepth, minDepth, 0, 255, (float) average(new Color(image.getRGB(x, y)))), x, y);
                }
            }
            RandomAccessFile raf = new RandomAccessFile( path+"out.stl", "rw" );
            raf.setLength(0L);
            FileChannel ch = raf.getChannel();

            String title = "title";

            ByteBuffer bb = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);

            byte titleByte[] = new byte[ 80 ];
            System.arraycopy( title.getBytes(), 0, titleByte, 0, title.length() );
            bb.put( titleByte );
            verts = new Vector3d[100000000];


            // gen tris for pixels
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    temp = generateFacade(new Vector3d(borderWidth+(x*pixelWidth), borderWidth+(y*pixelHeight), colors[x][y].v), new Vector3d(borderWidth+((x+1)*pixelWidth), borderWidth+((y+1)*pixelHeight), colors[x][y].v));
                    for(int i = 0; i < 6; i++){
                        verts[currentIndex] = temp[i];
                        currentIndex++;
                    }


                    if(validIndex(x,y+1) && Float.compare(colors[x][y].v, colors[x][y+1].v) < 0){ // down (relative to the picture)
                        temp = generateFacade(new Vector3d(borderWidth+(x*pixelWidth), borderWidth+((y+1)*pixelHeight), colors[x][y].v), new Vector3d(borderWidth+((x+1)*pixelWidth), borderWidth+((y+1)*pixelHeight), colors[x][y+1].v));
                        for(int i = 0; i < 6; i++){
                            verts[currentIndex] = temp[i];
                            currentIndex++;
                        }
                    }
                    if(!validIndex(x, y+1) && Float.compare(colors[x][y].v, maxDepth) < 0){
                        temp = generateFacade(new Vector3d(borderWidth+(x*pixelWidth), borderWidth+((y+1)*pixelHeight), colors[x][y].v), new Vector3d(borderWidth+((x+1)*pixelWidth), borderWidth+((y+1)*pixelHeight), maxDepth));
                        for(int i = 0; i < 6; i++){
                            verts[currentIndex] = temp[i];
                            currentIndex++;
                        }
                    }


                    if(validIndex(x,y-1) && Float.compare(colors[x][y].v, colors[x][y-1].v) < 0){ // up (relative to the picture)
                        temp = generateFacade(new Vector3d(borderWidth+(x*pixelWidth), borderWidth+((y)*pixelHeight), colors[x][y].v), new Vector3d(borderWidth+((x+1)*pixelWidth), borderWidth+((y)*pixelHeight), colors[x][y-1].v));
                        for(int i = 0; i < 6; i++){
                            verts[currentIndex] = temp[i];
                            currentIndex++;
                        }
                    }
                    if(!validIndex(x,y-1) && Float.compare(colors[x][y].v, maxDepth) < 0){
                        temp = generateFacade(new Vector3d(borderWidth+(x*pixelWidth), borderWidth+((y)*pixelHeight), colors[x][y].v), new Vector3d(borderWidth+((x+1)*pixelWidth), borderWidth+((y)*pixelHeight), maxDepth));
                        for(int i = 0; i < 6; i++){
                            verts[currentIndex] = temp[i];
                            currentIndex++;
                        }
                    }


                    if(validIndex(x+1, y) && Float.compare(colors[x][y].v, colors[x+1][y].v) < 0){ // right
                        temp = generateFacade(new Vector3d(borderWidth+((x+1)*pixelWidth), borderWidth+((y)*pixelHeight), colors[x][y].v), new Vector3d(borderWidth+((x+1)*pixelWidth), borderWidth+((y+1)*pixelHeight), colors[x+1][y].v));
                        for(int i = 0; i < 6; i++){
                            verts[currentIndex] = temp[i];
                            currentIndex++;
                        }
                    }if(!validIndex(x+1, y) && Float.compare(colors[x][y].v, maxDepth) < 0){ // right
                        temp = generateFacade(new Vector3d(borderWidth+((x+1)*pixelWidth), borderWidth+((y)*pixelHeight), colors[x][y].v), new Vector3d(borderWidth+((x+1)*pixelWidth), borderWidth+((y+1)*pixelHeight), maxDepth));
                        for(int i = 0; i < 6; i++){
                            verts[currentIndex] = temp[i];
                            currentIndex++;
                        }
                    }


                    if(validIndex(x-1, y) && Float.compare(colors[x][y].v, colors[x-1][y].v) < 0){ // left
                        temp = generateFacade(new Vector3d(borderWidth+((x)*pixelWidth), borderWidth+((y)*pixelHeight), colors[x][y].v), new Vector3d(borderWidth+((x)*pixelWidth), borderWidth+((y+1)*pixelHeight), colors[x-1][y].v));
                        for(int i = 0; i < 6; i++){
                            verts[currentIndex] = temp[i];
                            currentIndex++;
                        }
                    } if(!validIndex(x-1, y) && Float.compare(colors[x][y].v, maxDepth) < 0){
                        temp = generateFacade(new Vector3d(borderWidth+((x)*pixelWidth), borderWidth+((y)*pixelHeight), colors[x][y].v), new Vector3d(borderWidth+((x)*pixelWidth), borderWidth+((y+1)*pixelHeight), maxDepth));
                        for(int i = 0; i < 6; i++){
                            verts[currentIndex] = temp[i];
                            currentIndex++;
                        }
                    }
                    /**/
                }
            }
            temp = generateFacade(new Vector3d(0, 0, 0), new Vector3d(frameWidth+borderWidth*2, frameHeight+borderWidth*2, 0));
            for(int i = 0; i < 6; i++){
                verts[currentIndex] = temp[i];
                currentIndex++;
            }

            temp = generateFacade(new Vector3d(0, 0, 0), new Vector3d(0, frameHeight+borderWidth*2, maxDepth));
            for(int i = 0; i < 6; i++){
                verts[currentIndex] = temp[i];
                currentIndex++;
            }
            temp = generateFacade(new Vector3d(0, 0, 0), new Vector3d(frameWidth+borderWidth*2, 0, maxDepth));
            for(int i = 0; i < 6; i++){
                verts[currentIndex] = temp[i];
                currentIndex++;
            }
            temp = generateFacade(new Vector3d(frameWidth+borderWidth*2, frameHeight+borderWidth*2, 0), new Vector3d(frameWidth+borderWidth*2, 0, maxDepth));
            for(int i = 0; i < 6; i++){
                verts[currentIndex] = temp[i];
                currentIndex++;
            }
            temp = generateFacade(new Vector3d(frameWidth+borderWidth*2, frameHeight+borderWidth*2, 0), new Vector3d(0, frameHeight+borderWidth*2, maxDepth));
            for(int i = 0; i < 6; i++){
                verts[currentIndex] = temp[i];
                currentIndex++;
            }

            temp = generateFacade(new Vector3d(0, 0, maxDepth), new Vector3d(borderWidth, frameHeight+borderWidth*2, maxDepth));
            for(int i = 0; i < 6; i++){
                verts[currentIndex] = temp[i];
                currentIndex++;
            }
            temp = generateFacade(new Vector3d(frameWidth+borderWidth, 0, maxDepth), new Vector3d(frameWidth+borderWidth*2, frameHeight+borderWidth*2, maxDepth));
            for(int i = 0; i < 6; i++){
                verts[currentIndex] = temp[i];
                currentIndex++;
            }
            temp = generateFacade(new Vector3d(borderWidth, 0, maxDepth), new Vector3d(frameWidth+borderWidth, borderWidth, maxDepth));
            for(int i = 0; i < 6; i++){
                verts[currentIndex] = temp[i];
                currentIndex++;
            }
            System.out.println(frameHeight+borderWidth);
            temp = generateFacade(new Vector3d(borderWidth, frameWidth+borderWidth, maxDepth), new Vector3d(frameWidth+borderWidth, frameWidth+borderWidth*2, maxDepth));
            for(int i = 0; i < 6; i++){
                verts[currentIndex] = temp[i];
                currentIndex++;
            }

            bb.putInt(currentIndex/3);    // num of tris
            bb.flip();                  // prep for writing
            ch.write( bb );
            boolean toggle = true;
            Vector3d norm;
            for(int i = 0; i < currentIndex; i+=3){
                toggle = !toggle;
                bb.clear();
                norm = getNormal(verts[i], verts[i+1], verts[i+2]);
                if(toggle)
                    norm = multiply(norm, -1);


                bb.putFloat((float) norm.getX());
                bb.putFloat((float) norm.getY());
                bb.putFloat((float) norm.getZ());

                for(int j = 0; j < 3; j++){
                    bb.putFloat((float) verts[i+j].getX());
                    bb.putFloat((float) verts[i+j].getY());
                    bb.putFloat((float) verts[i+j].getZ());
                }
                bb.putShort((short) 0);
                bb.flip();
                ch.write(bb);
            }
            ch.close();
        } catch (IOException fileNotFoundException) {
            fileNotFoundException.printStackTrace();
        }
    }

    /*
    *
    * parameters:
    *   min1 max1 what you want to map n to.
    *   min2 max2 the bounds that n is currently in.
    *
    * returns:
    *   n maped to min1 max1
    *
    * */
    static float map(float min1, float max1, float min2, float max2, float n){
        return min1+((max1-min1)*((n-min2)/(max2-min2)));
    }


    static Vector3d[] generateFacade(Vector3d topLeft, Vector3d bottomRight){
        Vector3d[] localVerts = new Vector3d[6];
        Vector3d avg = div(add(topLeft, bottomRight), 2);
        Vector3d dif = abs(subtract(topLeft, bottomRight));
        if(dif.z < dif.x && dif.z < dif.y){ // z is least different
            localVerts[0] = new Vector3d(topLeft.x, topLeft.y, topLeft.z);
            localVerts[1] = new Vector3d(bottomRight.x, topLeft.y, avg.z);
            localVerts[2] = new Vector3d(topLeft.x, bottomRight.y, avg.z);

            localVerts[3] = new Vector3d(bottomRight.x, bottomRight.y, bottomRight.z);
            localVerts[4] = new Vector3d(bottomRight.x, topLeft.y, avg.z);
            localVerts[5] = new Vector3d(topLeft.x, bottomRight.y, avg.z);
        }else if(dif.x < dif.z && dif.x < dif.y){ // x is least different
            localVerts[0] = new Vector3d(topLeft.x, topLeft.y, topLeft.z);
            localVerts[1] = new Vector3d(avg.x, bottomRight.y, topLeft.z);
            localVerts[2] = new Vector3d(avg.x, topLeft.y, bottomRight.z);

            localVerts[3] = new Vector3d(bottomRight.x, bottomRight.y, bottomRight.z);
            localVerts[4] = new Vector3d(avg.x, bottomRight.y, topLeft.z);
            localVerts[5] = new Vector3d(avg.x, topLeft.y, bottomRight.z);
        }else if(dif.y < dif.z && dif.y < dif.x){ // y is least different
            localVerts[0] = new Vector3d(topLeft.x, topLeft.y, topLeft.z);
            localVerts[1] = new Vector3d(bottomRight.x, avg.y, topLeft.z);
            localVerts[2] = new Vector3d(topLeft.x, avg.y, bottomRight.z);

            localVerts[3] = new Vector3d(bottomRight.x, bottomRight.y, bottomRight.z);
            localVerts[4] = new Vector3d(bottomRight.x, avg.y, topLeft.z);
            localVerts[5] = new Vector3d(topLeft.x, avg.y, bottomRight.z);
        }else{
            System.out.println(dif.x);
            System.out.println(dif.y);
            System.out.println(dif.z);
            System.out.println("You fucked something up ");
        }
        return localVerts;
    }


    static int average(Color c){ return (c.getRed()+c.getGreen()+c.getBlue())/3; }

    static boolean validIndex(int x, int y){
        return 0 <= x && x < image.getWidth() && 0 <= y && y < image.getHeight();
    }

    static Vector3d getNormal(Vector3d v1, Vector3d v2, Vector3d v3){
        Vector3d A = subtract(copy(v2), copy(v1));
        Vector3d B = subtract(copy(v3), copy(v1));
        Vector3d r = new Vector3d(A.getY()*B.getZ()-A.getZ()*B.getY(), A.getZ()*B.getX()-A.getX()*B.getZ(),   A.getX()*B.getY()-A.getY()*B.getX());
        r.normalize();
        return r;
    }

    static Vector3d copy(Vector3d v){ return new Vector3d(v.getX(), v.getY(),v.getZ()); }

    static Vector3d subtract(Vector3d v1, Vector3d v2){ return new Vector3d(v1.x-v2.x, v1.y-v2.y, v1.z-v2.z); }

    static Vector3d multiply(Vector3d v1, Vector3d v2){ return new Vector3d(v1.x*v2.x, v1.y*v2.y, v1.z*v2.z); }

    static Vector3d multiply(Vector3d v1, int v2){ return new Vector3d(v1.x*v2, v1.y*v2, v1.z*v2); }

    static Vector3d add(Vector3d v1, Vector3d v2){ return new Vector3d(v1.x+v2.x, v1.y+v2.y, v1.z+v2.z); }

    static Vector3d div(Vector3d v1, Vector3d v2){ return new Vector3d(v1.x/v2.x, v1.y/v2.y, v1.z/v2.z); }

    static Vector3d div(Vector3d v1, int v2){ return new Vector3d(v1.x/v2, v1.y/v2, v1.z/v2); }

    static Vector3d abs(Vector3d v){ return new Vector3d(Math.abs(v.x), Math.abs(v.y), Math.abs(v.z)); }
}
