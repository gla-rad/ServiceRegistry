package net.maritimeconnectivity.serviceregistry.services;

import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.components.Gmsp;
import net.maritimeconnectivity.serviceregistry.models.domain.Instance;
import net.maritimeconnectivity.serviceregistry.models.domain.SearchArea;
import net.maritimeconnectivity.serviceregistry.utils.SearchAreaCalculator;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
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

    private final Gmsp gmsp;

    private final SearchAreaCalculator sac;

    public SubscriptionService(Gmsp gmsp, SearchAreaCalculator searchAreaCalculator) {
        this.gmsp = gmsp;
        this.sac = searchAreaCalculator;

    }


    /**
     * Update geo-based subscriptions with the GMSP such that this instance is always subscribed to all subject areas
     * for which it contains services
     */
    public void addSubscription(Instance newInstance) {

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
    }

    public void removeSubscription(Instance instance) {
        // TODO
    }


}
