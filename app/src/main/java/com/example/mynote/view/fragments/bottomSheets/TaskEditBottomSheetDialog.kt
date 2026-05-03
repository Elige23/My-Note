package com.example.mynote.view.fragments.bottomSheets

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mynote.databinding.FragmentTaskEditDialogBinding
import com.example.mynote.model.room.TaskItem
import com.example.mynote.utils.adjustMaxLinesBasedOnOrientation
import com.example.mynote.utils.preventBottomSheetInterceptionFully
import com.example.mynote.utils.preventBottomSheetInterceptionScrollUp
import com.example.mynote.view.adapter.SubtasksEditBottomSheetAdapter
import com.example.mynote.view.fragments.dialogs.DeleteSubtaskConfirmationDialog
import com.example.mynote.view.fragments.dialogs.SaveEditTaskConfirmationDialog
import com.example.mynote.viewmodel.FormViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

/**
 * Bottom sheet dialog for editing tasks (both single and group tasks).
 *
 * Supports editing:
 * - SingleTaskItem: Standalone tasks without subtasks
 * - TaskGroupWithSubtasksItem: Group tasks with subtasks
 *
 * Features:
 * - Dynamic UI based on task type
 * - Real-time validation and change tracking
 * - Subtask management (add, edit, delete)
 * - Delete confirmation
 * - Save/discard confirmation on close
 *
 * @see com.example.mynote.viewmodel.FormViewModel
 * @see com.example.mynote.view.adapter.SubtasksEditBottomSheetAdapter
 */
class TaskEditBottomSheetDialog: BottomSheetDialogFragment() {

    private var _binding: FragmentTaskEditDialogBinding? = null
    private val binding get() = _binding!!
    private val formViewModel: FormViewModel by viewModels( ownerProducer = { requireParentFragment() })
    private val editTask: TaskItem get() = formViewModel.editTask.value
    private lateinit var subtasksAdapter: SubtasksEditBottomSheetAdapter
    private var descTextWatcher: TextWatcher? = null

    companion object{
        const val TAG = "TaskEditBottomSheet"
        const val REQUEST_KEY = "edit_dialog_result"
        const val ACTION_KEY = "edit_action"
        const val SAVE_KEY = "save_task"
        const val DELETE_KEY = "delete_edit_task"

        fun newInstance(): TaskEditBottomSheetDialog {
            return TaskEditBottomSheetDialog()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTaskEditDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateSystemBars()
        updateUIGroupMode()
        setUpRecyclerView()
        setUpObservers()
        setUpTextWatcher()
        setUpListeners()
        setupFragmentResultListener()
        setupEditTextScroll()
        setupEditTextConfigureMaxLines()
    }

    override fun onStart() {
        super.onStart()

        setUpBottomSheetBehavior{

            handleCancelClick()
        }
    }

    override fun onDestroyView() {
        formViewModel.isEditing = false
        binding.subtaskRecyclerView.adapter = null
        subtasksAdapter.submitList(emptyList())
        removeTextWatcher()
        _binding = null
        super.onDestroyView()
    }

    /**
     * Updates UI based on task type (single vs group).
     */
    private fun updateUIGroupMode(){

        if (editTask is TaskItem.SingleTaskItem){

            binding.etGroupTaskDescriptionTextInputLayout.visibility = View.GONE
            binding.etGroupDescription.visibility = View.GONE
            binding.addSubtaskButton.visibility = View.GONE
            binding.textViewSubtask.visibility = View.GONE
            binding.etGroupDescription.text?.clear()
            binding.checkBoxGroupTask.visibility = View.GONE
            binding.addSubtaskButton.visibility = View.GONE
            binding.subtaskRecyclerView.visibility = View.GONE

            binding.etTaskDescriptionTextInput.visibility = View.VISIBLE
            binding.etTaskDescriptionTextInputLayout.visibility = View.VISIBLE
            binding.checkBoxSingleTask.visibility = View.VISIBLE
            binding.linearLayoutSingleTask.layoutParams.let { params ->
                if (params is LinearLayout.LayoutParams) {
                    params.weight = 1f
                    binding.linearLayoutSingleTask.layoutParams = params
                }
            }

            binding.checkBoxSingleTask.isChecked = editTask.isCompleted
            binding.etTaskDescriptionTextInput.setText(editTask.desc)
        }
        if (editTask is TaskItem.TaskGroupWithSubtasksItem){

            binding.etTaskDescriptionTextInput.text?.clear()
            binding.etTaskDescriptionTextInput.visibility = View.GONE
            binding.etTaskDescriptionTextInputLayout.visibility = View.GONE
            binding.checkBoxSingleTask.visibility = View.GONE
            binding.linearLayoutSingleTask.layoutParams.let { params ->
                if (params is LinearLayout.LayoutParams) {
                    params.weight = 0f
                    binding.linearLayoutSingleTask.layoutParams = params
                }
            }

            binding.etGroupTaskDescriptionTextInputLayout.visibility = View.VISIBLE
            binding.etGroupDescription.visibility = View.VISIBLE
            binding.addSubtaskButton.visibility = View.VISIBLE
            binding.textViewSubtask.visibility = View.VISIBLE
            binding.checkBoxGroupTask.visibility = View.VISIBLE
            binding.groupSubtasksLinearLayout.visibility = View.VISIBLE

            binding.checkBoxGroupTask.isChecked = editTask.isCompleted
            binding.etGroupDescription.setText(editTask.desc)
            binding.subtaskRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun setUpObservers(){

       viewLifecycleOwner.lifecycleScope.launch {
           repeatOnLifecycle(Lifecycle.State.STARTED) {
               formViewModel.subtasksEdit.collect { subTasks ->

                   if (!formViewModel.isEditing) {
                       subtasksAdapter.submitList(subTasks)
                   }
               }
           }
       }
    }

    private fun setUpRecyclerView(){

        subtasksAdapter = SubtasksEditBottomSheetAdapter(

            onSubtaskChanged = { subTask ->

                formViewModel.isEditing = true
                formViewModel.updateSingleSubTaskFunctional(subTask)
            },
            onLongClick = { subTask ->

                DeleteSubtaskConfirmationDialog.Companion.showDeleteSubtaskConfirmationDialog(
                    childFragmentManager,
                    subTask
                )
            }
        )
        binding.subtaskRecyclerView.apply {

            layoutManager = LinearLayoutManager(requireContext())
            adapter = subtasksAdapter
            isNestedScrollingEnabled = true
        }
    }

    private fun setUpTextWatcher(){

        val isGroupMode: Boolean = editTask is TaskItem.TaskGroupWithSubtasksItem

        descTextWatcher = object : TextWatcher {
            var previousDesc = editTask.desc
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
            }

            override fun afterTextChanged(s: Editable?) {

                val newDesc = s?.toString()?: ""
                if (previousDesc != newDesc){

                    formViewModel.updateDescTitle(newDesc)
                }
            }
        }

        if (isGroupMode){
            binding.etGroupDescription.addTextChangedListener(descTextWatcher)
        }
        else{
            binding.etTaskDescriptionTextInput.addTextChangedListener(descTextWatcher)
        }
    }

    private fun removeTextWatcher(){
        val isGroupMode: Boolean = editTask is TaskItem.TaskGroupWithSubtasksItem
        if (isGroupMode){
            binding.etGroupDescription.removeTextChangedListener(descTextWatcher)
        }
        else{
            binding.etTaskDescriptionTextInput.removeTextChangedListener(descTextWatcher)
        }

        descTextWatcher = null
    }

    private fun setUpListeners(){

        binding.saveButton.setOnClickListener {

            formViewModel.isEditing = false
            val saveBundle: Bundle =
                Bundle().apply { putString(ACTION_KEY, SAVE_KEY) }

            parentFragmentManager.setFragmentResult(
                REQUEST_KEY,
                saveBundle
            )
            dismiss()
        }

        binding.cancelButton.setOnClickListener {
            handleCancelClick()
        }

        binding.checkBoxSingleTask.setOnCheckedChangeListener { _, isChecked ->
            formViewModel.editIsCompletedTitle(isChecked)
        }

        binding.checkBoxGroupTask.setOnCheckedChangeListener { _, isChecked ->
            formViewModel.editIsCompletedTitle(isChecked)
        }

        binding.addSubtaskButton.setOnClickListener {
            formViewModel.isEditing = false
            formViewModel.addEmptySubtask()

            binding.subtaskRecyclerView.post {
                binding.subtaskRecyclerView.smoothScrollToPosition(0)
            }

            formViewModel.isEditing = true
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

    private fun handleCancelClick(){

        if (!formViewModel.compareEditTaskWithOriginalTask()) {
            SaveEditTaskConfirmationDialog.Companion.showSaveEditConfirmationDialog(childFragmentManager)
        }
        else {
            formViewModel.isEditing = false
            formViewModel.clearTaskData()
            dismiss()
        }
    }

    private fun setupFragmentResultListener(){

        childFragmentManager.setFragmentResultListener(
            DeleteSubtaskConfirmationDialog.Companion.DELETE_SUBTASK_DIALOG_REQUEST,
            viewLifecycleOwner)
        { requestKey, bundle ->

            when(bundle.getString(DeleteSubtaskConfirmationDialog.Companion.DELETE_ACTION)){
                "delete" ->{
                    val subtaskId = bundle.getLong(DeleteSubtaskConfirmationDialog.Companion.DELETE_SUBTASK_ID)
                    val groupId = bundle.getLong(DeleteSubtaskConfirmationDialog.Companion.DELETE_SUBTASK_GROUP_ID)
                    deleteSubtaskByID(subtaskId)
                }
            }
        }

        childFragmentManager.setFragmentResultListener(SaveEditTaskConfirmationDialog.Companion.SAVE_DIALOG_REQUEST, viewLifecycleOwner)
        { requestKey, bundle ->

            when(bundle.getString(SaveEditTaskConfirmationDialog.Companion.SAVE_ACTION)){

                "save_changes" -> {
                    formViewModel.isEditing = false

                    val saveBundle: Bundle =
                        Bundle().apply { putString(ACTION_KEY, SAVE_KEY) }

                    parentFragmentManager.setFragmentResult(
                        REQUEST_KEY,
                        saveBundle
                    )
                    dismiss()
                }
                "delete_changes" -> {

                    formViewModel.isEditing = false

                    val deleteBundle: Bundle =
                        Bundle().apply { putString(ACTION_KEY, DELETE_KEY) }

                    parentFragmentManager.setFragmentResult(
                        REQUEST_KEY,
                        deleteBundle
                    )
                    dismiss()
                }
            }
        }
    }

    private fun deleteSubtaskByID(id: Long){

        formViewModel.isEditing = false
        formViewModel.deleteSubtaskById(id)
        formViewModel.isEditing = true
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
        val editTextSingleDesc = binding.etTaskDescriptionTextInput
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
        binding.etGroupDescription.adjustMaxLinesBasedOnOrientation()
    }
}