package com.bhui.dto;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author JXS
 */
public class SimilarityResultDTO {
    private double similarity;
    private Integer[] resultArray;

    private Integer[] roadOBJECTIDs;


    @Override
    public String toString() {
        return "SimilarityResultDTO{" +
                "similarity=" + similarity +
                ", resultArray=" + Arrays.toString(resultArray) +
                ", roadOBJECTIDs=" + Arrays.toString(roadOBJECTIDs) +
                '}';
    }


    public Integer[] getRoadOBJECTIDs() {
        return roadOBJECTIDs;
    }

    public void setRoadOBJECTIDs(Integer[] roadOBJECTIDs) {
        this.roadOBJECTIDs = roadOBJECTIDs;
    }

    public SimilarityResultDTO(double similarity, Integer[] resultArray, Integer[] roadOBJECTIDs) {
        this.similarity = similarity;
        this.resultArray = resultArray;
        this.roadOBJECTIDs = roadOBJECTIDs;
    }

    // Getter 和 Setter
    public double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(double similarity) {
        this.similarity = similarity;
    }

    public Integer[] getResultArray() {
        return resultArray;
    }

    public void setResultArray(Integer[] resultArray) {
        this.resultArray = resultArray;
    }

}