package com.example.hotel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * PriceCalculator用のプロパティ設定クラス
 */
@Component
@ConfigurationProperties(prefix = "price")
public class PriceProperties {

  private int basePerPerson;
  private List<Double> capacityMultipliers;
  private double hotelPriceBaseMultiplier;
  private int hotelVariationCount;
  private double hotelVariationStep;
  private double hotelBaseOffset;
  private int demandVariationCycle;
  private double demandVariationStep;
  private double demandBaseFactor;

  // Getters and Setters
  public int getBasePerPerson() {
    return basePerPerson;
  }

  public void setBasePerPerson(int basePerPerson) {
    this.basePerPerson = basePerPerson;
  }

  public List<Double> getCapacityMultipliers() {
    return capacityMultipliers;
  }

  public void setCapacityMultipliers(List<Double> capacityMultipliers) {
    this.capacityMultipliers = capacityMultipliers;
  }

  public double getHotelPriceBaseMultiplier() {
    return hotelPriceBaseMultiplier;
  }

  public void setHotelPriceBaseMultiplier(double hotelPriceBaseMultiplier) {
    this.hotelPriceBaseMultiplier = hotelPriceBaseMultiplier;
  }

  public int getHotelVariationCount() {
    return hotelVariationCount;
  }

  public void setHotelVariationCount(int hotelVariationCount) {
    this.hotelVariationCount = hotelVariationCount;
  }

  public double getHotelVariationStep() {
    return hotelVariationStep;
  }

  public void setHotelVariationStep(double hotelVariationStep) {
    this.hotelVariationStep = hotelVariationStep;
  }

  public double getHotelBaseOffset() {
    return hotelBaseOffset;
  }

  public void setHotelBaseOffset(double hotelBaseOffset) {
    this.hotelBaseOffset = hotelBaseOffset;
  }

  public int getDemandVariationCycle() {
    return demandVariationCycle;
  }

  public void setDemandVariationCycle(int demandVariationCycle) {
    this.demandVariationCycle = demandVariationCycle;
  }

  public double getDemandVariationStep() {
    return demandVariationStep;
  }

  public void setDemandVariationStep(double demandVariationStep) {
    this.demandVariationStep = demandVariationStep;
  }

  public double getDemandBaseFactor() {
    return demandBaseFactor;
  }

  public void setDemandBaseFactor(double demandBaseFactor) {
    this.demandBaseFactor = demandBaseFactor;
  }
}
