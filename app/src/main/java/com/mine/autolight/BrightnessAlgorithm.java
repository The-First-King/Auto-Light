package com.mine.autolight;

import java.util.Map;
import java.util.TreeMap;

public class BrightnessAlgorithm {

    public static int calculateBrightness(float ambientLux) {
        // Use a TreeMap to sort your MySettings points by Lux value automatically
        TreeMap<Float, Integer> sortedPoints = new TreeMap<>();
        
        // Convert your MySettings HashMap into our sorted TreeMap
        for (Map.Entry<Integer, Integer> entry : MySettings.points.entrySet()) {
            sortedPoints.put(entry.getKey().floatValue(), entry.getValue());
        }

        if (sortedPoints.isEmpty()) return 125; // Safety default

        // Logic for Lux lower than your first point
        if (ambientLux <= sortedPoints.firstKey()) return sortedPoints.firstEntry().getValue();
        // Logic for Lux higher than your last point
        if (ambientLux >= sortedPoints.lastKey()) return sortedPoints.lastEntry().getValue();

        // Find the "Segment" (the point below and the point above current Lux)
        Map.Entry<Float, Integer> lower = sortedPoints.floorEntry(ambientLux);
        Map.Entry<Float, Integer> upper = sortedPoints.ceilingEntry(ambientLux);

        return interpolateLogarithmically(
                lower.getKey(), lower.getValue(), 
                upper.getKey(), upper.getValue(), 
                ambientLux
        );
    }

    private static int interpolateLogarithmically(float x1, int y1, float x2, int y2, float currentX) {
        if (x1 == x2) return y1;

        // Human eye perception formula (Logarithmic scale)
        double logX = Math.log(currentX);
        double logX1 = Math.log(x1);
        double logX2 = Math.log(x2);

        double fraction = (logX - logX1) / (logX2 - logX1);
        double result = y1 + (y2 - y1) * fraction;

        return (int) Math.round(result);
    }
}
