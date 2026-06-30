package com.lifelog.service.accessibility

import android.view.accessibility.AccessibilityNodeInfo
import android.widget.EditText

object UiTreeParser {
    fun parse(root: AccessibilityNodeInfo): List<UiTextNode> {
        val nodes = mutableListOf<UiTextNode>()
        traverse(root, depth = 0, nodes)
        return nodes
    }

    fun contentHash(nodes: List<UiTextNode>): Int = nodes.joinToString(separator = "|") { "${it.top}:${it.text}" }.hashCode()

    private fun traverse(
        node: AccessibilityNodeInfo,
        depth: Int,
        out: MutableList<UiTextNode>,
    ) {
        val text = extractText(node)
        if (text != null) {
            val bounds = android.graphics.Rect()
            node.getBoundsInScreen(bounds)
            out.add(
                UiTextNode(
                    text = text,
                    className = node.className?.toString().orEmpty(),
                    depth = depth,
                    top = bounds.top,
                    bottom = bounds.bottom,
                    isEditable = node.isEditable,
                ),
            )
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                traverse(child, depth + 1, out)
            } finally {
                child.recycle()
            }
        }
    }

    private fun extractText(node: AccessibilityNodeInfo): String? {
        val candidates =
            listOfNotNull(
                node.text?.toString()?.trim(),
                node.contentDescription?.toString()?.trim(),
            ).filter { it.isNotEmpty() }
        return candidates.firstOrNull()
    }

    fun isInputField(node: UiTextNode): Boolean =
        node.isEditable ||
            node.className.contains(EditText::class.java.simpleName, ignoreCase = true)
}
