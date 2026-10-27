package crmpams.dao;

import crmpams.model.LookupItem;
import crmpams.model.UrgencyLevel;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Reads the reference tables managed in the Maintenance module (13.3). */
public interface ReferenceDataDao {

    List<LookupItem> listProjectTypes(Connection c) throws SQLException;

    /** Active urgency levels from lowest to highest rank. */
    List<UrgencyLevel> listUrgencyLevels(Connection c) throws SQLException;

    /** Active workflow stages in workflow order. */
    List<LookupItem> listWorkflowStages(Connection c) throws SQLException;

    /** Active production staff accounts (designers, drafters, modelers), keyed by employee id. */
    List<LookupItem> listProductionStaff(Connection c) throws SQLException;

    Optional<Integer> findStageId(Connection c, String stageName) throws SQLException;

    boolean isActiveProjectType(Connection c, int projectTypeId) throws SQLException;

    boolean isActiveUrgencyLevel(Connection c, int urgencyLevelId) throws SQLException;
}
