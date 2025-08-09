package com.stocks.repository;

import com.stocks.entity.StrikeSetupEntity;
import com.stocks.entity.TradeSetupEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Repository
public class StrikeSetupManager {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TradeSetupManager tradeSetupManager;

    @Transactional
    public void saveStrikeSetup(StrikeSetupEntity strikeSetup) {
        entityManager.persist(strikeSetup);
    }

    @Transactional(readOnly = true)
    public StrikeSetupEntity findById(Long id) {
        return entityManager.find(StrikeSetupEntity.class, id);
    }

    @Transactional(readOnly = true)
    public List<StrikeSetupEntity> findAll() {
        return entityManager.createQuery("FROM StrikeSetupEntity", StrikeSetupEntity.class)
                .getResultList();
    }
}