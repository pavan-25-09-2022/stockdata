package com.stocks.service;

import com.stocks.dto.OptionChainData;
import com.stocks.dto.StockData;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OptionChainDataRepository extends JpaRepository<OptionChainData, Long> {
    List<OptionChainData> findByStockDataId(Long stockDataId);
    List<OptionChainData> findByStockData(StockData stockData);
}