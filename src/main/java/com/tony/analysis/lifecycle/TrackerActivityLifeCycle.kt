package com.tony.analysis.lifecycle

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import com.tony.analysis.Tracker
import com.tony.analysis.layout.wrap
import com.tony.analysis.utils.getTrackName
import com.tony.analysis.utils.getTrackProperties
import com.tony.analysis.utils.getTrackTitle
import java.lang.ref.WeakReference

/**
 * 该类用于监听项目中所有Activity的生命周期<p/>
 * 需要在[Application]中初始化，以便于能够及时监听所有的[Activity]
 *
 * Created by Tony on 2018/9/18.
 */
class TrackerActivityLifeCycle : Application.ActivityLifecycleCallbacks {

    private val fragmentLifeCycle = TrackerFragmentLifeCycle()
    private val refs = ArrayList<WeakReference<Activity>>()

    private var startTime = System.currentTimeMillis()
    private var endTime = System.currentTimeMillis()

    fun getFragmentLifeCycle(): TrackerFragmentLifeCycle {

        return fragmentLifeCycle
    }


    override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {
        if (activity != null) {
            wrap(activity)
        }
        if (activity is FragmentActivity) {
            activity.supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentLifeCycle, true)
        }
    }

    override fun onActivityStarted(activity: Activity?) {
        if (/*Tracker.isBackground &&*/ refs.isEmpty()) {
            // 此处仅从后台切换到前台时触发，首次触发为初始化时，防止首次触发滞后
            Tracker.onForeground()
        }
        activity?.let {
            refs.add(WeakReference(activity))
        }
    }


    override fun onActivityResumed(p0: Activity?) {
        startTime = System.currentTimeMillis()
    }


    override fun onActivityPaused(activity: Activity?) {
        endTime = System.currentTimeMillis()
        if (activity != null) {
            if (activity is ITrackerIgnore) {
                if (!activity.isIgnored()) {
                    // 内部没有Fragment，直接进行统计
                    track(activity)
                }
            } else {
                // Activity内部没有Fragment，则直接进行统计
                track(activity)
            }
        }
    }


    override fun onActivityStopped(activity: Activity?) {
        activity?.let {
            for (ref in refs) {
                if (ref.get() == activity) {
                    refs.remove(ref)
                    break
                }
            }
        }
        if (refs.isEmpty()) {
            Tracker.onBackground()
        }
    }


    override fun onActivityDestroyed(activity: Activity?) {
        if (activity is FragmentActivity) {
            activity.supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentLifeCycle)
        }
    }

    override fun onActivitySaveInstanceState(p0: Activity?, p1: Bundle?) {

    }


    private fun track(activity: Activity) {
        Tracker.referer = Tracker.screenName
        Tracker.refererClass = Tracker.screenClass
        Tracker.screenName = activity.getTrackName()
        Tracker.screenClass = activity.javaClass.canonicalName
        Tracker.screenTitle = activity.getTrackTitle()
        Tracker.parent = ""
        Tracker.startTime = startTime
        Tracker.endTime = endTime
        Tracker.parentClass = ""
        Tracker.trackScreen(activity.getTrackProperties())
    }


}