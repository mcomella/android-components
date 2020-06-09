/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.tabstray

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.concept.tabstray.Tabs
import mozilla.components.concept.tabstray.TabsTray
import mozilla.components.support.base.observer.Observable
import mozilla.components.support.base.observer.ObserverRegistry

/**
 * Function responsible for creating a `TabViewHolder` in the `TabsAdapter`.
 */
typealias ViewHolderProvider = (ViewGroup, BrowserTabsTray) -> TabViewHolder

object Timings {
    var tabsStart = -1L

    var tabsTrayEnd = -1L
    var tabsTrayTabsEnd = -1L // loaded async

    val trayDuration get() = tabsTrayEnd - tabsStart
    val trayTabsDuration get() = tabsTrayTabsEnd - tabsStart
}

/**
 * RecyclerView adapter implementation to display a list/grid of tabs.
 * @param delegate TabsTray.Observer registry to allow `TabsAdapter` to conform to `Observable<TabsTray.Observer>`.
 * @param viewHolderProvider a function that creates a `TabViewHolder`.
 */
@Suppress("TooManyFunctions")
open class TabsAdapter(
    delegate: Observable<TabsTray.Observer> = ObserverRegistry(),
    private val viewHolderProvider: ViewHolderProvider = { parent, tabsTray ->
        DefaultTabViewHolder(
                LayoutInflater.from(parent.context).inflate(
                        R.layout.mozac_browser_tabstray_item,
                        parent,
                        false),
                tabsTray
        )
    }
) : RecyclerView.Adapter<TabViewHolder>(),
    TabsTray,
    Observable<TabsTray.Observer> by delegate {

    internal lateinit var tabsTray: BrowserTabsTray

    private var tabs: Tabs? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder {
        return viewHolderProvider.invoke(parent, tabsTray)
    }

    override fun getItemCount() = tabs?.list?.size ?: 0

    var listenerAdded = false
    var frame = 1

    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
        val tabs = tabs ?: return

        holder.bind(tabs.list[position], position == tabs.selectedIndex, this)

        Log.e("lol", "onBind")
        if (!listenerAdded) {
            listenerAdded = true
            holder.itemView.viewTreeObserver.addOnPreDrawListener {
                if (frame == 4) {
                    // On the 4th frame with animations enabled, the tabs in the tray start to fade in:
                    // tested with 1 tab & 8 tabs.
                    Handler(Looper.getMainLooper()).post {
                        Timings.tabsTrayTabsEnd = SystemClock.elapsedRealtime()
                        Log.e("lol tabsEnd", "average ${Timings.trayTabsDuration}")
//                        Thread.sleep(5000)
                    }
                    // If this code became permanent, we should remove the listener.
                }
                frame++
                true
            }
        }
    }

    override fun updateTabs(tabs: Tabs) {
        this.tabs = tabs
    }

    override fun onTabsInserted(position: Int, count: Int) = notifyItemRangeInserted(position, count)

    override fun onTabsRemoved(position: Int, count: Int) = notifyItemRangeRemoved(position, count)

    override fun onTabsMoved(fromPosition: Int, toPosition: Int) = notifyItemMoved(fromPosition, toPosition)

    override fun onTabsChanged(position: Int, count: Int) = notifyItemRangeChanged(position, count)
}
