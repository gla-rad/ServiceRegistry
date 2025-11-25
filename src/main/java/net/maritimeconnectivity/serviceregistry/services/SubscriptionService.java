package net.maritimeconnectivity.serviceregistry.services;

import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.components.Gmsp;
import net.maritimeconnectivity.serviceregistry.models.domain.Instance;
import net.maritimeconnectivity.serviceregistry.models.domain.SearchArea;
import net.maritimeconnectivity.serviceregistry.repos.InstanceRepo;
import net.maritimeconnectivity.serviceregistry.utils.SearchAreaCalculator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Service implementation for managing subscriptions with the edgerouter.
 * Triggered upon updates to instances in the local service registry.
 *
 * @author Jakob Svenningsen (email: jakob@dmc.international)
 */
@Service
@Slf4j
public class SubscriptionService {

    @Value("${info.gmsp.search.globalSubject}")
    private String globalSearchSubject;

    private final Gmsp gmsp;

    private final SearchAreaCalculator sac;
    private final InstanceRepo instanceRepo;

    public SubscriptionService(Gmsp gmsp, SearchAreaCalculator searchAreaCalculator, InstanceRepo instanceRepo) {
        this.gmsp = gmsp;
        this.sac = searchAreaCalculator;
        this.instanceRepo = instanceRepo;
    }


    /**
     * Update geo-based subscriptions with the GMSP such that this instance is always subscribed to all subject areas
     * for which it contains services
     */
    public void removeSubscriptions() {
        Set<String> existingSubscriptions = gmsp.getSubscriptions();
        // Remove subscriptions not
        List<SearchArea> allAreasInDb = instanceRepo.findAllInstanceSearchAreasUsed();
        ArrayList<String> allSubjectsInDb = this.sac.areaToSubjectMapper(allAreasInDb);
        for (String existingSub : existingSubscriptions) {
            if (!allSubjectsInDb.contains(existingSub) && !Objects.equals(existingSub, globalSearchSubject)) {
                log.debug("SubscriptionService : Removing subscription for subject {} as no instances in the DB require it", existingSub);
                gmsp.unsubscribe(existingSub);
            }
        }
    }


    public void updateSubscriptions(Instance newInstance) {
        // Get search areas for the instance
        List<SearchArea> searchAreas = newInstance.getSearchAreas().stream().toList();
        ArrayList<String> subjects = this.sac.areaToSubjectMapper(searchAreas);


        Set<String> existingSubscriptions = gmsp.getSubscriptions();
        for (String subject : subjects) {

            if (!existingSubscriptions.contains(subject)) {
                gmsp.subscribe(subject);
            } else {
                log.debug("SubscriptionService : Subscription already exists for subject {}", subject);
            }
        }
        // Removes necessary subs, e.g. if coverage area has shrinked
        removeSubscriptions();
    }


}
