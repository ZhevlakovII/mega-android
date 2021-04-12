package mega.privacy.android.app.meeting.fragments

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.zhpan.bannerview.constants.IndicatorGravity
import com.zhpan.indicator.enums.IndicatorStyle
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.GridViewCallFragmentBinding
import mega.privacy.android.app.meeting.TestTool
import mega.privacy.android.app.meeting.adapter.GridViewPagerAdapter
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.adapter.VideoGridViewAdapter
import mega.privacy.android.app.utils.Util
import kotlin.random.Random

class GridViewCallFragment : MeetingBaseFragment() {

    lateinit var viewDataBinding: GridViewCallFragmentBinding

    var maxWidth = 0

    var maxHeight = 0

    // TODO test code start
    val data: MutableList<Participant> =
        mutableListOf(Participant("Katayama Fumiki", null, "#1223ff", false, false, false, true))
    // TODO test code end

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewDataBinding = GridViewCallFragmentBinding.inflate(inflater, container, false)
        return viewDataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val display = meetingActivity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)
        maxWidth = outMetrics.widthPixels
        maxHeight = outMetrics.heightPixels

        viewDataBinding.gridViewPager
            .setScrollDuration(800)
            .setAutoPlay(false)
            .setIndicatorStyle(IndicatorStyle.CIRCLE)
            .setIndicatorSliderGap(Util.dp2px(6f))
            .setIndicatorSliderRadius(
                Util.dp2px(3f),
                Util.dp2px(3f)
            )
            .setIndicatorMargin(0, 0, 0, 170)
            .setIndicatorGravity(IndicatorGravity.CENTER)
            .setIndicatorSliderColor(
                ContextCompat.getColor(requireContext(), R.color.grey_300_grey_600),
                ContextCompat.getColor(requireContext(), R.color.white)
            )
            .setOnPageClickListener(null)
            .setAdapter(GridViewPagerAdapter(parentFragment, maxWidth, maxHeight))
            .create()
    }


    // TODO test code start
    fun loadParticipants(add: Boolean) {
        if (add) {
            // Random.nextInt(TestTool.testData().size)
//            if (data.size < 6) {
//            } else {
//                data.removeAll(data.subList(2, data.size))
//            }
            data.add(TestTool.testData()[Random.nextInt(TestTool.testData().size)])
        } else {
            if (data.size > 2) {
                // Random.nextInt(data.size)
                data.removeAt(data.size - 1)
            }
        }

        viewDataBinding.gridViewPager.refreshData(sliceBy6())
    }
    // TODO test code end

    private fun sliceBy6(): MutableList<List<Participant>> {
        val result = mutableListOf<List<Participant>>()
        val sliceCount = if (data.size % 6 == 0) data.size / 6 else data.size / 6 + 1

        for (i in 0 until sliceCount) {
            var to = i * 6 + 5
            if (to >= data.size) {
                to = data.size - 1
            }

            result.add(i, data.slice(IntRange(i * 6, to)))
        }

        return result
    }

    companion object {

        const val TAG = "GridViewCallFragment"

        @JvmStatic
        fun newInstance() = GridViewCallFragment()
    }
}