package org.opensearch.commons.alerting.action;

import org.opensearch.action.ActionRequest
import org.opensearch.action.ActionRequestValidationException
import org.opensearch.core.common.io.stream.StreamInput
import org.opensearch.core.common.io.stream.StreamOutput
import org.opensearch.core.xcontent.XContentParser
import org.opensearch.core.xcontent.XContentParserUtils
import java.io.IOException

class RunWorkflowRequest : ActionRequest {
    var workflowId: String
    var documents: List<IdDocPair>

    constructor(workflowId: String, documents: List<IdDocPair>) : super() {
        this.workflowId = workflowId
        this.documents = documents
    }

    @Throws(IOException::class)
    constructor(sin: StreamInput) : this(
        workflowId = sin.readString(),
        documents = sin.readList(::IdDocPair)
    )

    override fun validate(): ActionRequestValidationException? {
        return null
    }

    @Throws(IOException::class)
    override fun writeTo(out: StreamOutput) {
        out.writeString(workflowId)
        out.writeCollection(documents)
    }

    companion object {
        const val WORKFLOW_ID_FIELD = "workflowId"
        const val DOCUMENTS_FIELD = "documents"

        @JvmStatic
        @JvmOverloads
        @Throws(IOException::class)
        fun parse(xcp: XContentParser): RunWorkflowRequest {
            var workflowId: String? = null
            var documents: MutableList<IdDocPair> = mutableListOf()

            XContentParserUtils.ensureExpectedToken(XContentParser.Token.START_OBJECT, xcp.currentToken(), xcp)
            while (xcp.nextToken() != XContentParser.Token.END_OBJECT) {
                val fieldName = xcp.currentName()
                xcp.nextToken()

                when (fieldName) {
                    WORKFLOW_ID_FIELD -> workflowId = xcp.text()
                    DOCUMENTS_FIELD -> {
                        XContentParserUtils.ensureExpectedToken(
                            XContentParser.Token.START_ARRAY,
                            xcp.currentToken(),
                            xcp
                        )
                        while (xcp.nextToken() != XContentParser.Token.END_ARRAY) {
                            val document = IdDocPair.parse(xcp)
                            documents.add(document)
                        }
                    }

                    else -> {
                        xcp.skipChildren()
                    }
                }
            }

            return RunWorkflowRequest(
                requireNotNull(workflowId) { "workflowId is null" },
                documents
            )
        }
    }
}
