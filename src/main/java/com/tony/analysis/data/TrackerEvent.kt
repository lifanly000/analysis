package com.tony.analysis.data

import com.google.gson.annotations.SerializedName
import com.tony.analysis.Tracker
import com.tony.analysis.utils.PRETTY_GSON
import com.tony.analysis.utils.buildInObject
import com.tony.analysis.utils.buildInProperties
import com.tony.analysis.utils.getNetworkType

/**
 *  统计事件
 *
 * Created by Tony on 2018/9/18.
 */

data class TrackerEvent(
        @SerializedName("event")
        @EventType private var event: String) {

    @SerializedName("properties")
    private var properties = HashMap<String, Any>()
    @SerializedName("start_time")
    internal var start_time = Tracker.startTime

    @SerializedName("end_time")
    internal var end_time = Tracker.endTime

    @SerializedName("screenName")
    private var screenName = Tracker.screenName
    @SerializedName("screenClass")
    private var screenClass = Tracker.screenClass
    @SerializedName("referer")
    private var referer = Tracker.referer
    @SerializedName("refererClass")
    private var refererClass = Tracker.refererClass
    @SerializedName("parent")
    private var parent = Tracker.parent
    @SerializedName("parentClass")
    private var parentClass = Tracker.parentClass

    init {
        Tracker.additionalProperties.filter { it.value != null }.forEach {
            this@TrackerEvent.properties.put(it.key, it.value!!)
        }
    }

    fun addProperties(properties: Map<String, Any?>?) {
        if (properties == null) {
            return
        }
        properties.filter { it.value != null }.forEach {
            this@TrackerEvent.properties.put(it.key, it.value!!)
        }
    }

    fun build(): Map<String, Any> {
        val o = HashMap<String, Any>()
        o.putAll(buildInObject)
        o.put(EVENT, event)

        val properties = HashMap<String, Any>()
        properties.putAll(buildInProperties)
        properties.put(SCREEN_NAME, screenName)
        properties.put(SCREEN_CLASS, screenClass)
        properties.put(REFERER, referer)
        properties.put(REFERER_CLASS, refererClass)
        properties.put(PARENT, parent)
        properties.put(PARENT_CLASS, parentClass)
        properties.put(START_TIME, start_time)
        if (event == "AppClick") {
            end_time = start_time
        }
        properties.put(END_TIME, end_time)
        Tracker.trackContext?.let {
            properties.put(NETWORK_TYPE,
                    it.getApplicationContext().getNetworkType().desc())
        }

        Tracker.channelId?.let {
            properties.put(CHANNEL, it)
        }
        this@TrackerEvent.properties.let {
            properties.putAll(it)
        }

        o.put(PROPERTIES, properties)
        return o
    }

    fun toPrettyJson(): String {
        return PRETTY_GSON.toJson(build())
    }
}
