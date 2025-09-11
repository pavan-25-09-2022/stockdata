package com.stocks.repository;

import com.stocks.dto.StrikeTO;
import com.stocks.dto.TradeSetupTO;
import com.stocks.entity.StrikeSetupEntity;
import com.stocks.entity.TradeSetupEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class TradeSetupManager {
	@PersistenceContext
	private EntityManager entityManager;

	@Transactional
	public void saveTradeSetup(TradeSetupTO tradeSetupTO) {
		if (tradeSetupTO != null) {
			TradeSetupTO tradeSetupTO1 = getStockByDateAndTime(tradeSetupTO.getStockSymbol(), tradeSetupTO.getStockDate(), tradeSetupTO.getFetchTime());
			if (tradeSetupTO1 != null) {
				return;
			}
			entityManager.persist(mapTradeSetup(tradeSetupTO));
		}

	}

	@Transactional
	public void saveTradeSetups(List<TradeSetupTO> tradeSetupTOList) {
		if (tradeSetupTOList == null || tradeSetupTOList.isEmpty()) {
			return;
		}
		for (TradeSetupTO tradeSetupTO : tradeSetupTOList) {
			TradeSetupTO tradeSetupTO1 = getStockByDateAndTime(tradeSetupTO.getStockSymbol(), tradeSetupTO.getStockDate(), tradeSetupTO.getFetchTime());
			if (tradeSetupTO1 != null) {
				return;
			}
			TradeSetupEntity tradeSetupEntity = mapTradeSetup(tradeSetupTO);
			List<StrikeSetupEntity> strikes = mapStrikes(tradeSetupTO);
			saveTradeWithStrikes(tradeSetupEntity, strikes);
		}
	}

	public void saveTradeWithStrikes(TradeSetupEntity tradeSetup, List<StrikeSetupEntity> strikes) {
		for (StrikeSetupEntity strike : strikes) {
			strike.setTradeSetup(tradeSetup);
		}
		tradeSetup.setStrikeSetups(strikes);
		entityManager.persist(tradeSetup);
	}

	@Transactional(readOnly = true)
	public TradeSetupEntity findTradeSetupByStockAndStrategyAndDate(String stockSymbol, String strategy, String stockDate) {
		String hql = "FROM TradeSetupEntity WHERE stockSymbol = :stockSymbol AND strategy = :strategy AND stockDate = :stockDate";
		return entityManager.createQuery(hql, TradeSetupEntity.class)
				.setParameter("stockSymbol", stockSymbol)
				.setParameter("strategy", strategy)
				.setParameter("stockDate", stockDate)
				.getResultStream().findFirst().orElse(null);
	}

	@Transactional(readOnly = true)
	public List<TradeSetupEntity> findAllTradeSetups() {
		String hql = "FROM TradeSetupEntity";
		return entityManager.createQuery(hql, TradeSetupEntity.class)
				.getResultList()
				.stream()
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<TradeSetupEntity> findAllByStockDate(String stockDate) {
		String hql = "FROM TradeSetupEntity WHERE stockDate = :stockDate";
		return entityManager.createQuery(hql, TradeSetupEntity.class)
				.setParameter("stockDate", stockDate)
				.getResultList();
	}

	@Transactional(readOnly = true)
	public List<TradeSetupTO> findTradeSetupByDate(String stockDate) {
		String hql = "FROM TradeSetupEntity WHERE stockDate = :stockDate";
		List<TradeSetupTO> list = entityManager.createQuery(hql, TradeSetupEntity.class)
				.setParameter("stockDate", stockDate)
				.getResultStream().map(this::mapEntityToTO)
				.collect(Collectors.toList());
		return list;
	}

	@Transactional(readOnly = true)
	public TradeSetupTO getStockByDateAndTime(String stock, String stockDate, String startTime) {
		String hql = "FROM TradeSetupEntity WHERE stockSymbol = :stock AND stockDate = :stockDate AND fetchTime = :startTime";
		return entityManager.createQuery(hql, TradeSetupEntity.class)
				.setParameter("stock", stock)
				.setParameter("stockDate", stockDate)
				.setParameter("startTime", startTime)
				.getResultStream().map(this::mapEntityToTO).findFirst().orElse(null);
	}

	@Transactional
	public void deleteTradeSetupsByStockDate(String stockDate) {
		// Delete related StrikeSetupEntity records first
		String deleteStrikesHql = "DELETE FROM StrikeSetupEntity s WHERE s.tradeSetup.id IN " +
				"(SELECT t.id FROM TradeSetupEntity t WHERE t.stockDate = :stockDate)";
		entityManager.createQuery(deleteStrikesHql)
				.setParameter("stockDate", stockDate)
				.executeUpdate();

		// Delete TradeSetupEntity records
		String deleteTradeSetupsHql = "DELETE FROM TradeSetupEntity t WHERE t.stockDate = :stockDate";
		entityManager.createQuery(deleteTradeSetupsHql)
				.setParameter("stockDate", stockDate)
				.executeUpdate();
	}

	@Transactional
	public void deleteTradeSetupsByStockDateAndName(String stockDate, String stockName) {
		// Delete related StrikeSetupEntity records first
		String deleteStrikesHql = "DELETE FROM StrikeSetupEntity s WHERE s.tradeSetup.id IN " +
				"(SELECT t.id FROM TradeSetupEntity t WHERE t.stockDate = :stockDate AND t.stockSymbol = :stockName)";
		entityManager.createQuery(deleteStrikesHql)
				.setParameter("stockDate", stockDate)
				.setParameter("stockName", stockName)
				.executeUpdate();

		// Delete TradeSetupEntity records
		String deleteTradeSetupsHql = "DELETE FROM TradeSetupEntity t WHERE t.stockDate = :stockDate AND t.stockSymbol = :stockName";
		entityManager.createQuery(deleteTradeSetupsHql)
				.setParameter("stockDate", stockDate)
				.setParameter("stockName", stockName)
				.executeUpdate();
	}

	private TradeSetupTO mapEntityToTO(TradeSetupEntity entity) {
		if (entity == null) return null;
		TradeSetupTO to = new TradeSetupTO();
		to.setStockSymbol(entity.getStockSymbol());
		to.setStockDate(entity.getStockDate());
		to.setFetchTime(entity.getFetchTime());
		to.setOiChgPer(entity.getOiChgPer());
		to.setLtpChgPer(entity.getLtpChgPer());
		to.setEntry1(entity.getEntry1());
		to.setEntry2(entity.getEntry2());
		to.setTarget1(entity.getTarget1());
		to.setTarget2(entity.getTarget2());
		to.setStopLoss1(entity.getStopLoss1());
		
		to.setStopLoss2(entity.getStopLoss2());
		to.setStatus(entity.getStatus());
		to.setTradeNotes(entity.getTradeNotes());
		to.setStrategy(entity.getStrategy());
		to.setType(entity.getType());
		return to;
	}

	private TradeSetupEntity mapTradeSetup(TradeSetupTO tradeSetupTO) {
		TradeSetupEntity entity = new TradeSetupEntity();
		entity.setStockSymbol(tradeSetupTO.getStockSymbol());
		entity.setStockDate(tradeSetupTO.getStockDate());
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

	private List<StrikeSetupEntity> mapStrikes(TradeSetupTO tradeSetupTO) {
		return tradeSetupTO.getStrikes().entrySet().stream()
				.filter(e -> e.getKey() >= -3 && e.getKey() <= 3)
				.map(e -> {
					StrikeTO strike = e.getValue();
					StrikeSetupEntity entity = new StrikeSetupEntity();
					entity.setCurPrice(strike.getCurPrice());
					entity.setStrikePrice(strike.getStrikePrice());
					entity.setCeOi(strike.getCeOi());
					entity.setCeOiChg(strike.getCeOiChg());
					entity.setCeLtpChg(strike.getCeLtpChg());
					entity.setCeOiInt(strike.getCeOiInt());
					entity.setCeVolume(strike.getCeVolume());
					entity.setCeIv(strike.getCeIv());
					entity.setCeIvChg(strike.getCeIvChg());
					entity.setPeOi(strike.getPeOi());
					entity.setPeOiChg(strike.getPeOiChg());
					entity.setPeOiInt(strike.getPeOiInt());
					entity.setPeVolume(strike.getPeVolume());
					entity.setPeIv(strike.getPeIv());
					entity.setPeIvChg(strike.getPeIvChg());
					return entity;
				})
				.collect(Collectors.toList());
	}
}
