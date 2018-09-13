package br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.boardDetector;

import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.ORB;
import org.opencv.imgcodecs.Imgcodecs;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BoardDetectorByImageSimilarity implements BoardDetectorInterface {

    private int imageIndex;
    private int state;
    private Mat lastImageWhenBoardWasInside;
    private List<DMatch> matchesList;

    public BoardDetectorByImageSimilarity() {
        lastImageWhenBoardWasInside = null;
    }

    public boolean isBoardContainedIn(Mat ortogonalBoardImage) {
        if (isBoardInsideContourAccordingToImageSimilarity(ortogonalBoardImage)) {
            lastImageWhenBoardWasInside = ortogonalBoardImage;
            return true;
        }
        return false;
    }

    private boolean isBoardInsideContourAccordingToImageSimilarity(Mat ortogonalBoardImage) {
        if (lastImageWhenBoardWasInside == null) return true;
        generateDescriptorMatches(lastImageWhenBoardWasInside, ortogonalBoardImage);
        double SIMILARITY_THRESHOULD = 0.9;
        return calculateSimilarityWithorbFeatureMatching() > SIMILARITY_THRESHOULD;
    }

    private void generateDescriptorMatches(Mat image1, Mat image2) {
        Mat processedImage1 = image1.clone();
        Mat processedImage2 = image2.clone();
//        Imgcodecs.imwrite("processing/difference_between_" + imageIndex + "_1.jpg", processedImage1);
//        Imgcodecs.imwrite("processing/difference_between_" + imageIndex + "_2.jpg", processedImage2);

        FeatureDetector detector = FeatureDetector.create(FeatureDetector.ORB);
        MatOfKeyPoint keypointsA = new MatOfKeyPoint();
        MatOfKeyPoint keypointsB = new MatOfKeyPoint();
        detector.detect(processedImage1, keypointsA);
        detector.detect(processedImage2, keypointsB);

        DescriptorExtractor extractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        Mat descriptorsA = new Mat();
        Mat descriptorsB = new Mat();
        extractor.compute(processedImage1, keypointsA, descriptorsA);
        extractor.compute(processedImage2, keypointsB, descriptorsB);

        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
        MatOfDMatch matches = new MatOfDMatch();
        matcher.match(descriptorsA, descriptorsB, matches);

        matchesList = matches.toList();
        Collections.sort(matchesList, new Comparator<DMatch>() {
            public int compare(DMatch a, DMatch b) {
                if (a.distance < b.distance) return -1;
                else if (a.distance > b.distance) return 1;
                return 0;
            }
        });
    }

    private double calculateSimilarityWithorbFeatureMatching() {
        double averageDistanceOfClosestDescriptors = 0;
        int numberOfDescriptorsToConsider = 15;
        for (int i = 0; i < numberOfDescriptorsToConsider; i++) {
            averageDistanceOfClosestDescriptors += matchesList.get(i).distance;
        }

        double similarity = 1 - (averageDistanceOfClosestDescriptors / numberOfDescriptorsToConsider) / 100;
        System.out.println("Similarity of images by calculating average distance of ORB descriptors: " + similarity);
        return similarity;
    }

    public void setState(int state) {
        this.state = state;
    }

    public void setImageIndex(int imageIndex) {
        this.imageIndex = imageIndex;
    }

}
