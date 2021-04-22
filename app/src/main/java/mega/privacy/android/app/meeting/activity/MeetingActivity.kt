package mega.privacy.android.app.meeting.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.*
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavGraph
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_meeting.*
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.BroadcastConstants
import mega.privacy.android.app.databinding.ActivityMeetingBinding
import mega.privacy.android.app.meeting.fragments.MeetingBaseFragment
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.IncomingCallNotification
import mega.privacy.android.app.utils.Util


// FIXME: Keep Meeting Activity from implementing this and that listeners
// FIXME: And don't directly call megaChatApi in view layer, try don't put everything together and bloat the View layer file

@AndroidEntryPoint
class MeetingActivity : BaseActivity() {

    companion object {
        /** Tne name of actions denoting set
        JOIN/CREATE/JOIN AS GUEST/In-meeting screen as the initial screen */
        const val MEETING_ACTION_JOIN = "join_meeting"
        const val MEETING_ACTION_CREATE = "create_meeting"
        const val MEETING_ACTION_GUEST = "join_meeting_as_guest"
        const val MEETING_ACTION_IN = "in_meeting"

        /** The names of the Extra data being passed to the initial fragment */
        const val MEETING_NAME = "meeting_name"
        const val MEETING_LINK = "meeting_link"
        const val MEETING_CHAT_ID = "chat_id"
    }

    private lateinit var binding: ActivityMeetingBinding
    private val meetingViewModel: MeetingActivityViewModel by viewModels()

    private val headphoneReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            meetingViewModel.sendHeadPhoneEvent()
        }
    }

    // TODO: Move to a more common place
    private fun View.setMarginTop(marginTop: Int) {
        val menuLayoutParams = this.layoutParams as ViewGroup.MarginLayoutParams
        menuLayoutParams.setMargins(0, marginTop, 0, 0)
        this.layoutParams = menuLayoutParams
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // FIXME: The Notification is responsible for its disappearance, not MeetingActivity's duty
        IncomingCallNotification.cancelIncomingCallNotification(this)

        binding = ActivityMeetingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val meetingAction = intent.action

        initReceiver()
        initActionBar(meetingAction)
        initNavigation(meetingAction)
        setStatusBarTranslucent(window, true)
    }

    private fun setStatusBarTranslucent(window: Window, translucent: Boolean) {
        val decorView: View = window.decorView

        if (translucent) {
            decorView.setOnApplyWindowInsetsListener { v: View, insets: WindowInsets? ->
                val defaultInsets = v.onApplyWindowInsets(insets)

                toolbar.setMarginTop(defaultInsets.systemWindowInsetTop)

                defaultInsets.replaceSystemWindowInsets(
                    defaultInsets.systemWindowInsetLeft,
                    0,
                    defaultInsets.systemWindowInsetRight,
                    defaultInsets.systemWindowInsetBottom
                )
            }
        } else {
            decorView.setOnApplyWindowInsetsListener(null)
        }

        ViewCompat.requestApplyInsets(decorView)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(headphoneReceiver)
    }

    /**
     * Register broadcast receiver that needed
     */
    private fun initReceiver() {
        registerReceiver(
            headphoneReceiver, IntentFilter(Constants.BROADCAST_ACTION_INTENT_HEADPHONE)
        )
    }

    /**
     * Initialize Action Bar and set icon according to param
     *
     * @param meetAction Create Meeting or Join Meeting
     */
    private fun initActionBar(meetAction: String?) {
        setSupportActionBar(binding.toolbar)
        val actionBar = supportActionBar ?: return
        actionBar.setHomeButtonEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.title = ""

        when (meetAction) {
            MEETING_ACTION_JOIN, MEETING_ACTION_CREATE, MEETING_ACTION_GUEST
            -> actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white)
            MEETING_ACTION_IN -> actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white)
        }
    }

    /**
     * Initialize Navigation and set startDestination(initial screen)
     * according to the meeting action
     *
     * @param meetingAction Create Meeting or Join Meeting
     */
    private fun initNavigation(meetingAction: String?) {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val navGraph: NavGraph =
            navHostFragment.navController.navInflater.inflate(R.navigation.meeting)

        // The args to be passed to startDestination
        val bundle = Bundle()

        if (meetingAction == MEETING_ACTION_GUEST || meetingAction == MEETING_ACTION_JOIN) {
            bundle.putString(MEETING_LINK, intent.dataString)
            bundle.putString(MEETING_NAME, intent.getStringExtra(MEETING_NAME))
        }

        navGraph.startDestination = when (meetingAction) {
            MEETING_ACTION_CREATE -> R.id.createMeetingFragment
            MEETING_ACTION_JOIN -> R.id.joinMeetingFragment
            MEETING_ACTION_GUEST -> R.id.joinMeetingAsGuestFragment
            MEETING_ACTION_IN -> R.id.inMeetingFragment
            else -> R.id.createMeetingFragment
        }

        // Remove app:navGraph="@navigation/meeting" and instead call navController.graph = navGraph
        // Change start destination dynamically
        navController.setGraph(navGraph, bundle)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Get current fragment from navHostFragment
     */
    fun getCurrentFragment(): MeetingBaseFragment? {
        val navHostFragment: Fragment? =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        return navHostFragment?.childFragmentManager?.fragments?.get(0) as MeetingBaseFragment?
    }
}
