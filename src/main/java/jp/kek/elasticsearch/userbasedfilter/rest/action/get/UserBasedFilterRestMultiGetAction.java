package jp.kek.elasticsearch.userbasedfilter.rest.action.get;

import jp.kek.elasticsearch.userbasedfilter.common.UserBasedFilterCommon;
import jp.kek.elasticsearch.userbasedfilter.common.UserBasedFilterUtils;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.Strings;
import org.elasticsearch.rest.action.support.RestToXContentListener;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.get.RestMultiGetAction;
import org.elasticsearch.search.fetch.source.FetchSourceContext;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.Arrays;

public class UserBasedFilterRestMultiGetAction extends RestMultiGetAction {

    private final boolean allowExplicitIndex;

    @Inject
    public UserBasedFilterRestMultiGetAction(Settings settings, RestController controller, Client client) {
        super(settings, controller, client);
        this.allowExplicitIndex = settings.getAsBoolean("rest.action.multi.allow_explicit_index", true);
    }

    @Override
    public void handleRequest(final RestRequest request, final RestChannel channel, final Client client) throws Exception {
        logger.debug("Request path: " + request.path());
        logger.debug("Request query: " + request.content().toUtf8());
        String userId = UserBasedFilterUtils.getUserId(request);
        logger.debug("User ID: " + userId);

        if (UserBasedFilterUtils.isAdmin(userId)) {
            logger.debug("Admin bypass");
            super.handleRequest(request, channel, client);
        } else {
            trimHandleRequest(request, channel, client, userId);
        }
    }

    void trimHandleRequest(final RestRequest request, final RestChannel channel, final Client client, String userId) throws Exception {
        String[] indices = UserBasedFilterUtils.trimIndices(request.param("index"), userId);
        String[] types = UserBasedFilterUtils.trimTypes(request.param("type"), userId);
        String[] allowedIndices = UserBasedFilterUtils.getAllowedIndices(userId);
        String[] allowedTypes = UserBasedFilterUtils.getAllowedTypes(userId);

        MultiGetRequest multiGetRequest = new MultiGetRequest();
        multiGetRequest.refresh(request.paramAsBoolean("refresh", multiGetRequest.refresh()));
        multiGetRequest.preference(request.param("preference"));
        multiGetRequest.realtime(request.paramAsBoolean("realtime", null));
        multiGetRequest.ignoreErrorsOnGeneratedFields(request.paramAsBoolean("ignore_errors_on_generated_fields", false));

        String[] sFields = null;
        String sField = request.param("fields");
        if (sField != null) {
            sFields = Strings.splitStringByCommaToArray(sField);
        }

        JSONObject content = new JSONObject(request.content().toUtf8());
        logger.debug("original content: " + content.toString());
        JSONArray docs = content.getJSONArray("docs");
        JSONArray filteredDocs = new JSONArray();
        if (UserBasedFilterUtils.isKibanaIndex(indices)) {
            filteredDocs = docs;
        } else {
            for (int i = 0; i < docs.length(); i++) {
                JSONObject row = docs.getJSONObject(i);
                // TODO: consider _index and _type parameters treated as JSONArray: ["xxx", "yyy"]
                String index = UserBasedFilterUtils.getJSONObjectString(row, "_index");
                String type = UserBasedFilterUtils.getJSONObjectString(row, "_type");
                if (index.startsWith(UserBasedFilterCommon.UBF_KIBANA_INDEX_PREFIX) || (index == null && UserBasedFilterUtils.isKibanaIndex(indices))) {
                    // skip kibana_index filtering
                    filteredDocs.put(row);
                } else if ((Arrays.asList(allowedIndices).contains(index) || (index == null && indices.length > 0)) && (Arrays.asList(allowedTypes).contains(type) || (type == null && types.length > 0))) {
                    filteredDocs.put(row);
                }
            }
        }
        JSONObject filteredContent = new JSONObject().put("docs", filteredDocs);
        logger.debug("filtered content: " + filteredContent.toString());
        FetchSourceContext defaultFetchSource = FetchSourceContext.parseFromRestRequest(request);
        multiGetRequest.add(Strings.arrayToCommaDelimitedString(indices), Strings.arrayToCommaDelimitedString(types), sFields, defaultFetchSource, request.param("routing"), new BytesArray(filteredContent.toString()), allowExplicitIndex);

        client.multiGet(multiGetRequest, new RestToXContentListener<MultiGetResponse>(channel));
    }
}
