package org.wordpress.android.ui.reader.discover.interests

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.reader_fullscreen_error_with_retry.*
import kotlinx.android.synthetic.main.reader_interests_fragment_layout.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.DoneButtonUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState.ContentUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState.ErrorUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState.InitialLoadingUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.LocaleManager
import org.wordpress.android.viewmodel.observeEvent
import org.wordpress.android.widgets.WPSnackbar
import javax.inject.Inject

class ReaderInterestsFragment : Fragment(R.layout.reader_interests_fragment_layout) {
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: ReaderInterestsViewModel
    private var parentViewModel: ReaderViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val entryPoint = requireActivity().intent.getSerializableExtra(READER_INTEREST_ENTRY_POINT) as? EntryPoint
                ?: EntryPoint.DISCOVER
        initDoneButton()
        initRetryButton()
        initBackButton(entryPoint)
        initViewModel(entryPoint)
    }

    private fun initDoneButton() {
        done_button.setOnClickListener {
            viewModel.onDoneButtonClick()
        }
    }

    private fun initRetryButton() {
        error_retry.setOnClickListener {
            viewModel.onRetryButtonClick()
        }
    }

    private fun initBackButton(entryPoint: EntryPoint) {
        if (entryPoint == EntryPoint.DISCOVER) {
            back_button.visibility = View.VISIBLE
            back_button.setOnClickListener {
                viewModel.onBackButtonClick()
            }
        }
    }

    private fun initViewModel(entryPoint: EntryPoint) {
        viewModel = ViewModelProvider(this, viewModelFactory).get(ReaderInterestsViewModel::class.java)
        if (entryPoint == EntryPoint.DISCOVER) {
            parentViewModel = ViewModelProvider(requireParentFragment()).get(ReaderViewModel::class.java)
        }
        startObserving(entryPoint)
    }

    private fun startObserving(entryPoint: EntryPoint) {
        viewModel.uiState.observe(viewLifecycleOwner, { uiState ->
            when (uiState) {
                is InitialLoadingUiState -> {
                }
                is ContentUiState -> {
                    updateInterests(uiState.interestsUiState)
                }
                is ErrorUiState -> {
                    updateErrorLayout(uiState)
                }
            }
            updateDoneButton(uiState.doneButtonUiState)
            with(uiHelpers) {
                updateVisibility(progress_bar, uiState.progressBarVisible)
                updateVisibility(title, uiState.titleVisible)
                updateVisibility(error_layout, uiState.errorLayoutVisible)
            }
        })

        viewModel.snackbarEvents.observeEvent(viewLifecycleOwner, {
                it.showSnackbar()
        })

        viewModel.closeReaderInterests.observeEvent(viewLifecycleOwner, {
            requireActivity().finish()
        })

        viewModel.start(
                LocaleManager.getLanguage(WordPress.getContext()),
                parentViewModel,
                entryPoint
        )
    }

    private fun updateDoneButton(doneButtonUiState: DoneButtonUiState) {
        with(done_button) {
            isEnabled = doneButtonUiState.enabled
            text = getString(doneButtonUiState.titleRes)
        }
        uiHelpers.updateVisibility(done_button, doneButtonUiState.visible)
    }

    private fun updateInterests(interestsUiState: List<TagUiState>) {
        interestsUiState.forEachIndexed { index, interestTagUiState ->
            val chip = interests_chip_group.findViewWithTag(interestTagUiState.slug)
                    ?: createChipView(interestTagUiState.slug, index)
            with(chip) {
                text = interestTagUiState.title
                isChecked = interestTagUiState.isChecked
            }
        }
    }

    private fun updateErrorLayout(uiState: ErrorUiState) {
        with(uiHelpers) {
            setTextOrHide(error_title, uiState.titleRes)
        }
    }

    private fun SnackbarMessageHolder.showSnackbar() {
        val snackbar = WPSnackbar.make(
                bottom_bar,
                uiHelpers.getTextOfUiString(requireContext(), this.message),
                Snackbar.LENGTH_LONG
        )
        if (this.buttonTitle != null) {
            snackbar.setAction(uiHelpers.getTextOfUiString(requireContext(), this.buttonTitle)) {
                this.buttonAction.invoke()
            }
        }
        snackbar.anchorView = bottom_bar
        snackbar.show()
    }

    private fun createChipView(slug: String, index: Int): Chip {
        val chip = layoutInflater.inflate(
                R.layout.reader_interest_filter_chip,
                interests_chip_group,
                false
        ) as Chip
        with(chip) {
            tag = slug
            setOnCheckedChangeListener { compoundButton, isChecked ->
                if (compoundButton.isPressed) {
                    viewModel.onInterestAtIndexToggled(index, isChecked)
                }
            }
            interests_chip_group.addView(chip)
        }
        return chip
    }

    companion object {
        const val TAG = "reader_interests_fragment_tag"
        const val READER_INTEREST_ENTRY_POINT = "reader_interest_entry_point"
    }

    enum class EntryPoint {
        DISCOVER,
        SETTINGS
    }
}
