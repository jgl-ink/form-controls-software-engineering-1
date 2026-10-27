package crmpams.service;

import crmpams.model.LookupItem;
import java.util.List;

/** Active workflow stages and production staff for the Monitoring Filter form. */
public record MonitoringOptions(List<LookupItem> activeStages, List<LookupItem> employees) {
}
