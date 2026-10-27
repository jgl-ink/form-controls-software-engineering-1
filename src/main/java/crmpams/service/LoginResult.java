package crmpams.service;

import crmpams.model.CurrentUser;
import crmpams.validation.ValidationResult;

/** Outcome of a login attempt. user is set only when the login succeeds. */
public record LoginResult(CurrentUser user, ValidationResult errors) {

    public boolean success() {
        return user != null;
    }
}
