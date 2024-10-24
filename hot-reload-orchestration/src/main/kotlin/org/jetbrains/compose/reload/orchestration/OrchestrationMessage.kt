package org.jetbrains.compose.reload.orchestration

import java.io.Serializable
import java.nio.file.Path
import java.util.UUID

sealed class OrchestrationMessage : Serializable {

    val messageId: UUID = UUID.randomUUID()

    data class ReloadClassesRequest(
        /**
         * Note: In case of any failure of a reload of classes, the 'pending changes'
         * will be kept by the Agent: This means, that a 'retry' is effectively just a request without
         * additional changed files
         */
        val changedClassFiles: Map<Path, ChangeType> = emptyMap()
    ) : OrchestrationMessage() {
        enum class ChangeType : Serializable {
            Modified, Added, Removed
        }
    }

    data class AgentReloadClassesResult(
        val requestId: UUID,
        val isSuccess: Boolean,
        val errorMessage: String? = null,
    ) : OrchestrationMessage()


    data class UIReloadClassesResult(
        val requestId: UUID,
        val isSuccess: Boolean,
        val errorMessage: String? = null,
    ) : OrchestrationMessage()

    data class LogMessage(
        val log: String
    ) : OrchestrationMessage()
}

