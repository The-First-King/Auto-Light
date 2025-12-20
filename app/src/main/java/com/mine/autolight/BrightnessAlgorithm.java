package com.mine.autolight;

import java.util.Map;
import java.util.TreeMap;

/**
 * This class calculates the target brightness by connecting the user's
 * defined points in MySettings using a logarithmic curve.
 */
public class BrightnessAlgorithm {

    public static int calculateBrightness(float ambientLux) {
        // 1. Initialize a TreeMap to automatically sort points by Lux (lowest to highest)
        TreeMap<Float, Integer> sortedPoints = new TreeMap<>();
        
        // 2. Safely import points from your MySettings file
        if (MySettings.points != null) {
            for (Map.Entry<Integer, Integer> entry : MySettings.points.entrySet()) {
                sortedPoints.put(entry.getKey().floatValue(), entry.getValue());
            }
        }

        // 3. BUILD SAFETY CHECK: Ensure we have enough data to calculate a curve
        if (sortedPoints.size() < 2) {
            // If the settings are empty or only have one point, return a safe default
            return sortedPoints.isEmpty() ? 125 : sortedPoints.firstEntry().getValue();
        }

        // 4. EDGE CASE: Lux is below your lowest defined point
        if (ambientLux <= sortedPoints.firstKey()) {
            return sortedPoints.firstEntry().getValue();
        }

        // 5. EDGE CASE: Lux is above your highest defined point
        if (ambientLux >= sortedPoints.lastKey()) {
            return sortedPoints.lastEntry().getValue();
        }

        // 6. FIND THE SEGMENT: Identify the points exactly below and above the current lux
        Map.Entry<Float, Integer> lower = sortedPoints.floorEntry(ambientLux);
        Map.Entry<Float, Integer> upper = sortedPoints.ceilingEntry(ambientLux);

        // 7. Calculate the brightness using Logarithmic Interpolation
        return interpolateLogarithmically(
                lower.getKey(), lower.getValue(), 
                upper.getKey(), upper.getValue(), 
                ambientLux
        );
    }

    /**
     * Connects two points (x1,y1) and (x2,y2) using a logarithmic curve.
     * This mimics human eye perception for smoother transitions.
     */
    private static int interpolateLogarithmically(float x1, int y1, float x2, int y2, float currentX) {
        // Safety check to avoid division by zero
        if (x1 == x2) return y1;

        // Logarithmic Math: y = y1 + (y2 - y1) * [ log(currentX) - log(x1) ] / [ log(x2) - log(x1) ]
        double logX = Math.log(currentX);
        double logX1 = Math.log(x1);
        double logX2 = Math.log(x2);

        double fraction = (logX - logX1) / (logX2 - logX1);
        double result = y1 + (y2 - y1) * fraction;

        // Round and clamp between the standard Android range (0-255)
        return Math.max(0, Math.min(255, (int) Math.round(result)));
    }
}
