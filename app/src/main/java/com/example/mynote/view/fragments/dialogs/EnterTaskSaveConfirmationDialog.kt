package com.example.mynote.view.fragments.dialogs


import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.example.mynote.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**

 * Dialog fragment for confirming task save (both single and group tasks with subtasks)
 * in the entry task bottom sheet.
 *
 * Displays a confirmation dialog with the task description and options to save, close dialog without saving
 * or cancel dialog, when the user attempts to close entry task bottom sheet without saving.
 *
 * **Actions:**
 * - **Save:** Saves the task (single or group with subtasks)
 * - **Close dialog without saving:** Discards changes and closes
 * - **Cancel:** Returns to editing without saving
 *
 *  Handles both:
 *  - SingleTaskItem: Standalone tasks without subtasks
 *  - TaskGroupWithSubtasksItem: Group tasks with subtasks
 *
 */
class EnterTaskSaveConfirmationDialog : DialogFragment() {

    companion object {
        private const val ARG_TASK_DESC = "task_desc"
        private const val ARG_TASK_GROUP_DESC = "task_group_desc"
        private const val ARG_IS_GROUP_MODE = "is_group_mode"
        private const val ARG_SUBTASKS = "subtasks"
        const val REQUEST_KEY = "save_confirmation_enter_dialog_result"

        /**
         * Creates a new instance of the confirmation dialog for a task (both single and group tasks),
         * when the user attempts to close entry task bottom sheet without saving
         *
         * @param taskDesc Description of the single task (for single task mode)
         * @param taskGroupDesc Description of the group task (for group task mode)
         * @param isGroupMode True if creating a group task with subtasks, false for single task
         * @param subtasks List of subtask descriptions (ignored for single tasks)
         * @return A new instance of EnterTaskSaveConfirmationDialog
         *
         * @see EnterTaskSaveConfirmationDialog
         */
        fun newInstance(
            taskDesc: String,
            taskGroupDesc: String,
            isGroupMode: Boolean,
            subtasks: List<String>
        ): EnterTaskSaveConfirmationDialog {
            val fragment = EnterTaskSaveConfirmationDialog()
            val args = Bundle().apply {
                putString(ARG_TASK_DESC, taskDesc)
                putString(ARG_TASK_GROUP_DESC, taskGroupDesc)
                putBoolean(ARG_IS_GROUP_MODE, isGroupMode)
                putStringArrayList(ARG_SUBTASKS, ArrayList(subtasks))
            }
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var taskDesc: String
    private lateinit var taskGroupDesc: String
    private var isGroupMode: Boolean = false
    private lateinit var subtasks: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let { args ->
            taskDesc = args.getString(ARG_TASK_DESC, "")
            taskGroupDesc = args.getString(ARG_TASK_GROUP_DESC, "")
            isGroupMode = args.getBoolean(ARG_IS_GROUP_MODE, false)
            subtasks = args.getStringArrayList(ARG_SUBTASKS) ?: emptyList()
        } ?: run {
            taskDesc = ""
            taskGroupDesc = ""
            isGroupMode = false
            subtasks = emptyList()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext(),R.style.ThemeOverlay_MyNote_MaterialAlertDialog).apply {
            val title = if (isGroupMode) {
                getString(R.string.dialog_group_title_save_task_entry_bottom_sheet, taskGroupDesc)
            } else {
                getString(R.string.dialog_single_title_save_task_entry_bottom_sheet, taskDesc)
            }
            setTitle(title)
            setPositiveButton(getString(R.string.dialog_action_save_task_entry_bottom_sheet)) { dialog, _ ->
                val resultBundle = Bundle().apply {
                    putString("action", "save")
                    putString("taskDesc", taskDesc)
                    putString("taskGroupDesc", taskGroupDesc)
                    putBoolean("isGroupMode", isGroupMode)
                    putStringArrayList("subtasks", ArrayList(subtasks))
                }
                parentFragmentManager.setFragmentResult(REQUEST_KEY, resultBundle)
            }

            setNegativeButton(getString(R.string.dialog_action_close_no_save_entry_bottom_sheet)) { dialog, _ ->
                val resultBundle = Bundle().apply {
                    putString("action", "discard")
                }
                parentFragmentManager.setFragmentResult(REQUEST_KEY, resultBundle)
            }

            setNeutralButton(getString(R.string.dialog_action_cancel_dialog_entry_bottom_sheet)) { dialog, _ ->
                val resultBundle = Bundle().apply {
                    putString("action", "cancel")
                }
                parentFragmentManager.setFragmentResult(REQUEST_KEY, resultBundle)
                dialog.dismiss()
            }
        }.create()
    }
}
