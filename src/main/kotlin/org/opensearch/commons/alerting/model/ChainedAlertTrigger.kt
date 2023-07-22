package org.opensearch.commons.alerting.model

import org.opensearch.common.CheckedFunction
import org.opensearch.common.UUIDs
import org.opensearch.commons.alerting.model.Trigger.Companion.ACTIONS_FIELD
import org.opensearch.commons.alerting.model.Trigger.Companion.ID_FIELD
import org.opensearch.commons.alerting.model.Trigger.Companion.NAME_FIELD
import org.opensearch.commons.alerting.model.Trigger.Companion.SEVERITY_FIELD
import org.opensearch.commons.alerting.model.action.Action
import org.opensearch.core.ParseField
import org.opensearch.core.common.io.stream.StreamInput
import org.opensearch.core.common.io.stream.StreamOutput
import org.opensearch.core.xcontent.NamedXContentRegistry
import org.opensearch.core.xcontent.ToXContent
import org.opensearch.core.xcontent.XContentBuilder
import org.opensearch.core.xcontent.XContentParser
import org.opensearch.core.xcontent.XContentParserUtils
import org.opensearch.script.Script
import java.io.IOException

data class ChainedAlertTrigger(
    override val id: String = UUIDs.base64UUID(),
    override val name: String,
    override val severity: String,
    override val actions: List<Action>,
    val condition: Script
) : Trigger {

    @Throws(IOException::class)
    constructor(sin: StreamInput) : this(
        sin.readString(), // id
        sin.readString(), // name
        sin.readString(), // severity
        sin.readList(::Action), // actions
        Script(sin)
    )

    override fun toXContent(builder: XContentBuilder, params: ToXContent.Params): XContentBuilder {
        builder.startObject()
            .startObject(CHAINED_ALERT_TRIGGER_FIELD)
            .field(ID_FIELD, id)
            .field(NAME_FIELD, name)
            .field(SEVERITY_FIELD, severity)
            .startObject(CONDITION_FIELD)
            .field(SCRIPT_FIELD, condition)
            .endObject()
            .field(ACTIONS_FIELD, actions.toTypedArray())
            .endObject()
            .endObject()
        return builder
    }

    override fun name(): String {
        return CHAINED_ALERT_TRIGGER_FIELD
    }

    /** Returns a representation of the trigger suitable for passing into painless and mustache scripts. */
    fun asTemplateArg(): Map<String, Any> {
        return mapOf(
            ID_FIELD to id,
            NAME_FIELD to name,
            SEVERITY_FIELD to severity,
            ACTIONS_FIELD to actions.map { it.asTemplateArg() }
        )
    }

    @Throws(IOException::class)
    override fun writeTo(out: StreamOutput) {
        out.writeString(id)
        out.writeString(name)
        out.writeString(severity)
        out.writeCollection(actions)
        condition.writeTo(out)
    }

    companion object {
        const val CHAINED_ALERT_TRIGGER_FIELD = "chained_alert_trigger"
        const val CONDITION_FIELD = "condition"
        const val SCRIPT_FIELD = "script"
        const val QUERY_IDS_FIELD = "query_ids"

        val XCONTENT_REGISTRY = NamedXContentRegistry.Entry(
            Trigger::class.java,
            ParseField(CHAINED_ALERT_TRIGGER_FIELD),
            CheckedFunction { parseInner(it) }
        )

        @JvmStatic
        @Throws(IOException::class)
        fun parseInner(xcp: XContentParser): ChainedAlertTrigger {
            var id = UUIDs.base64UUID() // assign a default triggerId if one is not specified
            lateinit var name: String
            lateinit var severity: String
            lateinit var condition: Script
            val actions: MutableList<Action> = mutableListOf()

            if (xcp.currentToken() != XContentParser.Token.START_OBJECT && xcp.currentToken() != XContentParser.Token.FIELD_NAME) {
                XContentParserUtils.throwUnknownToken(xcp.currentToken(), xcp.tokenLocation)
            }

            // If the parser began on START_OBJECT, move to the next token so that the while loop enters on
            // the fieldName (or END_OBJECT if it's empty).
            if (xcp.currentToken() == XContentParser.Token.START_OBJECT) xcp.nextToken()

            while (xcp.currentToken() != XContentParser.Token.END_OBJECT) {
                val fieldName = xcp.currentName()

                xcp.nextToken()
                when (fieldName) {
                    ID_FIELD -> id = xcp.text()
                    NAME_FIELD -> name = xcp.text()
                    SEVERITY_FIELD -> severity = xcp.text()
                    CONDITION_FIELD -> {
                        xcp.nextToken()
                        condition = Script.parse(xcp)
                        require(condition.lang == Script.DEFAULT_SCRIPT_LANG) {
                            "Invalid script language. Allowed languages are [${Script.DEFAULT_SCRIPT_LANG}]"
                        }
                        xcp.nextToken()
                    }
                    ACTIONS_FIELD -> {
                        XContentParserUtils.ensureExpectedToken(
                            XContentParser.Token.START_ARRAY,
                            xcp.currentToken(),
                            xcp
                        )
                        while (xcp.nextToken() != XContentParser.Token.END_ARRAY) {
                            actions.add(Action.parse(xcp))
                        }
                    }
                }
                xcp.nextToken()
            }

            return ChainedAlertTrigger(
                name = requireNotNull(name) { "Trigger name is null" },
                severity = requireNotNull(severity) { "Trigger severity is null" },
                condition = requireNotNull(condition) { "Trigger condition is null" },
                actions = requireNotNull(actions) { "Trigger actions are null" },
                id = requireNotNull(id) { "Trigger id is null." }
            )
        }

        @JvmStatic
        @Throws(IOException::class)
        fun readFrom(sin: StreamInput): ChainedAlertTrigger {
            return ChainedAlertTrigger(sin)
        }
    }
}
