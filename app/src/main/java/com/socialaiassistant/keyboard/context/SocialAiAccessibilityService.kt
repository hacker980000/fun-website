package com.socialaiassistant.keyboard.context

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Handler
import android.os.SystemClock
import android.os.Looper
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.socialaiassistant.keyboard.ime.BubbleFlightBus
import com.socialaiassistant.keyboard.ime.BubbleFlightEditorToken
import com.socialaiassistant.keyboard.ime.BubbleFlightRequest
import com.socialaiassistant.keyboard.ime.BubbleFlightSink
import com.socialaiassistant.keyboard.ime.BubbleFlightTarget
import com.socialaiassistant.keyboard.ime.BubbleFlightTargetResolver
import com.socialaiassistant.keyboard.ime.BubbleFlightTargetSource
import com.socialaiassistant.keyboard.ime.ImeSessionRegistry
import com.socialaiassistant.keyboard.safety.EditorDescriptor
import com.socialaiassistant.keyboard.safety.FieldSafety
import com.socialaiassistant.keyboard.safety.SensitiveFieldPolicy
import java.util.ArrayDeque

class SocialAiAccessibilityService : AccessibilityService(), BubbleFlightSink {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val adapter = GenericConversationAdapter()
    private val platformAdapters = PlatformContextAdapterRegistry.default()
    private val pendingByPackage = mutableMapOf<String, Runnable>()
    private val bubbleTargetResolver = BubbleFlightTargetResolver()
    private val sensitiveFieldPolicy = SensitiveFieldPolicy()
    private var latestBubbleEditableTarget: BubbleFlightTarget? = null
    private var bubbleOverlayRenderer: BubbleFlightOverlayRenderer? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        bubbleOverlayRenderer?.release()
        bubbleOverlayRenderer = BubbleFlightOverlayRenderer(AccessibilityBubbleOverlayWindowHost(this))
        BubbleFlightBus.register(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val safeEvent = event ?: return
        if (!SUPPORTED_EVENTS.contains(safeEvent.eventType)) return

        val packageName = safeEvent.packageName?.toString().orEmpty()
        if (packageName.isBlank() || packageName == this.packageName) return

        if (SENSITIVE_FIELD_EVENTS.contains(safeEvent.eventType)) {
            updateFocusedFieldRestriction(safeEvent, packageName)
        }
        updateBubbleEditableTarget(safeEvent, packageName)

        if (!ContextAccessGate.allowed.value) {
            ContextSnapshotBus.clear()
            return
        }

        val imeSession = ImeSessionRegistry.session.value
        if (imeSession == null || imeSession.safety != FieldSafety.ALLOW_AI) {
            ContextSnapshotBus.clear()
            return
        }
        if (imeSession.packageName != packageName) return
        if (!platformAdapters.supportsPackage(packageName)) {
            cancelPendingContextExtractions()
            ContextSnapshotBus.clear()
            return
        }

        val windowSignature = "${safeEvent.windowId}:${safeEvent.className ?: "window"}"
        val conversationHint = safeEvent.contentDescription?.toString()

        // A window transition can represent moving to a different thread inside the same app.
        // Clear the old snapshot immediately instead of allowing a short debounce window where
        // the user could request AI against the previous conversation.
        if (safeEvent.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            ContextSnapshotBus.clear()
        }
        debounceExtract(packageName, windowSignature, conversationHint)
    }

    override fun onInterrupt() {
        cancelPendingContextExtractions()
        cancelAll()
        ContextSnapshotBus.clear()
    }

    private fun cancelPendingContextExtractions() {
        pendingByPackage.values.forEach(mainHandler::removeCallbacks)
        pendingByPackage.clear()
    }

    override fun onDestroy() {
        BubbleFlightBus.unregister(this)
        bubbleOverlayRenderer?.release()
        bubbleOverlayRenderer = null
        latestBubbleEditableTarget = null
        onInterrupt()
        super.onDestroy()
    }

    override fun submit(request: BubbleFlightRequest): Boolean {
        val activeEditor = activeBubbleEditorToken() ?: return false
        if (activeEditor != request.editor) return false
        val session = ImeSessionRegistry.session.value ?: return false
        if (session.safety == FieldSafety.BLOCK_AI) return false

        val target = bubbleTargetResolver.resolve(
            editor = request.editor,
            exact = request.exactTarget,
            fallback = latestBubbleEditableTarget,
            nowUptimeMs = SystemClock.uptimeMillis()
        ) ?: return false
        return bubbleOverlayRenderer?.show(request, target) == true
    }

    override fun retarget(flightId: Long, target: BubbleFlightTarget): Boolean {
        val activeEditor = activeBubbleEditorToken() ?: return false
        if (activeEditor != target.editor) return false
        return bubbleOverlayRenderer?.retarget(flightId, target) == true
    }

    override fun cancelEditor(editor: BubbleFlightEditorToken) {
        bubbleOverlayRenderer?.cancelEditor(editor)
        if (latestBubbleEditableTarget?.editor == editor) latestBubbleEditableTarget = null
    }

    override fun cancelAll() {
        bubbleOverlayRenderer?.cancelAll()
        latestBubbleEditableTarget = null
    }

    private fun activeBubbleEditorToken(): BubbleFlightEditorToken? {
        val session = ImeSessionRegistry.session.value ?: return null
        return BubbleFlightEditorToken(session.packageName, session.fieldId, session.generation)
    }

    private fun updateFocusedFieldRestriction(event: AccessibilityEvent, packageName: String) {
        val session = ImeSessionRegistry.session.value ?: return
        if (session.packageName != packageName) return

        val focused = event.source?.takeIf { node -> node.isEditable && node.isFocused }
            ?: rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: return
        if (!focused.isEditable) return

        val descriptor = EditorDescriptor(
            inputType = focused.inputType,
            packageName = packageName,
            hintText = focused.hintText?.toString(),
            accessibilityPassword = focused.isPassword,
            labelText = focused.contentDescription?.toString(),
            fieldName = focused.viewIdResourceName,
            editorMetadataHints = listOfNotNull(focused.className?.toString())
        )
        val restriction = sensitiveFieldPolicy.evaluate(descriptor)

        // Some virtual/Compose editors expose inputType=0 through Accessibility. Do not downgrade
        // those to NO_CONVERSATION solely because metadata is incomplete; BLOCK_AI signals remain
        // authoritative (password flag / OTP / PIN / CVV / credential semantics).
        if (restriction == FieldSafety.NO_CONVERSATION && focused.inputType == 0) return
        if (restriction != FieldSafety.ALLOW_AI && ImeSessionRegistry.restrictSafety(packageName, restriction)) {
            cancelPendingContextExtractions()
            ContextSnapshotBus.clear()
        }
    }

    private fun updateBubbleEditableTarget(event: AccessibilityEvent, packageName: String) {
        val session = ImeSessionRegistry.session.value
        if (session == null || session.packageName != packageName || session.safety == FieldSafety.BLOCK_AI) {
            latestBubbleEditableTarget = null
            return
        }

        val eventSource = event.source
        val node = when {
            eventSource?.isEditable == true -> eventSource
            else -> rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        }
        if (node == null || !node.isEditable || node.isPassword) {
            latestBubbleEditableTarget = null
            return
        }

        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        val rtl = resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
        val point = BubbleAccessibilityTargetMapper.pointInside(bounds, rtl, dp(16f))
        if (point == null) {
            latestBubbleEditableTarget = null
            return
        }

        latestBubbleEditableTarget = BubbleFlightTarget(
            point = point,
            editor = BubbleFlightEditorToken(session.packageName, session.fieldId, session.generation),
            capturedAtUptimeMs = SystemClock.uptimeMillis(),
            source = BubbleFlightTargetSource.ACCESSIBILITY_BOUNDS
        )
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private fun debounceExtract(
        packageName: String,
        windowSignature: String,
        conversationHint: String?
    ) {
        pendingByPackage.remove(packageName)?.let(mainHandler::removeCallbacks)
        val task = Runnable {
            pendingByPackage.remove(packageName)
            extractCurrentWindow(packageName, windowSignature, conversationHint)
        }
        pendingByPackage[packageName] = task
        mainHandler.postDelayed(task, DEBOUNCE_MS)
    }

    private fun extractCurrentWindow(
        packageName: String,
        windowSignature: String,
        conversationHint: String?
    ) {
        if (!ContextAccessGate.allowed.value) return
        val session = ImeSessionRegistry.session.value ?: return
        if (session.safety != FieldSafety.ALLOW_AI || session.packageName != packageName) return
        if (!platformAdapters.supportsPackage(packageName)) {
            ContextSnapshotBus.clear()
            return
        }
        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != packageName) return

        val nodes = ArrayList<VisibleTextNode>()
        var composerHint: String? = null
        var sawEditable = false
        val queue = ArrayDeque<NodeFrame>()
        queue.add(NodeFrame(root, depth = 0))
        var visited = 0
        val screenWidth = resources.displayMetrics.widthPixels.coerceAtLeast(1)
        val isRtlLayout = resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL

        while (queue.isNotEmpty() && visited < MAX_NODES) {
            val frame = queue.removeFirst()
            val node = frame.node
            visited++

            if (node.isPassword) continue

            val text = node.text?.toString()?.trim().orEmpty()
            val description = node.contentDescription?.toString()?.trim().orEmpty()
            val candidate = when {
                text.isNotEmpty() -> text
                description.isNotEmpty() && !node.isEditable -> description
                else -> ""
            }

            if (node.isEditable) {
                sawEditable = true
                if (composerHint.isNullOrBlank()) {
                    composerHint = node.hintText?.toString()?.trim()
                        ?: node.contentDescription?.toString()?.trim()
                }
            } else if (candidate.isNotBlank()) {
                val bounds = Rect()
                node.getBoundsInScreen(bounds)
                val visible = VisibleTextNode(
                    order = nodes.size,
                    text = candidate,
                    senderHint = SenderClass.UNKNOWN,
                    editable = false,
                    control = looksLikeControl(candidate),
                    contentDescription = description.ifBlank { null },
                    viewIdResourceName = node.viewIdResourceName,
                    className = node.className?.toString(),
                    clickable = node.isClickable,
                    screenLeft = bounds.left,
                    screenTop = bounds.top,
                    screenRight = bounds.right,
                    screenBottom = bounds.bottom
                )
                nodes += visible.copy(
                    senderHint = ConversationSenderClassifier.infer(
                        node = visible,
                        screenWidth = screenWidth,
                        isRtlLayout = isRtlLayout
                    )
                )
            }

            if (frame.depth < MAX_DEPTH) {
                for (index in 0 until node.childCount) {
                    node.getChild(index)?.let { child ->
                        queue.addLast(NodeFrame(child, frame.depth + 1))
                    }
                }
            }
        }

        if (!sawEditable) return

        val resolvedHint = ConversationHintResolver.resolve(
            explicitHint = conversationHint,
            windowSignature = windowSignature,
            nodes = nodes
        )
        val platform = platformAdapters.adapt(
            PlatformContextInput(
                packageName = packageName,
                windowSignature = windowSignature,
                conversationHint = resolvedHint,
                composerHint = composerHint,
                nodes = nodes
            )
        )

        adapter.fromNodes(
            packageName = packageName,
            windowSignature = windowSignature,
            conversationHint = platform.conversationHint ?: resolvedHint,
            nodes = platform.nodes,
            composerHint = composerHint,
            surface = platform.surface,
            confidenceBoost = platform.confidenceBoost
        )?.let(ContextSnapshotBus::publish)
    }

    private fun looksLikeControl(value: String): Boolean =
        value.length <= 3 && value.none { it.isLetterOrDigit() }

    private data class NodeFrame(val node: AccessibilityNodeInfo, val depth: Int)

    private companion object {
        const val MAX_NODES = 250
        const val MAX_DEPTH = 8
        const val DEBOUNCE_MS = 180L

        val SUPPORTED_EVENTS = setOf(
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED,
            AccessibilityEvent.TYPE_VIEW_FOCUSED
        )
        val SENSITIVE_FIELD_EVENTS = setOf(
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_FOCUSED
        )
    }
}
