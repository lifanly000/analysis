package com.tony.analysis.lifecycle


import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import com.tony.analysis.Tracker
import com.tony.analysis.utils.getTrackName
import com.tony.analysis.utils.getTrackProperties
import com.tony.analysis.utils.getTrackTitle
import java.lang.ref.WeakReference

/**
 *
 * 该类用于监听所有Fragment的生命周期<p/>
 *
 * Created by Tony on 2018/9/18.
 */

class TrackerFragmentLifeCycle : FragmentManager.FragmentLifecycleCallbacks(),ITrackerFragmentVisible{

    private val refs = ArrayList<WeakReference<Fragment>>()

    private var startTime = System.currentTimeMillis()
    private var endTime = System.currentTimeMillis()


    override fun onFragmentResumed(fm: FragmentManager?, f: Fragment?) {
        startTime = System.currentTimeMillis()
        if (f != null) {
            refs.add(WeakReference(f))
        }
//        f?.let {
//            if (isAncestorVisible(f) && !isParentFragment(f) && isVisible(f)) {
//                // 如果父Fragment可见
//                // 并且本身不是父Fragment
//                // 并且本身可见，则进行统计
//                track(f)
//            }
//        }
    }



    override fun onFragmentPaused(fm: android.support.v4.app.FragmentManager?, f: Fragment?) {
        // 在Fragment不可见时对应的移除该Fragment
        endTime = System.currentTimeMillis()
        f?.let {
            track(f)
        }
        for (ref in refs) {
            if (ref.get() == f) {
                refs.remove(ref)
                break
            }
        }
    }




    override fun onFragmentVisibilityChanged(visible: Boolean, f: Fragment?) {
        if (visible) {
            f?.let {
                // 由于内嵌的Fragment不会触发onHiddenChange()和setUserVisibleHint()方法，故此处只能根据其父Fragment来判断
                findVisibleChildren(f).forEach {
                    track(it)
                }
            }
        }
    }

    /**
     * 根据一个Fragment，从[refs]中查找其所有的子Fragment/子孙Fragment
     * @param parent 要查找的父/祖先Fragment
     * @return 查找到的Fragment，如果不存在Fragment，则返回的列表元素数量为0
     */
    private fun findVisibleChildren(parent: Fragment): List<Fragment> {
        val children = ArrayList<Fragment>()
        refs.filter {
            // 首先过滤掉已经被忽略的Fragment
            val child = it.get()
            !(child is ITrackerIgnore && child.isIgnored())
        }.filter {
            // 此处用于过滤掉父Fragment不符的Fragment
            val child = it.get()
            child != null && checkParent(child, parent)
        }.filter {
            // 此处用于过滤掉不可见的Fragment
            val child = it.get()
            child != null && !child.isHidden && child.userVisibleHint && isAncestorVisible(child)
        }.forEach {
            val child = it.get()
            child?.let { children.add(child) }
        }

        // 如果没有符合需要的children，则其自身就为符合需要的Fragment
        if (children.isEmpty() && !isParentFragment(parent)) {
            children.add(parent)
        }

        return children
    }


    /**
     * 判断是否为其他Fragment的父级（实现了[IFragment]接口，并且[ITrackerIgnore.isIgnored]的值为true）
     * @param f 需要检查的Fragment
     * @return 在[f]实现了[ITrackerIgnore]接口，并且[ITrackerIgnore.isIgnored]值为true时，返回true；其他情况下返回false
     */
    private fun isParentFragment(f: Fragment): Boolean = f is ITrackerIgnore && f.isIgnored()


    /**
     * 检查一个[parent]是否是[child]的父Fragment/祖先Fragment
     */
    private fun checkParent(child: Fragment, parent: Fragment): Boolean {
        val parentFragment = child.parentFragment
        return if (parentFragment != null) {
            if (parentFragment == parent) {// 如果是父Fragment，则直接返回true
                true
            } else {// 如果不是父Fragment，并且还存在祖先Fragment，则进入递归
                checkParent(parentFragment, parent)
            }
        } else {// 如果不存在父Fragment，则直接返回false
            false
        }
    }

    /**
     * 检查一个Fragment的祖先是否都可见
     * @param f 要检查的Fragment
     * @return 如果祖先都可见则返回true；如果不存在祖先（其直接宿主为Activity），则返回true；否则返回false
     */
    private fun isAncestorVisible(f: Fragment): Boolean {
        val parent = f.parentFragment
        return if (parent == null) {
            true
        } else if (!parent.isHidden && parent.userVisibleHint) {
            isAncestorVisible(parent)
        } else {
            false
        }
    }



    private fun track(f: Fragment) {
        if (f is ITrackerIgnore) {
            if (!f.isIgnored()) {
                trackImpl(f)
            }
        } else {
            trackImpl(f)
        }
    }

    private fun trackImpl(f: Fragment) {
        val screenName = f.getTrackName()
        Tracker.referer = Tracker.screenName
        Tracker.refererClass = Tracker.screenClass
        Tracker.screenName = screenName
        Tracker.screenClass = f.javaClass.canonicalName
        Tracker.screenTitle = f.getTrackTitle()
        Tracker.startTime = startTime
        Tracker.endTime = System.currentTimeMillis()

        var parentAlias = ""
        var parent = ""
        val parentFragment = f.parentFragment
        if (parentFragment != null) {
            parentAlias = parentFragment.getTrackName()
            parent = parentFragment.javaClass.canonicalName
        } else {
            val activity = f.activity
            if (activity != null) {
                parentAlias = activity.getTrackName()
                parent = activity.javaClass.canonicalName
            }
        }
        Tracker.parent = parentAlias
        Tracker.parentClass = parent
        Tracker.trackScreen(f.getTrackProperties())
    }
}
