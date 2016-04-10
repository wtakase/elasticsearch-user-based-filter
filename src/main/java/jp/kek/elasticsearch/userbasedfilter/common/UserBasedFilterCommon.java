package jp.kek.elasticsearch.userbasedfilter.common;

import jp.kek.elasticsearch.userbasedfilter.common.UserBasedFilterProperties;

public class UserBasedFilterCommon {

    static private final UserBasedFilterProperties properties = new UserBasedFilterProperties();
    static public final String UBF_PROPERTIES_FILE = "/etc/elasticsearch/user-based-filter/user-based-filter.properties";
    static public final String UBF_STR_NULL = properties.getProperty("string_null");
    static public final String UBF_USERMAP = properties.getProperty("usermap_file");
    static public final String UBF_ANONYMOUS_USER = properties.getProperty("anonymous_user");
    static public final String UBF_REMOTE_USER = properties.getProperty("remote_user");
    static public final String UBF_ALT_REMOTE_USER = properties.getProperty("alt_remote_user");
    static public final String UBF_USERMAP_FIELD_SEPARATOR = properties.getProperty("usermap_field_separator", " ");
    static public final String UBF_USERMAP_DOCUMENT_FIELD_SEPARATOR = properties.getProperty("usermap_document_field_separator");
    static public final String UBF_USERMAP_FIELD_ITEM_DELIMITER = properties.getProperty("usermap_field_item_delimiter");
    static public final String UBF_KIBANA_INDEX_PREFIX = properties.getProperty("kibana_index_prefix");
}
