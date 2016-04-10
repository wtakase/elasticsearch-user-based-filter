package jp.kek.elasticsearch.userbasedfilter.rest.action.get;

import jp.kek.elasticsearch.userbasedfilter.common.UserBasedFilterUtils;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.rest.action.support.RestActions;
import org.elasticsearch.rest.action.support.RestBuilderListener;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.action.get.RestGetAction;
import org.elasticsearch.search.fetch.source.FetchSourceContext;

import static org.elasticsearch.rest.RestStatus.NOT_FOUND;
import static org.elasticsearch.rest.RestStatus.OK;

public class UserBasedFilterRestGetAction extends RestGetAction {

    @Inject
    public UserBasedFilterRestGetAction(Settings settings, RestController controller, Client client) {
        super(settings, controller, client);
    }

    @Override
    public void handleRequest(final RestRequest request, final RestChannel channel, final Client client) {
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

    void trimHandleRequest(final RestRequest request, final RestChannel channel, final Client client, String userId) {
        String[] indices = UserBasedFilterUtils.trimIndices(request.param("index"), userId);
        if (UserBasedFilterUtils.isKibanaIndex(indices)) {
            super.handleRequest(request, channel, client);
            return;
        }
        String[] types = UserBasedFilterUtils.trimTypes(request.param("type"), userId);
        String indicesString = Strings.arrayToCommaDelimitedString(indices);
        String typesString = Strings.arrayToCommaDelimitedString(types);
        logger.debug("Trimed indices: " + indicesString);
        logger.debug("Trimed types: " + typesString);

        final GetRequest getRequest = new GetRequest(indicesString, typesString, request.param("id"));
        getRequest.operationThreaded(true);
        getRequest.refresh(request.paramAsBoolean("refresh", getRequest.refresh()));
        getRequest.routing(request.param("routing"));  // order is important, set it after routing, so it will set the routing
        getRequest.parent(request.param("parent"));
        getRequest.preference(request.param("preference"));
        getRequest.realtime(request.paramAsBoolean("realtime", null));
        getRequest.ignoreErrorsOnGeneratedFields(request.paramAsBoolean("ignore_errors_on_generated_fields", false));

        String sField = request.param("fields");
        if (sField != null) {
            String[] sFields = Strings.splitStringByCommaToArray(sField);
            if (sFields != null) {
                getRequest.fields(sFields);
            }
        }

        getRequest.version(RestActions.parseVersion(request));
        getRequest.versionType(VersionType.fromString(request.param("version_type"), getRequest.versionType()));

        getRequest.fetchSourceContext(FetchSourceContext.parseFromRestRequest(request));

        client.get(getRequest, new RestBuilderListener<GetResponse>(channel) {
            @Override
            public RestResponse buildResponse(GetResponse response, XContentBuilder builder) throws Exception {
                builder.startObject();
                response.toXContent(builder, request);
                builder.endObject();
                if (!response.isExists()) {
                    return new BytesRestResponse(NOT_FOUND, builder);
                } else {
                    return new BytesRestResponse(OK, builder);
                }
            }
        });
    }
}
