package com.mine.autolight;

import java.util.Map;
import java.util.TreeMap;

public class BrightnessAlgorithm {

    public static int calculateBrightness(float ambientLux) {
        TreeMap<Float, Integer> sortedPoints = new TreeMap<>();
        
        for (Map.Entry<Integer, Integer> entry : MySettings.points.entrySet()) {
            sortedPoints.put(entry.getKey().floatValue(), entry.getValue());
        }

        if (sortedPoints.size() < 2) return 125;

        // Clamp values to your defined range
        if (ambientLux <= sortedPoints.firstKey()) return sortedPoints.firstEntry().getValue();
        if (ambientLux >= sortedPoints.lastKey()) return sortedPoints.lastEntry().getValue();

        Map.Entry<Float, Integer> lower = sortedPoints.floorEntry(ambientLux);
        Map.Entry<Float, Integer> upper = sortedPoints.ceilingEntry(ambientLux);

        return interpolateLogarithmically(lower.getKey(), lower.getValue(), upper.getKey(), upper.getValue(), ambientLux);
    }

    private static int interpolateLogarithmically(float x1, int y1, float x2, int y2, float currentX) {
        if (x1 == x2) return y1;
        double logX = Math.log(currentX);
        double logX1 = Math.log(x1);
        double logX2 = Math.log(x2);
        double fraction = (logX - logX1) / (logX2 - logX1);
        return (int) Math.round(y1 + (y2 - y1) * fraction);
    }
}
