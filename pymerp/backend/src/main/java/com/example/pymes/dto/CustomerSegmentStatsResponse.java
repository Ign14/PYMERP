package com.example.pymes.dto;

/**
 * DTO for customer segment statistics.
 */
public class CustomerSegmentStatsResponse {

    private String code;
    private String name;
    private String color;
    private Long total;

    public CustomerSegmentStatsResponse() {
    }

    public CustomerSegmentStatsResponse(String code, String name, String color, Long total) {
        this.code = code;
        this.name = name;
        this.color = color;
        this.total = total;
    }

    // Getters and Setters

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }
}
