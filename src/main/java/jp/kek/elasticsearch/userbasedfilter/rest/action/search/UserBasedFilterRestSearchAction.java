package jp.kek.elasticsearch.userbasedfilter.rest.action.search;

import jp.kek.elasticsearch.userbasedfilter.common.UserBasedFilterUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.Strings;
import org.elasticsearch.rest.action.support.RestStatusToXContentListener;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.search.RestSearchAction;

public class UserBasedFilterRestSearchAction extends RestSearchAction {

    @Inject
    public UserBasedFilterRestSearchAction(Settings settings, RestController controller, Client client) {
        super(settings, controller, client);
    }

    @Override
    public void handleRequest(final RestRequest request, final RestChannel channel, final Client client) {
        String userId = UserBasedFilterUtils.getUserId(request);
        if (!userId.equals("kibana")) {
            logger.debug("User ID: " + userId);
            logger.debug("Request path: " + request.path());
            logger.debug("Request query: " + request.content().toUtf8());
        }

        if (UserBasedFilterUtils.isAdmin(userId)) {
            logger.debug("Admin bypass");
            super.handleRequest(request, channel, client);
        } else {
            SearchRequest searchRequest = RestSearchAction.parseSearchRequest(request, parseFieldMatcher);

            String[] indices = UserBasedFilterUtils.trimIndices(request.param("index"), userId);

            if (indices.length == 0) {
                // current plugin cannot filter index names by wildcard. So index name must be specified in URL.
                logger.warn("No indices specified");
                searchRequest.indices("");
                searchRequest.types("");
                searchRequest.source(new BytesArray(""));
            }

            // skip filtering kibana_index
            if (!UserBasedFilterUtils.isKibanaIndex(indices)) {
                String[] types = UserBasedFilterUtils.trimTypes(request.param("type"), userId);
                logger.debug("Trimed indices: " + Strings.arrayToCommaDelimitedString(indices));
                logger.debug("SearchAction: Trimed types: " + Strings.arrayToCommaDelimitedString(types));

                searchRequest.indices(indices);
                searchRequest.types(types);

                BytesReference filteredBodyContent = UserBasedFilterUtils.appendDocumentFilters(searchRequest.source(), userId, null, UserBasedFilterUtils.getAllowedTypes(userId));
                //BytesReference filteredBodyContent = UserBasedFilterUtils.appendDocumentFilters(searchRequest.source(), userId, UserBasedFilterUtils.getAllowedIndices(userId), UserBasedFilterUtils.getAllowedTypes(userId));
                logger.debug("Filtered bodyContent: " + filteredBodyContent.toUtf8());

                searchRequest.source(filteredBodyContent);
            }
            client.search(searchRequest, new RestStatusToXContentListener<SearchResponse>(channel));
        }
    }
}
