package com.stocks.service;

import com.stocks.dto.StockData;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Repository
public class StockDataManager {

	@PersistenceContext
	private EntityManager entityManager;

	@Transactional
	public void saveStockData(String stock, String date, String time, String oiInterpretation, String type, double limit) {
		StockData stockData = new StockData(stock, type);
		stockData.setDate(date);
		stockData.setTime(time);
		stockData.setOiInterpretation(oiInterpretation);
		stockData.setTrend(String.valueOf(limit));
		entityManager.persist(stockData);
	}

	@Transactional(readOnly = true)
	public StockData getRecord(String stock, String date, String time) {
		String hql = "FROM StockData WHERE stock = :stock AND date = :date AND time = :time";
		Session session = entityManager.unwrap(Session.class);
		return session.createQuery(hql, StockData.class)
				.setParameter("stock", stock)
				.setParameter("date", date)
				.setParameter("time", time)
				.uniqueResult();
	}

	@Transactional(readOnly = true)
	public StockData getRecordBasedOnCriteria(String stock, String date, String criteria) {
		String hql = "FROM StockData WHERE stock = :stock AND date = :date AND criteria = :criteria";
		Session session = entityManager.unwrap(Session.class);
		return session.createQuery(hql, StockData.class)
				.setParameter("stock", stock)
				.setParameter("date", date)
				.setParameter("criteria", criteria)
				.uniqueResult();
	}


	@Transactional(readOnly = true)
	public List<StockData> getStocksByDate(String date) {
		String hql = "FROM StockData WHERE date = :date";
		Session session = entityManager.unwrap(Session.class);
		return session.createQuery(hql, StockData.class)
				.setParameter("date", date)
				.getResultList();
	}

	@Transactional(readOnly = true)
	public String getRecentTime(String stock, String date) {
		String hql = "SELECT MAX(time) FROM StockData WHERE stock = :stock AND date = :date";
		Session session = entityManager.unwrap(Session.class);
		return session.createQuery(hql, String.class)
				.setParameter("stock", stock)
				.setParameter("date", date)
				.uniqueResult();
	}

	@Transactional(readOnly = true)
	public String getRecentTime(String stock, String date, String criteria) {
		String hql = "SELECT MAX(time) FROM StockData WHERE stock = :stock AND date = :date AND criteria = :criteria";
		Session session = entityManager.unwrap(Session.class);
		return session.createQuery(hql, String.class)
				.setParameter("stock", stock)
				.setParameter("date", date)
				.setParameter("criteria", criteria)
				.uniqueResult();
	}

	@Transactional(readOnly = true)
	public StockData getRecentRecord(String stock, String date) {
		String hql = "FROM StockData WHERE stock = :stock AND date = :date AND time = (SELECT MAX(time) FROM StockData WHERE stock = :stock AND date = :date)";
		Session session = entityManager.unwrap(Session.class);
		return session.createQuery(hql, StockData.class)
				.setParameter("stock", stock)
				.setParameter("date", date)
				.uniqueResult();
	}

	public List<StockData> getAllRecord() {
		String hql = "FROM StockData";
		Session session = entityManager.unwrap(Session.class);
		return session.createQuery(hql, StockData.class).getResultList();
	}

	@Transactional
	public void saveStockData(String stock, String date, String time, String oiInterpretation, String type, double entryPrice1, double entryPrice2,
	                          double stopLoss, double targetPrice1, double targetPrice2, double averagePrice, String criteria) {


		StockData stockData = new StockData(stock, type);
		stockData.setDate(date);
		stockData.setTime(time);
		stockData.setOiInterpretation(oiInterpretation);
		// stockData.setTrend(String.format("%.2f", limit));
		stockData.setEntryPrice1(entryPrice1);
		stockData.setEntryPrice2(entryPrice2);
		stockData.setStopLoss(stopLoss);
		stockData.setTargetPrice1(targetPrice1);
		stockData.setTargetPrice2(targetPrice2);
		stockData.setAveragePrice(averagePrice);
		stockData.setCriteria(criteria);
		entityManager.persist(stockData);
	}

	@Transactional
	public void deleteStocksByDate(String date) {
		String hql = "DELETE FROM StockData WHERE date = :date";
		Session session = entityManager.unwrap(Session.class);
		session.createQuery(hql)
				.setParameter("date", date)
				.executeUpdate();
	}

	@Transactional
	public void deleteStocksByDateRange(String fromDate, String toDate) {
		String hql = "DELETE FROM StockData WHERE date >= :fromDate AND date <= :toDate";
		Session session = entityManager.unwrap(Session.class);
		session.createQuery(hql)
				.setParameter("fromDate", fromDate)
				.setParameter("toDate", toDate)
				.executeUpdate();
	}
}


