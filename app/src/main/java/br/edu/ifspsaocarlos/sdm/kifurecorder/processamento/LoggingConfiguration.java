package br.edu.ifspsaocarlos.sdm.kifurecorder.processamento;

import java.util.HashSet;
import java.util.Set;

public class LoggingConfiguration {

    // Images
    public static int RAW_CAMERA_IMAGE = 1;
    public static int CAMERA_IMAGE_WITH_BOARD_CONTOUR = 2;
    public static int ORTOGONAL_BOARD_IMAGE = 3;
    public static int CORNER_REGIONS_IMAGES = 4;

    // Processing objects
    public static int CORNER_POSITIONS = 5;
    public static int STONE_DETECTION_INFORMATION = 6;

    private static Set<Integer> activatedFlags = new HashSet<>();

    private LoggingConfiguration() {};

    public static void activateLogging(int flag) {
        activatedFlags.add(flag);
    }

    public static void deactivateLogging(int flag) {
        activatedFlags.remove(flag);
    }

    public static boolean shouldLog(int flag) {
        return activatedFlags.contains(flag);
    }

}
