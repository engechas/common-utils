package org.opensearch.commons.alerting.settings;

import org.opensearch.common.settings.Setting

// TODO - better class name
class SharedSettings {
    companion object {
        // TODO - should this be in security analytics? Or maybe just renamed
        val STREAMING_SECURITY_ANALYTICS = Setting.boolSetting(
            "plugins.alerting.streaming_security_analytics",
            false,
            Setting.Property.NodeScope, Setting.Property.Dynamic
        )
    }
}
