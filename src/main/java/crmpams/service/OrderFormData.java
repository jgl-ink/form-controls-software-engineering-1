package crmpams.service;

import crmpams.model.LookupItem;
import crmpams.model.UrgencyLevel;
import java.util.List;

/** Reference data the New Order form needs before it opens. */
public record OrderFormData(List<LookupItem> projectTypes, List<UrgencyLevel> urgencyLevels) {
}
