package crmpams.service;

import crmpams.model.LookupItem;
import crmpams.model.UrgencyLevel;
import java.util.List;

/** Drop-down values for the Search Orders form. */
public record SearchOptions(List<LookupItem> projectTypes, List<LookupItem> stages,
        List<LookupItem> employees, List<UrgencyLevel> urgencyLevels) {
}
