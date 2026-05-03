package com.example.mynote.view.fragments.bottomSheets

import android.content.Context
import android.content.DialogInterface
import android.content.res.Configuration
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mynote.databinding.FragmentTaskEntryDialogBinding
import com.example.mynote.utils.adjustMaxLinesBasedOnOrientation
import com.example.mynote.utils.preventBottomSheetInterceptionFully
import com.example.mynote.utils.preventBottomSheetInterceptionScrollUp
import com.example.mynote.view.adapter.SubtaskEntryBottomSheetAdapter
import com.example.mynote.view.fragments.dialogs.EnterTaskSaveConfirmationDialog
import com.example.mynote.viewmodel.FormViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

/**
 * Bottom sheet dialog for creating new tasks (both single and group tasks with subtasks).
 *
 * Supports creation of::
 * - SingleTaskItem: Standalone tasks without subtasks
 * - TaskGroupWithSubtasksItem: Group tasks with subtasks
 *
 * Features:
 * - Toggle between single task and group task mode
 * - Real-time title input with validation
 * - Dynamic subtask list management (add, edit, remove)
 * - Keyboard handling with focus management
 * - Save confirmation on cancel with unsaved changes
 * - Save/discard confirmation on close
 *
 * @see com.example.mynote.viewmodel.FormViewModel
 * @see com.example.mynote.view.adapter.SubtaskEntryBottomSheetAdapter
 */
class TaskEntryBottomSheetDialog: BottomSheetDialogFragment() {

    private val formViewModel: FormViewModel by viewModels( ownerProducer = { requireParentFragment() })
    private var onTaskSavedListener : ((String, String, List<String>) -> Unit)? = null
    private var _binding: FragmentTaskEntryDialogBinding? = null
    private val binding get() = _binding!!
    private lateinit var subtaskAdapter: SubtaskEntryBottomSheetAdapter
    private lateinit var singleTaskTitleTextWatcher: TextWatcher
    private lateinit var groupTaskTitleTextWatcher: TextWatcher

    companion object {
        const val TAG = "TaskEntryBottomSheet"

        fun newInstance(): TaskEntryBottomSheetDialog {
            return TaskEntryBottomSheetDialog()
        }
    }

    fun setOnTaskSavedListener(listener: ((String, String, List<String>) -> Unit)?) {
        this.onTaskSavedListener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskEntryDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateSystemBars()
        setupFragmentResultListener()
        setupSubtaskRecyclerView()
        setupListeners()
        setupKeyboardBehavior()
        setupObservers()
        setupEditTextListener()
        setupEditTextScroll()
        setupEditTextConfigureMaxLines()

        formViewModel.updateIsOpenEnterBottomSheet(true)
    }

    override fun onStart() {
        super.onStart()

        setUpBottomSheetBehavior {
            handleCancelClick()
        }
    }

    override fun onDestroyView() {
        onTaskSavedListener = null
        (binding.subtaskRecyclerView.adapter as? SubtaskEntryBottomSheetAdapter)?.cleanUp()
        binding.subtaskRecyclerView.adapter = null
        binding.etTaskDescription.removeTextChangedListener(singleTaskTitleTextWatcher)
        binding.etGroupDescription.removeTextChangedListener(groupTaskTitleTextWatcher)
        _binding = null
        super.onDestroyView()
    }

    override fun onDismiss(dialog: DialogInterface) {
        hideKeyboard()
        super.onDismiss(dialog)
    }

    override fun onCancel(dialog: DialogInterface) {
        hideKeyboard()
        super.onCancel(dialog)
    }

    private fun setupObservers(){

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                formViewModel.isGroupMode.collect { isGroupMode ->
                    if (isGroupMode) {

                        updateSubtaskVisibility(isGroupMode)
                        binding.etGroupDescription.requestFocus()
                        showKeyboard(binding.etGroupDescription)
                    } else {

                        updateSubtaskVisibility(isGroupMode)
                        binding.etTaskDescription.requestFocus()
                        showKeyboard(binding.etTaskDescription)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                formViewModel.subtasks.collect { subtasks ->

                    subtaskAdapter.submitList(subtasks)
                }
            }
        }
    }

    private fun setupEditTextListener(){

        singleTaskTitleTextWatcher = object : TextWatcher {
            var previousSingleTitle = formViewModel.singleTitle

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                val newText = s?.toString() ?: ""
                if (previousSingleTitle != newText){
                    previousSingleTitle = newText
                    formViewModel.updateSingleTitle(newText)
                }
            }
        }

        binding.etTaskDescription.addTextChangedListener(singleTaskTitleTextWatcher)

        groupTaskTitleTextWatcher = object : TextWatcher {
            var previousGroupTitle = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                val newText = s?.toString() ?: ""
                if (previousGroupTitle != newText){
                    previousGroupTitle = newText
                    formViewModel.updateGroupTitle(newText)
                }
            }
        }

        binding.etGroupDescription.addTextChangedListener(groupTaskTitleTextWatcher)
    }

    private fun setupSubtaskRecyclerView() {
        subtaskAdapter = SubtaskEntryBottomSheetAdapter(
            onSubtaskRemoved = { position ->
                subtaskAdapter.updateFocusedEditTextMapAfterDelete(position)
                formViewModel.removeSubtask(position)
                subtaskAdapter.notifyItemRemoved(position)
                subtaskAdapter.updateFocusedAfterDelete(position)
            },
            onSubtaskChanged = { position, newText ->
                formViewModel.updateEntrySubtask(position, newText)
            },
            onSubtaskAdd = { position, newText ->
                addEmptySubtaskField()
            }
        )

        binding.subtaskRecyclerView.apply {
            adapter = subtaskAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupListeners() {
        binding.etTaskDescription.requestFocus()
        showKeyboard(binding.etTaskDescription)

        // Group mode toggle
        binding.switchIsGroup.setOnCheckedChangeListener { _, isChecked ->
            formViewModel.updateGroupMode(isChecked)
        }

        // Add new empty subtask
        binding.addSubtaskButton.setOnClickListener {
            addEmptySubtaskField()
        }

        // Save task
        binding.saveButton.setOnClickListener {
            saveTask()
            formViewModel.updateIsOpenEnterBottomSheet(false)
        }

        // Cancel
        binding.cancelButton.setOnClickListener {
            handleCancelClick()
        }

        dialog?.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {

                // Prevent standard processing, the event is processed, the dialog does not close
                handleCancelClick()
                true
            } else {
                false
            }
        }
    }

    private fun setupKeyboardBehavior() {
        binding.etGroupDescription.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER &&
                event.action == KeyEvent.ACTION_DOWN
            ) {
                addEmptySubtaskField()
                return@setOnKeyListener true
            }
            false
        }

        binding.etGroupDescription.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_NEXT-> {
                    addEmptySubtaskField()
                    return@setOnEditorActionListener true
                }
                else -> false
            }
        }
    }

    private fun addEmptySubtaskField() {

        formViewModel.addSubtask()
        val subtasksListSize = formViewModel.subtasks.value.size
        val newPosition = subtasksListSize - 1
        subtaskAdapter.notifyItemInserted(subtasksListSize - 1)

        // Scroll to bottom and focus on new field
        binding.subtaskRecyclerView.post {
            binding.subtaskRecyclerView.smoothScrollToPosition(subtasksListSize - 1)

            binding.subtaskRecyclerView.postDelayed({
                val viewHolder = binding.subtaskRecyclerView
                    .findViewHolderForAdapterPosition(newPosition)

                viewHolder?.let { holder ->
                    if (holder is SubtaskEntryBottomSheetAdapter.SubtaskViewHolder) {
                        holder.etSubtask.requestFocus()
                    }
                }
            }, 150)
        }
    }

    private fun updateSubtaskVisibility(isGroupMode: Boolean) {
        if (isGroupMode) {
            binding.groupSubtasksLinearLayout.visibility = View.VISIBLE
            binding.etGroupDescription.visibility = View.VISIBLE
            binding.etTaskDescription.visibility = View.GONE
            binding.etTaskDescriptionTextInputLayout.visibility = View.GONE
            binding.etTaskDescription.text?.clear()
        } else {
            binding.groupSubtasksLinearLayout.visibility = View.GONE
            binding.etGroupDescription.text?.clear()
            binding.etGroupDescription.visibility = View.GONE
            formViewModel.clearSubtasks()
            subtaskAdapter.notifyDataSetChanged()

            binding.etTaskDescription.visibility = View.VISIBLE
            binding.etTaskDescriptionTextInputLayout.visibility = View.VISIBLE
        }
    }

    private fun saveTask() {

        val isGroupMode = formViewModel.isGroupMode.value

        if (isGroupMode) {
            val groupDesc = formViewModel.groupTitle.let {
                if (it.isNullOrEmpty()) " "
                else it.trim()
            }

            val subtaskDesc = formViewModel.subtasks.value.filter { it.isNotEmpty() }

            onTaskSavedListener?.invoke("", groupDesc, subtaskDesc)
            hideKeyboard()
            dismiss()
            formViewModel.clearState()
        } else {
            val singleTaskDesc = formViewModel.singleTitle.trim()
            if (singleTaskDesc.isEmpty()) {
                binding.etTaskDescription.error = "Title is required"
                return
            }

            onTaskSavedListener?.invoke(singleTaskDesc, "", emptyList())
            hideKeyboard()
            dismiss()
            formViewModel.clearState()
        }
    }

    private fun handleCancelClick(){
        val isGroupMode = formViewModel.isGroupMode.value
        val taskForm = formViewModel.taskDialogUiState.value
        val subtasks = formViewModel.subtasks.value

        if (isGroupMode) {

            val taskGroupDesc = taskForm.groupTitle ?: ""
            if (taskGroupDesc.isBlank() && subtasks.isEmpty()) {
                hideKeyboard()
                dismiss()
                formViewModel.updateIsOpenEnterBottomSheet(false)
            } else {
                showSaveDialog("", taskGroupDesc, isGroupMode, subtasks)
            }
        } else {
            val taskDesc = taskForm.singleTitle ?: ""

            if (taskDesc.isBlank()) {
                hideKeyboard()
                dismiss()
                formViewModel.updateIsOpenEnterBottomSheet(false)
            } else {
                showSaveDialog(taskDesc, "", isGroupMode, emptyList())
            }
        }
    }

    private fun showKeyboard(editText: EditText) {
        editText.postDelayed({
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE)
                    as InputMethodManager
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        }, 100)
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE)
                as InputMethodManager
        val currentFocus = requireDialog().currentFocus
        currentFocus?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    private fun setupFragmentResultListener() {
        childFragmentManager.setFragmentResultListener(
            EnterTaskSaveConfirmationDialog.Companion.REQUEST_KEY,
            viewLifecycleOwner
        ) { requestKey, bundle ->
            when (bundle.getString("action")) {
                "save" -> {
                    val taskDesc = bundle.getString("taskDesc", "")
                    val taskGroupDesc = bundle.getString("taskGroupDesc", "")
                    val isGroupMode = bundle.getBoolean("isGroupMode", false)
                    val subtasks = bundle.getStringArrayList("subtasks") ?: emptyList()

                    onTaskSavedListener?.invoke(taskDesc, taskGroupDesc, subtasks)
                    formViewModel.updateIsOpenEnterBottomSheet(false)
                    dismiss()
                }
                "discard" -> {
                    formViewModel.updateIsOpenEnterBottomSheet(false)
                    dismiss()
                }
                "cancel" ->{
                    if (formViewModel.isGroupMode.value){
                        binding.etGroupDescription.requestFocus()
                        showKeyboard(binding.etGroupDescription)
                    }
                    else{
                        binding.etTaskDescription.requestFocus()
                        showKeyboard(binding.etTaskDescription)
                    }
                }
            }
        }
    }

    private fun showSaveDialog(
        taskDesc: String,
        taskGroupDesc: String,
        isGroupMode: Boolean,
        listSubtasks: List<String>
    ) {
        val dialog = EnterTaskSaveConfirmationDialog.Companion.newInstance(
            taskDesc = taskDesc,
            taskGroupDesc = taskGroupDesc,
            isGroupMode = isGroupMode,
            subtasks = listSubtasks
        )
        hideKeyboard()
        dialog.show(childFragmentManager, "save_confirmation")
    }

    /**
     * Prevents the BottomSheet from collapsing when the user scrolls inside an EditText.
     *
     * - Single task description: blocks BottomSheet only when scrolling UP.
     * - Group task description: blocks BottomSheet in both directions.
     *
     * @see preventBottomSheetInterceptionScrollUp
     * @see preventBottomSheetInterceptionFully
     */
    private fun setupEditTextScroll() {
        val editTextSingleDesc = binding.etTaskDescription
        val editTextGroupDesc = binding.etGroupDescription
        editTextSingleDesc.preventBottomSheetInterceptionScrollUp()
        editTextGroupDesc.preventBottomSheetInterceptionFully()
    }

    /**
     * Adjusts the maximum number of lines for an EditText based on screen orientation.
     *
     * @see adjustMaxLinesBasedOnOrientation
    */
    private fun setupEditTextConfigureMaxLines(){
        binding.etGroupDescription.adjustMaxLinesBasedOnOrientation(landscapeMaxLines = 1)
    }
}