package com.example.classifierapi.dto;

public class TreeNodeDto {
  public boolean isLeaf;
  public String label;
  public String feature;
  public Double threshold;
  public TreeNodeDto left;
  public TreeNodeDto right;
}

