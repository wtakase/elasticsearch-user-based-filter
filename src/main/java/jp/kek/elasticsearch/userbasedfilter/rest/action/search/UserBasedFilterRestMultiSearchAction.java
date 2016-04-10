package jp.kek.elasticsearch.userbasedfilter.rest.action.search;

import jp.kek.elasticsearch.userbasedfilter.common.UserBasedFilterCommon;
import jp.kek.elasticsearch.userbasedfilter.common.UserBasedFilterUtils;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.Strings;
import org.elasticsearch.rest.action.support.RestToXContentListener;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.search.RestMultiSearchAction;
import org.elasticsearch.rest.action.support.RestActions;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.Arrays;

public class UserBasedFilterRestMultiSearchAction extends RestMultiSearchAction {

    private final boolean allowExplicitIndex;

    @Inject
    public UserBasedFilterRestMultiSearchAction(Settings settings, RestController controller, Client client) {
        super(settings, controller, client);
        this.allowExplicitIndex = settings.getAsBoolean("rest.action.multi.allow_explicit_index", true);
    }

    @Override
    public void handleRequest(final RestRequest request, final RestChannel channel, final Client client) throws Exception {
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
            MultiSearchRequest multiSearchRequest = new MultiSearchRequest();
            String[] indices = UserBasedFilterUtils.trimIndices(request.param("index"), userId);
            String[] types = UserBasedFilterUtils.trimTypes(request.param("type"), userId);
            String[] allowedIndices = UserBasedFilterUtils.getAllowedIndices(userId);
            String[] allowedTypes = UserBasedFilterUtils.getAllowedTypes(userId);
            String path = request.path();
            boolean isTemplateRequest = isTemplateRequest(path);
            IndicesOptions indicesOptions = IndicesOptions.fromRequest(request, multiSearchRequest.indicesOptions());

            String[] content = request.content().toUtf8().split("\n");
            String filteredContent = "";
            for (int i = 0; i < content.length; i += 2) {
                JSONObject header = new JSONObject(content[i]);
                logger.debug("header: " + header.toString());
                String[] originalHeaderIndices = UserBasedFilterUtils.getJSONObjectStrings(header, "index");
                logger.debug("originalHeaderIndices: " + Strings.arrayToCommaDelimitedString(originalHeaderIndices));
                if (UserBasedFilterUtils.isKibanaIndex(originalHeaderIndices) || (originalHeaderIndices.length == 0 && UserBasedFilterUtils.isKibanaIndex(indices))) {
                    // skip kibana_index filtering
                    filteredContent += content[i] + "\n";
                    filteredContent += content[i + 1] + "\n";
                } else {
                    // TODO: filtering header index
                    String[] filteredHeaderIndices = UserBasedFilterUtils.trimIndices(Strings.arrayToCommaDelimitedString(originalHeaderIndices), userId);
                    if (filteredHeaderIndices.length == 0 && indices.length > 0) {
                        // indices are specified in URL
                        filteredContent += content[i] + "\n";
                        filteredContent += content[i + 1] + "\n";
                    } else if (filteredHeaderIndices.length > 0) {
                        JSONArray indicesArray = new JSONArray();
                        for (String filteredHeaderIndex: filteredHeaderIndices) {
                            indicesArray.put(filteredHeaderIndex);
                        }
                        header.put("index", indicesArray);
                        filteredContent += header.toString() + "\n";
                        filteredContent += UserBasedFilterUtils.appendDocumentFilters(content[i + 1], userId, null, allowedTypes) + "\n";
                    }
                }
            }
            logger.debug("Original query: " + RestActions.getRestContent(request).toUtf8());
            logger.debug("Filtered query: " + filteredContent);
            if (filteredContent.equals("")) {
                logger.error("No valid content found after filtering");
                return;
            }
            multiSearchRequest.add(new BytesArray(filteredContent), isTemplateRequest, indices, types, request.param("search_type"), request.param("routing"), indicesOptions, allowExplicitIndex);
            client.multiSearch(multiSearchRequest, new RestToXContentListener<MultiSearchResponse>(channel));
        }
    }

    private boolean isTemplateRequest(String path) {
        return (path != null && path.endsWith("/template"));
    }
}
