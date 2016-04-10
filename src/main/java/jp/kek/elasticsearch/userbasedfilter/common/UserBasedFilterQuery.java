package jp.kek.elasticsearch.userbasedfilter.common;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.Iterator;

class UserBasedFilterQuery {

    /* This class generates following schema's query:
     * {
     *   "query": {
     *     "bool": {
     *       "must": [
     *         {"match_all": {}}  // insert original query here
     *       ],
     *       "filter": [
     *         {"query": {
     *           "query_string": {"query": "some_key1:\"some value1\" OR some_key2:\"some_key2\""}
     *         }},
     *         {"bool": {
     *           "should": [
     *             {"wildcard": {"_type": "some_allowed_type"}},
     *             {"wildcard": {"_type": "another_allowed_type"}}
     *           ]
     *         }}
     *       ]
     *     }
     *   },
     *   "size":100  // insert original other options here
     * }
     */

    ESLogger logger;

    JSONObject originalContent;
    JSONObject root;
    JSONObject query;
    JSONObject bool;
    JSONArray must;
    JSONArray filter;
    JSONObject filterQuery;
    JSONObject filterBoolIndex;
    JSONArray filterBoolIndexShould;
    JSONObject filterBoolType;
    JSONArray filterBoolTypeShould;

    UserBasedFilterQuery(String content, String[] indices, String[] types, String query_string) {
        logger = Loggers.getLogger(UserBasedFilterQuery.class);

        originalContent = new JSONObject(content);
        root = new JSONObject();
        query = new JSONObject();
        bool = new JSONObject();
        must = new JSONArray();
        filter = new JSONArray();
        filterQuery = new JSONObject();
        filterBoolIndex = new JSONObject();
        filterBoolIndexShould = new JSONArray();
        filterBoolType = new JSONObject();
        filterBoolTypeShould = new JSONArray();

        filterBoolType.put("should", filterBoolTypeShould);
        filter.put(new JSONObject().put("bool", filterBoolType));
        //filterBoolIndex.put("should", filterBoolIndexShould);
        filter.put(new JSONObject().put("bool", filterBoolIndex));
        filter.put(new JSONObject().put("query", filterQuery));
        bool.put("filter", filter);
        bool.put("must", must);
        query.put("bool", bool);
        root.put("query", query);

        must.put(originalContent.get("query"));
        filterQuery.put("query_string", new JSONObject().put("query", query_string));

        /* wildcard query doesn't support against _index field. So current plugin filters index request by URL base.
         *  for (String index: indices) {
         *      filterBoolIndexShould.put(new JSONObject().put("wildcard", new JSONObject().put("_index", index)));
         * }
         */

        for (String type: types) {
            filterBoolTypeShould.put(new JSONObject().put("wildcard", new JSONObject().put("_type", type)));
        }
        Iterator<String> keys = originalContent.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (!key.equals("query")) {
                root.put(key, originalContent.get(key));
            }
        }

        logger.debug("generated query: " + getStringContent());
    }

    String getStringContent() {
        return root.toString();
    }
}
