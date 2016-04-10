package jp.kek.elasticsearch.userbasedfilter.common;

import jp.kek.elasticsearch.userbasedfilter.common.UserBasedFilterCommon;
import jp.kek.elasticsearch.userbasedfilter.common.UserBasedFilterUtils;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

class UserBasedFilterProperties {

    ESLogger logger;

    UserBasedFilterProperties() {
        logger = Loggers.getLogger(UserBasedFilterProperties.class);
    }

    String getProperty(String key) {
        return getProperty(key, null);
    }

    String getProperty(String key, String defaultValue) {
        Properties properties = new Properties();
        InputStream inputStream = null;
        String value = defaultValue;
        try {
            inputStream = new FileInputStream(new File(UserBasedFilterCommon.UBF_PROPERTIES_FILE));
            properties.load(inputStream);
            value = properties.getProperty(key);
            if (value.equals("")) {
                value = defaultValue;
            }
        } catch (Exception e) {
            logger.error(UserBasedFilterUtils.exceptionAsString(e));
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception e) {
                logger.error(UserBasedFilterUtils.exceptionAsString(e));
            }
        }
        return value;
    }
}
