package com.stocks.repository;

import com.stocks.dto.EodAnalyzerTO;
import com.stocks.entity.EodAnalyzerEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Repository
public class EodAnalyzerManager {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void persistEodAnalyzer(EodAnalyzerTO eodAnalyzer) {
        entityManager.persist(mapEodAnalyzer(eodAnalyzer));
    }

    @Transactional
    public void persistEodAnalyzers(List<EodAnalyzerTO> eodAnalyzers) {
        for(EodAnalyzerTO eodAnalyzer : eodAnalyzers) {
            persistEodAnalyzer(eodAnalyzer);
        }
        entityManager.flush();
    }

    public List<EodAnalyzerTO> fetchStockAndStockDate(String stockSymbol) {
        String hql = "FROM EodAnalyzerEntity WHERE stockSymbol = :stockSymbol";
        return entityManager.createQuery(hql, EodAnalyzerEntity.class)
                .setParameter("stockSymbol", stockSymbol)
                .getResultStream().map(this::mapEntityToTO).collect(Collectors.toList());
    }

    private EodAnalyzerTO mapEntityToTO(EodAnalyzerEntity eodAnalyzerEntity) {
        EodAnalyzerTO eodAnalyzerTO = new EodAnalyzerTO();
        eodAnalyzerTO.setStockSymbol(eodAnalyzerEntity.getStockSymbol());
        eodAnalyzerTO.setStockDate(eodAnalyzerEntity.getStockDate());
        eodAnalyzerTO.setOiInterpretation(eodAnalyzerEntity.getOiInterpretation());
        return eodAnalyzerTO;
    }

    private EodAnalyzerEntity mapEodAnalyzer(EodAnalyzerTO eodAnalyzer) {
        EodAnalyzerEntity eodAnalyzerEntity = new EodAnalyzerEntity();
        eodAnalyzerEntity.setStockSymbol(eodAnalyzer.getStockSymbol());
        eodAnalyzerEntity.setStockDate(eodAnalyzer.getStockDate());
        eodAnalyzerEntity.setOiInterpretation(eodAnalyzer.getOiInterpretation());
        return eodAnalyzerEntity;
    }
}