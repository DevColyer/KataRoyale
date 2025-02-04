package com.kataroyale.app.services;

import com.kataroyale.app.documents.Competitor;
import com.kataroyale.app.repositories.CompetitorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientException;

import java.util.Comparator;
import java.util.stream.Stream;

@Service
public class RoyaleService {

    private final CompetitorRepository competitorRepository;
    private final CodeWarsService codeWarsService;
    private final Logger logger = LoggerFactory.getLogger(RoyaleService.class);

    @Autowired
    public RoyaleService(CompetitorRepository competitorRepository, CodeWarsService codeWarsService) {
        this.codeWarsService = codeWarsService;
        this.competitorRepository = competitorRepository;
    }

    public Stream<Competitor> getCompetitors() {
        return competitorRepository.findAllBy()
                .sorted(Comparator.comparingInt(Competitor::getHonorInBattle).reversed());
    }

    public void addCompetitor(String competitorUserName) {
        try{
            if(getCompetitors().noneMatch(competitor -> competitor.getUserName().equals(competitorUserName))){
                Competitor competitor = codeWarsService.getCodeWarsUser(competitorUserName);
                competitor.setHonorOnLastReset(competitor.getTotalHonor());
                competitorRepository.save(competitor);
            }
        }catch (WebClientException e){
            e.printStackTrace();
            logger.warn("Failed to add competitor: {}", competitorUserName);
        }
    }

    public void updateCompetitors(){
        getCompetitors()
                .filter(Competitor::getIsCompeting)
                .map(
                        competitor -> {
                            competitor = codeWarsService.getCodeWarsUser(competitor);
                            competitor.setHonorInBattle(competitor.getTotalHonor() - competitor.getHonorOnLastReset());
                            return competitor;
                        }
                )
                .forEach(competitorRepository::save);
    }

    public void resetCompetitors() {
        getCompetitors()
                .map(
                        competitor -> {
                            competitor = codeWarsService.getCodeWarsUser(competitor.getUserName());
                            competitor.setHonorOnLastReset(competitor.getTotalHonor());
                            competitor.setIsCompeting(true);
                            return competitor;
                        }
                )
                .forEach(competitorRepository::save);
    }

    public void cutCompetitors(Integer amountCut) {
        getCompetitors()
                .filter(Competitor::getIsCompeting)
                .sorted(Comparator.comparingInt(Competitor::getHonorInBattle))
                .limit(amountCut)
                .peek(competitor -> competitor.setIsCompeting(false))
                .forEach(competitorRepository::save);
    }

    public void keepCompetitors(Integer amountKept) {
        getCompetitors()
                .skip(amountKept)
                .filter(Competitor::getIsCompeting)
                .peek(competitor -> competitor.setIsCompeting(false))
                .forEach(competitorRepository::save);
    }

    public void pickWinner() {
        keepCompetitors(1);
    }

    public void cutFifthOfCompetitors() {
        long amountToCut = getCompetitors()
                .count()/5;
        cutCompetitors((int) amountToCut);
    }
}
