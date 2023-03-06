package com.kt.apps.core.base

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.transition.TransitionHelper
import androidx.leanback.widget.*
import com.kt.apps.core.R
import com.kt.apps.core.base.logging.Logger
import com.kt.apps.core.databinding.DefaultGridFragmentBinding

abstract class BaseGridViewFragment<T : ViewDataBinding> : Fragment(),
    BrowseSupportFragment.MainFragmentAdapterProvider {
    abstract val layoutRes: Int
    protected var mAdapter: ObjectAdapter? = null
    abstract val mGridPresenter: VerticalGridPresenter
    abstract val mOnItemViewSelectedListener: OnItemViewSelectedListener
    abstract val mOnItemViewClickedListener: OnItemViewClickedListener
    protected lateinit var binding: T
    protected var mGridViewHolder: VerticalGridPresenter.ViewHolder? = null
    protected var mSceneAfterEntranceTransition: Any? = null
    protected var mSelectedPosition = -1

    private val mMainFragmentAdapter by lazy {
        object : BrowseSupportFragment.MainFragmentAdapter<Fragment>(this@BaseGridViewFragment) {
            override fun setEntranceTransitionState(state: Boolean) {
                super.setEntranceTransitionState(state)
            }
        }
    }

    private val mViewSelectedListener by lazy {
        OnItemViewSelectedListener { itemViewHolder, item, rowViewHolder, row ->
            val position: Int = mGridViewHolder?.gridView?.selectedPosition ?: -1
            Logger.d(this, message = "Selected position: $position")
            gridOnItemSelected(position)
            mOnItemViewSelectedListener.onItemSelected(
                itemViewHolder, item,
                rowViewHolder, row
            )
        }
    }

    private val mChildLaidOutListener by lazy {
        OnChildLaidOutListener { _, _, position, _ ->
            if (position == 0) {
                showOrHideTitle()
            }
        }
    }

    abstract fun onCreateAdapter()
    abstract fun initView(rootView: View)
    abstract fun initAction(rootView: View)


    private fun gridOnItemSelected(position: Int) {
        if (position != mSelectedPosition) {
            mSelectedPosition = position
            showOrHideTitle()
        }
    }

    private fun showOrHideTitle() {
        mGridViewHolder?.gridView
            ?.findViewHolderForAdapterPosition(mSelectedPosition)
            ?: return
        if (!mGridViewHolder!!.gridView.hasPreviousViewInSameRow(mSelectedPosition)) {
            mMainFragmentAdapter.fragmentHost.showTitleView(true)
        } else {
            mMainFragmentAdapter.fragmentHost.showTitleView(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onCreateAdapter()
        mainFragmentAdapter.fragmentHost.notifyDataReady(mMainFragmentAdapter)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            layoutRes,
            container,
            false
        )
        initView(binding.root)
        return binding.root
    }

    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val gridDock: ViewGroup = view.findViewById(R.id.browse_grid_dock)
        mGridViewHolder = mGridPresenter.onCreateViewHolder(gridDock)
        gridDock.addView(mGridViewHolder!!.view)
        mGridViewHolder!!.gridView.setOnChildLaidOutListener(mChildLaidOutListener)
        mSceneAfterEntranceTransition = TransitionHelper.createScene(
            gridDock
        ) { setEntranceTransitionState(true) }
        mainFragmentAdapter.fragmentHost.notifyViewCreated(mMainFragmentAdapter)
        updateAdapter()
        initAction(view)
    }

    protected fun updateAdapter() {
        if (mGridViewHolder != null) {
            mGridPresenter.onBindViewHolder(mGridViewHolder, mAdapter)
            if (mSelectedPosition != -1) {
                mGridViewHolder!!.gridView.selectedPosition = mSelectedPosition
            }
        }
    }

    open fun setEntranceTransitionState(afterTransition: Boolean) {
        mGridPresenter.setEntranceTransitionState(mGridViewHolder, afterTransition)
    }


    override fun getMainFragmentAdapter(): BrowseSupportFragment.MainFragmentAdapter<*> {
        return mMainFragmentAdapter
    }

    class DefaultGridFragment : BaseGridViewFragment<DefaultGridFragmentBinding>() {
        override val layoutRes: Int
            get() = R.layout.default_grid_fragment


        override val mGridPresenter: VerticalGridPresenter
            get() = VerticalGridPresenter(FocusHighlight.ZOOM_FACTOR_MEDIUM).apply {
                this.numberOfColumns = 5
            }
        override val mOnItemViewSelectedListener: OnItemViewSelectedListener
            get() = OnItemViewSelectedListener { itemViewHolder, item, rowViewHolder, row ->

            }
        override val mOnItemViewClickedListener: OnItemViewClickedListener
            get() = OnItemViewClickedListener { itemViewHolder, item, rowViewHolder, row ->

            }


        override fun onCreateAdapter() {
            val cardPresenter = ClassPresenterSelector()
            mAdapter = ArrayObjectAdapter(cardPresenter)
            updateAdapter()
        }

        override fun initView(rootView: View) {
        }

        override fun initAction(rootView: View) {
        }
    }


}