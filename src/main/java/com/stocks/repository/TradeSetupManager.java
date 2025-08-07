package com.stocks.repository;

import com.stocks.dto.TradeSetupTO;
import com.stocks.entity.TradeSetupEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Repository
public class TradeSetupManager {
	@PersistenceContext
	private EntityManager entityManager;

	@Transactional
	public void saveTradeSetup(TradeSetupTO tradeSetupTO) {
		entityManager.persist(mapTradeSetup(tradeSetupTO));
	}

	@Transactional
	public void saveTradeSetups(List<TradeSetupTO> tradeSetupTOList) {
		if (tradeSetupTOList == null || tradeSetupTOList.isEmpty()) {
			return;
		}
		for (TradeSetupTO tradeSetupTO : tradeSetupTOList) {
			entityManager.persist(mapTradeSetup(tradeSetupTO));
		}
	}

	private TradeSetupEntity mapTradeSetup(TradeSetupTO tradeSetupTO) {
		TradeSetupEntity entity = new TradeSetupEntity();
		entity.setStockSymbol(tradeSetupTO.getStockSymbol());
		entity.setDate(tradeSetupTO.getDate());
		entity.setFetchTime(tradeSetupTO.getFetchTime());
		entity.setOiChgPer(tradeSetupTO.getOiChgPer());
		entity.setLtpChgPer(tradeSetupTO.getLtpChgPer());
		entity.setEntry1(tradeSetupTO.getEntry1());
		entity.setEntry2(tradeSetupTO.getEntry2());
		entity.setTarget1(tradeSetupTO.getTarget1());
		entity.setTarget2(tradeSetupTO.getTarget2());
		entity.setStopLoss1(tradeSetupTO.getStopLoss1());
		entity.setStopLoss2(tradeSetupTO.getStopLoss2());
		entity.setStatus(tradeSetupTO.getStatus());
		entity.setTradeNotes(tradeSetupTO.getTradeNotes());
		entity.setStrategy(tradeSetupTO.getStrategy());
		entity.setType(tradeSetupTO.getType());
		return entity;
	}
}
