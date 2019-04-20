package cn.huww98.cv.graphcut;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class Main {
    public static void main(String [] args) throws IOException {
        int resize = 2;

        BufferedImage img = ImageIO.read(new File("raw"+resize+".jpg"));
        int[] rgb = img.getRGB(0,0,img.getWidth(), img.getHeight(), null, 0,img.getWidth());

        byte[] input = new byte[rgb.length * GraphCut.channel];
        for (int i = 0; i < rgb.length; i++) {
            input[i * GraphCut.channel + GraphCut.rOffset] = (byte)((rgb[i] >> 16) & 0xFF);
            input[i * GraphCut.channel + GraphCut.gOffset] = (byte)((rgb[i] >> 8) & 0xFF);
            input[i * GraphCut.channel + GraphCut.bOffset] = (byte)(rgb[i] & 0xFF);
        }


        ArrayList<Rectangle> foreground = new ArrayList<>();
        foreground.add(new Rectangle(400, 349, 430, 500));
        ArrayList<Rectangle> background = new ArrayList<>();
        background.add(new Rectangle(0,0, 100, 875));
        background.add(new Rectangle(345, 52, 355, 50));

        GraphCutClasses[] mask = new GraphCutClasses[rgb.length];
        int[] inputRgb = Arrays.copyOf(rgb, rgb.length);
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int i = y * img.getWidth() + x;
                int finalX = x;
                int finalY = y;
                if (foreground.stream().anyMatch(r->r.contains(finalX * resize , finalY * resize))) {
                    mask[i] = GraphCutClasses.FOREGROUND;
                    inputRgb[i] = 0xFF0000FF;
                } else if (background.stream().anyMatch(r->r.contains(finalX * resize, finalY * resize))) {
                    mask[i] = GraphCutClasses.BACKGROUND;
                    inputRgb[i] = 0xFFFF0000;
                } else {
                    mask[i] = GraphCutClasses.UNKNOWN;
                }
            }
        }

        BufferedImage img2 = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
        img2.setRGB(0,0,img.getWidth(), img.getHeight(), inputRgb, 0,img.getWidth());
        ImageIO.write(img2, "png", new File("input.png"));

        boolean[] out = GraphCut.graphCut(input,mask, img.getWidth());
        int[] outRgb = Arrays.copyOf(rgb, rgb.length);
        for (int i = 0; i < out.length; i++) {
            if (!out[i]) {
                outRgb[i] = 0;
            }
        }

        BufferedImage img3 = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
        img3.setRGB(0,0,img.getWidth(), img.getHeight(), outRgb, 0,img.getWidth());
        ImageIO.write(img3, "png", new File("out.png"));
    }
}
