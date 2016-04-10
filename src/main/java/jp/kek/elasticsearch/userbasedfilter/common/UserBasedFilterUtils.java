package jp.kek.elasticsearch.userbasedfilter.common;

import jp.kek.elasticsearch.userbasedfilter.common.UserBasedFilterCommon;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.Strings;
import org.elasticsearch.rest.RestRequest;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;

public class UserBasedFilterUtils {

    static ESLogger logger = Loggers.getLogger(UserBasedFilterUtils.class);

    static public String exceptionAsString(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    static public String getUserId(final RestRequest request) {
        String userId = request.header(UserBasedFilterCommon.UBF_REMOTE_USER);
        if (userId == null) {
            userId = request.header(UserBasedFilterCommon.UBF_ALT_REMOTE_USER);
            if (userId == null) {
                userId = UserBasedFilterCommon.UBF_ANONYMOUS_USER;
            }
        }
        return userId;
    }

    static private String getUsermapField(String userId, int fieldIndex) {
        return getUsermapField(userId, fieldIndex, false);
    }

    static private String getUsermapField(String userId, int fieldIndex, boolean documentFieldSeparation) {
        FileInputStream fileInputStream = null;
        BufferedReader bufferedReader = null;
        String fieldValue = UserBasedFilterCommon.UBF_STR_NULL;
        try {
            fileInputStream = new FileInputStream(UserBasedFilterCommon.UBF_USERMAP);
            bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
               if (line.startsWith("#")) {
                   continue;
               }
               String[] fields = line.split("[" + UserBasedFilterCommon.UBF_USERMAP_FIELD_SEPARATOR + "|\\" + UserBasedFilterCommon.UBF_USERMAP_DOCUMENT_FIELD_SEPARATOR + "]");
               if (fields[0].equals(userId)) {
                   if (documentFieldSeparation) {
                       try {
                           fieldValue = line.split("\\" + UserBasedFilterCommon.UBF_USERMAP_DOCUMENT_FIELD_SEPARATOR)[1];
                       } catch (Exception e) {
                           fieldValue = "";
                       }
                   } else {
                       fieldValue = fields[fieldIndex];
                   }
                   break;
               }
            }
            bufferedReader.close();
        } catch (Exception e) {
            logger.error(exceptionAsString(e));
        } finally {
            return fieldValue;
        }
    }

    static public boolean isAdmin(String userId) {
        String adminFlag = getUsermapField(userId, 1);
        if (adminFlag.equals("1")) {
            return true;
        } else {
            return false;
        }
    }

    static public Pattern[] getPatterns(String[] inputs) {
        ArrayList<Pattern> patterns = new ArrayList<Pattern>();
        for (String input: inputs) {
            patterns.add(Pattern.compile(input.replace("*", ".*")));
        }
        return patterns.toArray(new Pattern[0]);
    }

    static public String[] getAllowedIndices(String userId) {
        return getUsermapField(userId, 2).split(UserBasedFilterCommon.UBF_USERMAP_FIELD_ITEM_DELIMITER);
    }

    static public Pattern[] getAllowedIndicePatterns(String userId) {
        return getPatterns(getAllowedIndices(userId));
    }

    static public String[] getAllowedTypes(String userId) {
        return getUsermapField(userId, 3).split(UserBasedFilterCommon.UBF_USERMAP_FIELD_ITEM_DELIMITER);
    }

    static public Pattern[] getAllowedTypePatterns(String userId) {
        return getPatterns(getAllowedTypes(userId));
    }

    static public String getDocumentFilters(String userId) {
        String filters = "";
        for (String keyValue: getUsermapField(userId, 1, true).split(UserBasedFilterCommon.UBF_USERMAP_FIELD_ITEM_DELIMITER)) {
            try {
                String key = keyValue.split("=")[0];
                if (key.equals(UserBasedFilterCommon.UBF_STR_NULL)) {
                    continue;
                }
                String value = null;
                try {
                    value = keyValue.split("=")[1];
                    if (!key.equals("") && key != null && !value.equals("") && value != null) {
                        if (!filters.equals("")) {
                            filters += " OR ";
                        }
                        filters += key + ":" + value;
                    }
                } catch (Exception e) {
                    if (!filters.equals("")) {
                        filters += " OR ";
                    }
                    filters += key;
                    //logger.error(exceptionAsString(e));
                }
            } catch (Exception e2) {
                logger.error(exceptionAsString(e2));
            }
        }
        return filters;
    }

    static public String[] trimElements(String[] inputs, Pattern[] lookups) {
        ArrayList<String> outputs = new ArrayList<String>();
        for (String input: inputs) {
            for (Pattern lookup: lookups) {
                if (lookup.matcher(input).find()) {
                    outputs.add(input);
                    break;
                }
            }
        }
        return outputs.toArray(new String[0]);
    }

    static public String[] trimIndices(String indices, String userId) {
        return trimElements(Strings.splitStringByCommaToArray(indices), getAllowedIndicePatterns(userId));
    }

    static public String[] trimTypes(String types, String userId) {
        return trimElements(Strings.splitStringByCommaToArray(types), getAllowedTypePatterns(userId));
    }

    static public boolean isKibanaIndex(String[] indices) {
        if (indices.length == 1 && indices[0].startsWith(UserBasedFilterCommon.UBF_KIBANA_INDEX_PREFIX)) {
            logger.debug("skip kibana_index filtering: " + UserBasedFilterCommon.UBF_KIBANA_INDEX_PREFIX);
            return true;
        } else {
            return false;
        }
    }

    static public String appendDocumentFilters(String content, String userId, String[] indices, String[] types) {
        return appendDocumentFilters(new BytesArray(content), userId, indices, types).toUtf8();
    }

    static public BytesReference appendDocumentFilters(BytesReference content, String userId, String[] indices, String[] types) {
        String stringContent = content.toUtf8();
        if (!isValidContent(stringContent)) {
            return null;
        }
        try {
            String filters = getDocumentFilters(userId);
            logger.debug("filters: " + filters);
            UserBasedFilterQuery filteredContent = new UserBasedFilterQuery(stringContent, indices, types, filters);
            return new BytesArray(filteredContent.getStringContent());
        } catch (Exception e) {
            logger.error(exceptionAsString(e));
            return null;
        }
    }

    static private boolean isValidContent(String content) {
        if (content.equals("") || content.equals("{}") || content == null) {
            return false;
        }
        String jsonText = (new JSONObject(content)).toString();
        if (jsonText.equals("{}")) {
            return false;
        }
        return true;
    }

    static public String getJSONObjectString(JSONObject obj, String key) {
        try {
            return obj.getString(key);
        } catch (Exception e) {
            //logger.error(exceptionAsString(e));
            logger.warn("getJSONObjectString: failed to get JSON object as String");
            return null;
        }
    }

    static public String[] getJSONObjectStrings(JSONObject obj, String key) {
        ArrayList<String> objStrings = new ArrayList<String>();
        try {
            JSONArray ary = obj.getJSONArray(key);
            for (int i = 0; i < ary.length(); i++) {
                try {
                    objStrings.add(ary.getString(i));
                } catch (Exception e) {
                    logger.error(exceptionAsString(e));
                }
            }
        } catch (Exception e) {
            logger.debug("getJSONObjectStrings: failed to get JSON array");
            String objString = getJSONObjectString(obj, key);
            if (objString != null) {
                objStrings.add(objString);
            }
        }
        return objStrings.toArray(new String[0]);
    }
}
