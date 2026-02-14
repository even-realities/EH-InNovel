package com.fzfstudio.eh.innovel.models

/**
 * Timer duration presets available to the user.
 */
enum class TimerPreset(val label: String, val seconds: Int) {
    FIVE_MIN("5 min", 5 * 60),
    FIFTEEN_MIN("15 min", 15 * 60),
    THIRTY_MIN("30 min", 30 * 60);
}
