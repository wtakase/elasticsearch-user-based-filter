package jp.kek.elasticsearch.userbasedfilter;

import jp.kek.elasticsearch.userbasedfilter.rest.action.get.UserBasedFilterRestGetAction;
import jp.kek.elasticsearch.userbasedfilter.rest.action.get.UserBasedFilterRestMultiGetAction;
import jp.kek.elasticsearch.userbasedfilter.rest.action.search.UserBasedFilterRestSearchAction;
import jp.kek.elasticsearch.userbasedfilter.rest.action.search.UserBasedFilterRestMultiSearchAction;
import org.elasticsearch.rest.RestModule;
import org.elasticsearch.plugins.Plugin;
import java.util.Collection;
import java.util.Collections;

public class UserBasedFilterPlugin extends Plugin {

    @Override
    public String name() {
        return "user-based-filter";
    }

    @Override
    public String description() {
        return "Restricts REST API access based on REMOTE_USER";
    }

    public void onModule(RestModule module) {
        module.addRestAction(UserBasedFilterRestGetAction.class);
        module.addRestAction(UserBasedFilterRestMultiGetAction.class);
        module.addRestAction(UserBasedFilterRestSearchAction.class);
        module.addRestAction(UserBasedFilterRestMultiSearchAction.class);
    }
}
