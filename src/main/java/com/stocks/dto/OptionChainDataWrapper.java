package com.stocks.dto;

import java.util.List;

public class OptionChainDataWrapper {
    private List<OptionChainData> data;
    private IndiaVixData indiaVixData;
    private UnderLyingAssetData underLyingAssetData;

    public List<OptionChainData> getData() {
        return data;
    }

    public void setData(List<OptionChainData> data) {
        this.data = data;
    }

    public IndiaVixData getIndiaVixData() {
        return indiaVixData;
    }

    public void setIndiaVixData(IndiaVixData indiaVixData) {
        this.indiaVixData = indiaVixData;
    }

    public UnderLyingAssetData getUnderLyingAssetData() {
        return underLyingAssetData;
    }

    public void setUnderLyingAssetData(UnderLyingAssetData underLyingAssetData) {
        this.underLyingAssetData = underLyingAssetData;
    }
}