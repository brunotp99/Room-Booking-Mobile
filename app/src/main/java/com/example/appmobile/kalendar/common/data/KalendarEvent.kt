package com.himanshoe.kalendar.common.data

import org.threeten.bp.LocalDate


/**
 * [KalendarEvent] handles the event marked on any
 * @param[date] with specific
 * @param[eventName] and its
 * @param[eventDescription]
 */
data class KalendarEvent(
    val date: LocalDate,
    val eventName: String,
    val eventDescription: String? = null,
)
