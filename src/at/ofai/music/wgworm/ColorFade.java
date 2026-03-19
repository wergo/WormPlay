package at.ofai.music.wgworm;

import java.awt.Color;

public class ColorFade {

    public static Color[] fadeRed(int mapLength, double factor) {
        float comp;
        Color[] colorMap = new Color[mapLength];
        for (int j = 0; j < mapLength; j++) {
            comp = (float) (1 - Math.exp(factor * (double) j));
            // System.out.println("CompRed: " + comp);
            colorMap[j] = new Color(1.0f, comp, comp);
        }
        return colorMap;
    }

    public static Color[] fadeBlack(int mapLength, double factor) {
        float comp;
        Color[] colorMap = new Color[mapLength];
        for (int j = 0; j < mapLength; j++) {
            comp = (float) (1 - Math.exp(factor * (double) j));
            // System.out.println("CompBlack: " + comp);
            colorMap[j] = new Color(comp, comp, comp);
        }
        return colorMap;
    }
}
