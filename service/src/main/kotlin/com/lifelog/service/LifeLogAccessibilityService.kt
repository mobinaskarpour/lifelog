package com.lifelog.service

/**
 * Backward-compatible alias. The manifest registers [MessageAccessibilityService] directly.
 */
@Deprecated("Use MessageAccessibilityService", ReplaceWith("MessageAccessibilityService"))
class LifeLogAccessibilityService : MessageAccessibilityService()
